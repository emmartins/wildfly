/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat Inc., and individual contributors as indicated
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

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.ServiceInjectionSource;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.services.BeanManagerService;
import org.jboss.as.weld.util.Utils;
import org.jboss.msc.service.ServiceName;

import javax.enterprise.inject.spi.BeanManager;

/**
 * {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} that binds the bean manager to JNDI
 *
 * @author Eduardo Martins
 */
public class WeldBeanManagerJndiProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // do not bind on EAR's modules
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        final DeploymentUnit topLevelDeployment = Utils.getRootDeploymentUnit(deploymentUnit);
        if (!WeldDeploymentMarker.isPartOfWeldDeployment(topLevelDeployment)) {
            return;
        }
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }
        // create the java:comp/BeanManager binding configuration and add it to all components
        final ServiceName beanManagerServiceName = BeanManagerService.serviceName(deploymentUnit);
        final ServiceInjectionSource serviceInjectionSource = new ServiceInjectionSource(beanManagerServiceName, BeanManager.class);
        final BindingConfiguration bindingConfiguration = new BindingConfiguration("java:comp/BeanManager", serviceInjectionSource);
        moduleDescription.getSharedCompBindingConfigurations().add(bindingConfiguration);
        // FIXME emmartins: find out why this is needed
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit) && deploymentUnit.getName().endsWith(".jar")) {
            moduleDescription.getModuleBindingConfigurations().add(bindingConfiguration);
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }

}
