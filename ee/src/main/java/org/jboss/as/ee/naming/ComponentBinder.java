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
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.ServiceName;

import java.util.List;
import java.util.Map;

/**
 * The component binder manages the binds for a scoped java:comp context.
 * @author Eduardo Martins
 */
public class ComponentBinder extends AbstractBinder {

    /**
     * the bindings in the shared java:comp context
     */
    private final Map<String, ServiceName> sharedBindings;

    /**
     *
     * @param sharedBindings
     */
    public ComponentBinder(Map<String, ServiceName> sharedBindings) {
        this.sharedBindings = sharedBindings;
    }

    @Override
    public void doBindings(DeploymentPhaseContext phaseContext, List<ServiceName> servicesBound, InjectionSource.ResolutionContext resolutionContext, ServiceVerificationHandler serviceVerificationHandler) throws DeploymentUnitProcessingException, IllegalArgumentException {
        // first do the shared bindings, which the binder does not contain
        if (sharedBindings != null) {
            for (Map.Entry<String, ServiceName> sharedBinding : sharedBindings.entrySet()) {
                final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(resolutionContext.getApplicationName(), resolutionContext.getModuleName(), resolutionContext.getComponentName(), !resolutionContext.isCompUsesModule(), sharedBinding.getKey());
                if (!containsBinding(bindInfo)) {
                    bind(bindInfo, new BindingConfiguration(sharedBinding.getKey(), new ServiceInjectionSource(sharedBinding.getValue())), phaseContext, servicesBound, resolutionContext, serviceVerificationHandler);
                }
            }
        }
        super.doBindings(phaseContext, servicesBound, resolutionContext, serviceVerificationHandler);
    }
}
