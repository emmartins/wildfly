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
import org.jboss.as.ee.component.ServiceInjectionSource;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.ServiceName;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The application binder manages the binds for a scoped java:app context.
 * @author Eduardo Martins
 */
public class ApplicationBinder extends AbstractBinder {

    /**
     * the binders for modules in the application
     */
    private final Set<ModuleBinder> moduleBinders = new HashSet<>();
    /**
     * the java:app and global bindings in module binders, the app binder needs to account these to learn what shared bindings should not be bound
     */
    private final ConcurrentHashMap<ContextNames.BindInfo, BindingConfiguration> moduleBindings = new ConcurrentHashMap<>();
    /**
     * the bindings in the shared java:app context
     */
    private final Map<String, ServiceName> sharedBindings;

    /**
     *
     * @param sharedBindings
     */
    public ApplicationBinder(Map<String, ServiceName> sharedBindings) {
        this.sharedBindings = sharedBindings;
    }

    /**
     * Adds a module binder.
     * @param module
     */
    public synchronized void addModuleBinder(ModuleBinder module) {
        moduleBinders.add(module);
    }

    /**
     * A module binder is done binding.
     * @param module
     * @param phaseContext
     * @param servicesBound
     * @param resolutionContext
     * @param serviceVerificationHandler
     * @throws DeploymentUnitProcessingException
     */
    public synchronized void moduleBinderDone(ModuleBinder module, DeploymentPhaseContext phaseContext, List<ServiceName> servicesBound, InjectionSource.ResolutionContext resolutionContext, ServiceVerificationHandler serviceVerificationHandler) throws DeploymentUnitProcessingException {
        if (moduleBinders.remove(module) && moduleBinders.isEmpty()) {
            // last module does the java:app shared bindings not overridden
            if (sharedBindings != null) {
                for (Map.Entry<String, ServiceName> sharedBinding : sharedBindings.entrySet()) {
                    final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(resolutionContext.getApplicationName(), sharedBinding.getKey());
                    if (!containsBinding(bindInfo) && !moduleBindings.containsKey(bindInfo)) {
                        bind(bindInfo, new BindingConfiguration(sharedBinding.getKey(), new ServiceInjectionSource(sharedBinding.getValue())), phaseContext, servicesBound, resolutionContext, serviceVerificationHandler);
                    }
                }
            }
        }
    }

    /**
     * Adds a bind managed by a module binder
     * @param bindInfo
     * @param bindingConfiguration
     * @throws IllegalArgumentException if a module binder already contains the specified binding, and the binding value source of both bindings aren't equal
     */
    public void putModuleBinding(ContextNames.BindInfo bindInfo, BindingConfiguration bindingConfiguration) throws IllegalArgumentException {
        final BindingConfiguration existentBindingConfiguration = moduleBindings.putIfAbsent(bindInfo, bindingConfiguration);
        if (existentBindingConfiguration != null && !equals(existentBindingConfiguration.getSource(), bindingConfiguration.getSource())) {
            throw EeLogger.ROOT_LOGGER.conflictingBinding(bindingConfiguration.getName(), bindingConfiguration.getSource());
        }
    }

}
