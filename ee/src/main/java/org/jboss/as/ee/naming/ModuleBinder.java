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
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.ServiceName;

import java.util.List;
import java.util.Map;

/**
 * The module binder manages the binds for scoped java:module context. It also cooperates with the app binder to handle bindings for the scoped java:app, and global contexts.
 * @author Eduardo Martins
 */
public class ModuleBinder extends AbstractBinder {

    /**
     * the app binder
     */
    private final ApplicationBinder applicationBinder;
    /**
     * the bindings in the shared java:module context
     */
    private final Map<String, ServiceName> sharedBindings;

    /**
     *
     * @param applicationBinder
     * @param sharedBindings
     */
    public ModuleBinder(ApplicationBinder applicationBinder, Map<String, ServiceName> sharedBindings) {
        this.sharedBindings = sharedBindings;
        this.applicationBinder = applicationBinder;
        this.applicationBinder.addModuleBinder(this);
    }

    @Override
    public void putBinding(ContextNames.BindInfo bindInfo, BindingConfiguration bindingConfiguration) {
        final JndiName jndiName = bindingConfiguration.getName();
        if (!jndiName.isJavaModule() && !jndiName.isJavaComp()) {
            // app or global binding, notify app binder, which needs to account these bindings for all modules in the application
            applicationBinder.putModuleBinding(bindInfo, bindingConfiguration);
        }
        super.putBinding(bindInfo, bindingConfiguration);
    }

    @Override
    public void doBindings(DeploymentPhaseContext phaseContext, List<ServiceName> servicesBound, InjectionSource.ResolutionContext resolutionContext, ServiceVerificationHandler serviceVerificationHandler) throws DeploymentUnitProcessingException {
        // do the shared java:module bindings which were not overridden
        if (sharedBindings != null) {
            for (Map.Entry<String, ServiceName> sharedBinding : sharedBindings.entrySet()) {
                final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(resolutionContext.getApplicationName(), resolutionContext.getModuleName(), resolutionContext.getComponentName(), !resolutionContext.isCompUsesModule(), sharedBinding.getKey());
                if (!containsBinding(bindInfo)) {
                    bind(bindInfo, new BindingConfiguration(sharedBinding.getKey(), new ServiceInjectionSource(sharedBinding.getValue())), phaseContext, servicesBound, resolutionContext, serviceVerificationHandler);
                }
            }
        }
        super.doBindings(phaseContext, servicesBound, resolutionContext, serviceVerificationHandler);
        // warn the app binder we are done
        applicationBinder.moduleBinderDone(this, phaseContext, servicesBound, resolutionContext, serviceVerificationHandler);
    }

}