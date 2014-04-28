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

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.ServiceBasedNamingStoreService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Deployment processor that deploys the scoped java:comp contexts.
 *
 * @author Eduardo Martins
 */
public class ComponentContextProcessor implements DeploymentUnitProcessor {

    private static final JndiName JNDI_NAME_java_comp_env = new JndiName("java:comp/env");
    private static final BindingConfiguration BINDING_CONFIGURATION_java_comp_env = new BindingConfiguration(JNDI_NAME_java_comp_env, new ContextInjectionSource("env", JNDI_NAME_java_comp_env));

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }

        for (ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
            if (componentDescription.getNamingMode() == ComponentNamingMode.CREATE) {
                // setup the java:comp naming store
                final ServiceBasedNamingStoreService contextService = new ServiceBasedNamingStoreService(ContextNames.JAVA_COMP_NAME);
                phaseContext.getServiceTarget().addService(componentDescription.getContextServiceName(), contextService).install();
                // add the java:comp/env binding
                componentDescription.getBindingConfigurations().add(BINDING_CONFIGURATION_java_comp_env);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
