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
package org.jboss.as.ee.component.deployers;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.msc.service.ServiceName;

import java.util.Set;

/**
 * Processor that does the JNDI bindings that are owned by the module.
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class ModuleJndiBindingProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final EEModuleConfiguration moduleConfiguration = deploymentUnit.getAttachment(Attachments.EE_MODULE_CONFIGURATION);
        if (moduleConfiguration == null || DeploymentUtils.skipRepeatedActivation(deploymentUnit, 0)) {
            return;
        }
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        final InjectionSource.ResolutionContext resolutionContext = new InjectionSource.ResolutionContext(
                true,
                moduleConfiguration.getModuleName(),
                moduleConfiguration.getModuleName(),
                moduleConfiguration.getApplicationName(),
                eeModuleDescription.getBindingConfigurations().getDeploymentBindings()
        );
        final ServiceVerificationHandler serviceVerificationHandler = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.SERVICE_VERIFICATION_HANDLER);

        // do the bindings
        final Set<ServiceName> servicesBound = eeModuleDescription.getBindingConfigurations().doBindings(phaseContext, resolutionContext, serviceVerificationHandler);
        deploymentUnit.getAttachmentList(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES).addAll(servicesBound);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
