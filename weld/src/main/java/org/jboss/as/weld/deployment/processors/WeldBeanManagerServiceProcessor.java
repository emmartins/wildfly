/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.deployment.processors;

import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.arquillian.WeldContextSetup;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.services.BeanManagerService;
import org.jboss.as.weld.util.Utils;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * {@link DeploymentUnitProcessor} that binds the bean manager to JNDI
 *
 * @author Stuart Douglas
 */
public class WeldBeanManagerServiceProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevelDeployment = Utils.getRootDeploymentUnit(deploymentUnit);
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        if (!WeldDeploymentMarker.isPartOfWeldDeployment(topLevelDeployment)) {
            return;
        }

        BeanDeploymentArchiveImpl rootBda = deploymentUnit
                .getAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE);
        if (rootBda == null) {
            // this archive is not actually a bean archive.
            // then use the top level root bda
            rootBda = topLevelDeployment.getAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE);
        }
        if (rootBda == null) {
            WeldLogger.ROOT_LOGGER.couldNotFindBeanManagerForDeployment(deploymentUnit.getName());
            return;
        }

        final ServiceName weldServiceName = topLevelDeployment.getServiceName().append(WeldBootstrapService.SERVICE_NAME);

        // add the BeanManager service
        final ServiceName beanManagerServiceName = BeanManagerService.serviceName(deploymentUnit);
        BeanManagerService beanManagerService = new BeanManagerService(rootBda.getId());
        serviceTarget.addService(beanManagerServiceName, beanManagerService).addDependency(weldServiceName,
                WeldBootstrapService.class, beanManagerService.getWeldContainer()).install();

        deploymentUnit.addToAttachmentList(Attachments.SETUP_ACTIONS, new WeldContextSetup());
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }

}
