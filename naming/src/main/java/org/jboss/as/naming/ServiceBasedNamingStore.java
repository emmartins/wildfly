/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.naming;

import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.event.NamingListener;
import javax.naming.spi.ResolveResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author John Bailey
 * @author Jason T. Greene
 * @author Eduardo Martins
 */
public class ServiceBasedNamingStore implements NamingStore {
    private final Name EMPTY_NAME = new CompositeName();
    private Name baseName;
    private final ServiceRegistry serviceRegistry;
    private final ServiceName serviceNameBase;

    private final BoundServices boundServices;

    public ServiceBasedNamingStore(final ServiceRegistry serviceRegistry, final Name baseName, final ServiceName serviceNameBase) {
        this.serviceRegistry = serviceRegistry;
        this.baseName = baseName;
        this.serviceNameBase = serviceNameBase;
        this.boundServices = new BoundServices();
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name, true);
    }

    public Object lookup(final Name name, boolean dereference) throws NamingException {
        if (name.isEmpty()) {
            return new NamingContext(EMPTY_NAME, this, null);
        }
        final ServiceName lookupName = buildServiceName(name);
        final ServiceController<?> serviceController = lookupService(name, lookupName);
        if (serviceController != null) {
            return getServiceValue(name, serviceController, dereference);
        } else {
            if (boundServices.hasChild(lookupName)) {
                return new NamingContext((Name) name.clone(), this, null);
            } else {
                final ServiceName parent = boundServices.findParent(lookupName);
                if (parent != null) {
                    // Parent might be a reference or a link
                    final Name parentName = convertToName(parent);
                    final ServiceController<?> parentServiceController = lookupService(parentName, parent);
                    if (parentServiceController != null) {
                        final Object parentValue = getServiceValue(parentName, parentServiceController, dereference);
                        if (!(parentValue instanceof NamingContext)) {
                            checkReferenceForContinuation(name, parentValue);
                            return new ResolveResult(parentValue, name.getSuffix(parentName.size()));
                        }
                    }
                }
            }
            throw new NameNotFoundException(name.toString() + " -- " + lookupName);
        }
    }

    private void checkReferenceForContinuation(final Name name, final Object object) throws CannotProceedException {
        if (object instanceof Reference) {
            if (((Reference) object).get("nns") != null) {
                throw cannotProceedException(object, name);
            }
        }
    }

    private static CannotProceedException cannotProceedException(final Object resolvedObject, final Name remainingName) {
        final CannotProceedException cpe = new CannotProceedException();
        cpe.setResolvedObj(resolvedObject);
        cpe.setRemainingName(remainingName);
        return cpe;
    }

    private ServiceController<?> lookupService(final Name name, final ServiceName lookupName) throws NamingException {
        try {
            return serviceRegistry.getService(lookupName);
        } catch (IllegalStateException e) {
            NameNotFoundException n = new NameNotFoundException(name.toString());
            n.initCause(e);
            throw n;
        } catch (Throwable t) {
            NamingException n = NamingLogger.ROOT_LOGGER.lookupError(name.toString());
            n.initCause(t);
            throw n;
        }
    }

    private Object getServiceValue(final Name name, final ServiceController serviceController, boolean dereference) throws NamingException {
        try {
            final Object object = serviceController.getValue();
            if (dereference && object instanceof ManagedReferenceFactory) {
                final ManagedReference managedReference = ManagedReferenceFactory.class.cast(object).getReference();
                return managedReference != null ? managedReference.getInstance() : null;
            } else {
                return object;
            }
        } catch (IllegalStateException e) {
            NameNotFoundException n = new NameNotFoundException(name.toString());
            n.initCause(e);
            throw n;
        } catch (Throwable t) {
            NamingException n = NamingLogger.ROOT_LOGGER.lookupError(name.toString());
            n.initCause(t);
            throw n;
        }
    }

    public List<NameClassPair> list(final Name name) throws NamingException {
        final ServiceName serviceName = buildServiceName(name);
        checkIsContext(name, serviceName);
        final Set<String> childContexts = new HashSet<>();
        final List<NameClassPair> results = new ArrayList<>();
        for (ServiceName childServiceName : boundServices.getChildren(serviceName)) {
            final Name childName = convertToName(childServiceName);
            final String childNameComponent = childName.get(name.size());
            if (childName.size() > name.size()+1) {
                // child of a child context
                if (childContexts.add(childNameComponent)) {
                    results.add(new NameClassPair(childNameComponent, Context.class.getName()));
                }
            } else {
                final Object binding = lookup(childName, false);
                final String bindingType;
                if (binding instanceof ContextListManagedReferenceFactory) {
                    bindingType = ContextListManagedReferenceFactory.class.cast(binding)
                            .getInstanceClassName();
                } else {
                    if (binding instanceof ManagedReferenceFactory) {
                        bindingType = ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
                    } else {
                        bindingType = binding.getClass().getName();
                    }
                }
                results.add(new NameClassPair(childNameComponent, bindingType));
            }
        }
        return results;
    }

    public List<Binding> listBindings(final Name name) throws NamingException {
        final ServiceName serviceName = buildServiceName(name);
        checkIsContext(name, serviceName);
        final Set<String> childContexts = new HashSet<>();
        final List<Binding> results = new ArrayList<>();
        for (ServiceName childServiceName : boundServices.getChildren(serviceName)) {
            final Name childName = convertToName(childServiceName);
            final String childNameComponent = childName.get(name.size());
            if (childName.size() > name.size()+1) {
                // child of a child context
                if (childContexts.add(childNameComponent)) {
                    results.add(new Binding(childNameComponent, new NamingContext(((Name) name.clone()).add(childNameComponent), this, null)));
                }
            } else {
                final Object binding = lookup(childName, true);
                results.add(new Binding(childNameComponent, binding));
            }
        }
        return results;
    }

    private void checkIsContext(Name name, ServiceName lookupName) throws NamingException {
        if (name.isEmpty()) {
            return;
        }
    }

    public void close() throws NamingException {
        boundServices.clear();
    }

    public void addNamingListener(Name target, int scope, NamingListener listener) {
    }

    public void removeNamingListener(NamingListener listener) {
    }

    public void add(final ServiceName serviceName) {
        if (!boundServices.add(serviceName)) {
            throw NamingLogger.ROOT_LOGGER.serviceAlreadyBound(serviceName);
        }
    }

    public void remove(final ServiceName serviceName) {
        boundServices.remove(serviceName);
    }

    protected ServiceName buildServiceName(final Name name) {
        final Enumeration<String> parts = name.getAll();
        ServiceName current = serviceNameBase;
        while (parts.hasMoreElements()) {
            final String currentPart = parts.nextElement();
            if (!currentPart.isEmpty()) {
                current = current.append(currentPart);
            }
        }
        return current;
    }

    private Name convertToName(ServiceName serviceName) {
        String[] c = serviceName.toArray();
        CompositeName name = new CompositeName();
        int baseIndex = serviceNameBase.toArray().length;
        for (int i = baseIndex; i < c.length; i++) {
            try {
                name.add(c[i]);
            } catch (InvalidNameException e) {
                throw new IllegalStateException(e);
            }
        }
        return name;
    }

    private String convertToString(ServiceName serviceName) {
        String[] c = serviceName.toArray();
        StringBuilder name = new StringBuilder();
        int baseIndex = serviceNameBase.toArray().length;
        for (int i = baseIndex; i < c.length; i++) {
            if (i != baseIndex) {
                name.append('/');
            }
            name.append(c[i]);
        }
        return name.toString();
    }

    protected ServiceName getServiceNameBase() {
        return serviceNameBase;
    }

    protected ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    @Override
    public Name getBaseName() throws NamingException {
        return baseName;
    }

    private static class BoundServices {

        private final ConcurrentSkipListSet<ServiceName> boundServices = new ConcurrentSkipListSet<>();

        public boolean add(ServiceName serviceName) {
            return boundServices.add(serviceName);
        }

        public boolean remove(ServiceName serviceName) {
            return boundServices.remove(serviceName);
        }

        public ServiceName findParent(ServiceName serviceName) {
            final ServiceName lower = boundServices.lower(serviceName);
            return (lower != null && lower.isParentOf(serviceName)) ? lower : null;
        }

        public boolean hasChild(ServiceName serviceName) {
            final ServiceName higher = boundServices.higher(serviceName);
            return higher != null && serviceName.isParentOf(higher);
        }

        public List<ServiceName> getChildren(ServiceName parent) {
            List<ServiceName> children = new ArrayList<>();
            for(ServiceName child : boundServices.tailSet(parent, false)) {
                if (parent.isParentOf(child)) {
                    children.add(child);
                } else {
                    break;
                }
            }
            return children;
        }

        public void clear() {
            boundServices.clear();
        }
    }

    public Set<ServiceName> getBoundServicesSet() {
        return Collections.unmodifiableSet(boundServices.boundServices);
    }

    public Map<String, ServiceName> getBoundServicesMap() throws NamingException {
        final Map<String, ServiceName> result = new HashMap<>();
        final String baseName = getBaseName().toString();
        for (ServiceName serviceName : boundServices.boundServices) {
            result.put(new StringBuilder(baseName).append('/').append(convertToString(serviceName)).toString(), serviceName);
        }
        return result;
    }

    @Override
    public String toString() {
        return "ServiceBasedNamingStore("+serviceNameBase+")";
    }
}
