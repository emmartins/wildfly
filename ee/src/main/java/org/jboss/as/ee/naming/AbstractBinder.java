/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.naming;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.CircularDependencyException;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common functionality for all binders.
 * @author Eduardo Martins
 */
public abstract class AbstractBinder {

    /**
     * the bindings map
     */
    protected final ConcurrentHashMap<ContextNames.BindInfo, BindingConfiguration> bindings = new ConcurrentHashMap<>();

    /**
     * Indicates if the binder contains the specified binding.
     * @param bindInfo
     * @return
     */
    public boolean containsBinding(ContextNames.BindInfo bindInfo) {
        return bindings.containsKey(bindInfo);
    }

    /**
     * Adds the specified binding.
     * @param bindInfo
     * @param bindingConfiguration
     * @throws IllegalArgumentException if the binder already contains the specified binding, and the binding value source of both bindings aren't equal
     */
    public void putBinding(ContextNames.BindInfo bindInfo, BindingConfiguration bindingConfiguration) throws IllegalArgumentException {
        final BindingConfiguration existentBindingConfiguration = bindings.putIfAbsent(bindInfo, bindingConfiguration);
        if (existentBindingConfiguration != null && !equals(existentBindingConfiguration.getSource(), bindingConfiguration.getSource())) {
            throw EeLogger.ROOT_LOGGER.conflictingBinding(bindingConfiguration.getName(), bindingConfiguration.getSource());
        }
    }

    /**
     * Does the bindings.
     * @param phaseContext
     * @param servicesBound a list that will be filled with the service names of each binding done
     * @param resolutionContext
     * @param serviceVerificationHandler
     * @throws DeploymentUnitProcessingException if there is a failure resolving a binding source
     * @throws java.lang.IllegalArgumentException if a binder service failed to install, because it already exists, and the binding value source of both binder services aren't equal
     */
    public void doBindings(DeploymentPhaseContext phaseContext, List<ServiceName> servicesBound, InjectionSource.ResolutionContext resolutionContext, ServiceVerificationHandler serviceVerificationHandler) throws DeploymentUnitProcessingException, IllegalArgumentException {
        for (Map.Entry<ContextNames.BindInfo, BindingConfiguration> entry : bindings.entrySet()) {
            bind(entry.getKey(), entry.getValue(), phaseContext, servicesBound, resolutionContext, serviceVerificationHandler);
        }
    }

    /**
     * Does the specified bind.
     * @param bindInfo
     * @param bindingConfiguration
     * @param phaseContext
     * @param servicesBound
     * @param resolutionContext
     * @param serviceVerificationHandler
     * @throws DeploymentUnitProcessingException if there is a failure resolving a binding source
     * @throws java.lang.IllegalArgumentException if a binder service failed to install, because it already exists, and the binding value source of both binder services aren't equal
     */
    void bind(final ContextNames.BindInfo bindInfo, final BindingConfiguration bindingConfiguration, final DeploymentPhaseContext phaseContext, List<ServiceName> servicesBound, final InjectionSource.ResolutionContext resolutionContext, final ServiceVerificationHandler serviceVerificationHandler) throws DeploymentUnitProcessingException, IllegalArgumentException {
        final JndiName bindingName = bindingConfiguration.getName();
        final boolean shared = !bindingName.isJavaComp() && !bindingName.isJavaModule() && !bindingName.isJavaApp();
        BinderService service = null;
        try {
            service = new BinderService(bindInfo.getBindName(), bindingConfiguration.getSource(), shared);
            final ServiceTarget serviceTarget = shared ? CurrentServiceContainer.getServiceContainer() : phaseContext.getServiceTarget();
            final ServiceBuilder<ManagedReferenceFactory> serviceBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), service);
            bindingConfiguration.getSource().getResourceValue(resolutionContext, serviceBuilder, phaseContext, service.getManagedObjectInjector());
            serviceBuilder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, service.getNamingStoreInjector());
            serviceBuilder.addListener(serviceVerificationHandler);
            serviceBuilder.install();
        } catch (DuplicateServiceException e) {
            ServiceController<ManagedReferenceFactory> registered = (ServiceController<ManagedReferenceFactory>) CurrentServiceContainer.getServiceContainer().getService(bindInfo.getBinderServiceName());
            if (registered == null)
                throw e;
            service = (BinderService) registered.getService();
            if (!service.getSource().equals(bindingConfiguration.getSource()))
                throw EeLogger.ROOT_LOGGER.conflictingBinding(bindingName, bindingConfiguration.getSource());
        } catch (CircularDependencyException e) {
            throw EeLogger.ROOT_LOGGER.circularDependency(bindingName.getAbsoluteName());
        }
        if (shared && service != null) {
            //as these bindings are not child services
            //we need to add a listener that released the service when the deployment stops
            service.acquire();
            ServiceController<?> unitService = CurrentServiceContainer.getServiceContainer().getService(phaseContext.getDeploymentUnit().getServiceName());
            unitService.addListener(new BinderReleaseListener(service));
        }
        servicesBound.add(bindInfo.getBinderServiceName());
    }

    public static boolean equals(Object one, Object two) {
        return one == two || (one != null && one.equals(two));
    }

    private static class BinderReleaseListener<T> extends AbstractServiceListener<T> {

        private final BinderService binderService;

        public BinderReleaseListener(final BinderService binderService) {
            this.binderService = binderService;
        }

        @Override
        public void listenerAdded(final ServiceController<? extends T> serviceController) {
            if (serviceController.getState() == ServiceController.State.DOWN || serviceController.getState() == ServiceController.State.STOPPING) {
                binderService.release();
                serviceController.removeListener(this);
            }
        }

        @Override
        public void transition(final ServiceController<? extends T> serviceController, final ServiceController.Transition transition) {
            if (transition.getAfter() == ServiceController.Substate.STOPPING) {
                binderService.release();
                serviceController.removeListener(this);
            }
        }
    }

}
