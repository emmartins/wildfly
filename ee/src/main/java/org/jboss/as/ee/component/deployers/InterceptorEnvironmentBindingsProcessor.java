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

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.component.InterceptorEnvironment;
import org.jboss.as.ee.component.ResourceInjectionConfiguration;
import org.jboss.as.ee.naming.ComponentBindingConfigurations;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Processor which merges the interceptor environment binding configurations into component descriptions.
 *
 * @author Eduardo Martins
 */
public class InterceptorEnvironmentBindingsProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }
        for (final ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
            final ComponentBindingConfigurations componentBindingConfigurations = componentDescription.getBindingConfigurations();
            for (final InterceptorDescription interceptorDescription : componentDescription.getAllInterceptors()) {
                final InterceptorEnvironment interceptorEnvironment = moduleDescription.getInterceptorEnvironment().get(interceptorDescription.getInterceptorClassName());
                if (interceptorEnvironment != null) {
                    //if the interceptor has environment config we merge it into the components environment
                    for (BindingConfiguration bindingConfiguration : interceptorEnvironment.getBindingConfigurations()) {
                        componentBindingConfigurations.addDeploymentBinding(bindingConfiguration);
                    }
                    for (final ResourceInjectionConfiguration injection : interceptorEnvironment.getResourceInjections()) {
                        componentDescription.addResourceInjection(injection);
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
