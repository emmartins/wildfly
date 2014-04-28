/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.FixedInjectionSource;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.ServiceBasedNamingStoreService;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;

/**
 * Deployment processor that deploys the scoped java:app context.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public class ApplicationContextProcessor implements DeploymentUnitProcessor {

    private static final JndiName JNDI_NAME_java_app_AppName = new JndiName("java:app/AppName");
    private static final JndiName JNDI_NAME_java_app_env = new JndiName("java:app/env");

    private static final BindingConfiguration BINDING_CONFIGURATION_java_app_env = new BindingConfiguration(JNDI_NAME_java_app_env, new ContextInjectionSource("env", JNDI_NAME_java_app_env));

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            return;
        }
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }

        // setup the scoped java:app naming store
        final ServiceBasedNamingStoreService contextService = new ServiceBasedNamingStoreService(ContextNames.JAVA_APP_NAME);
        final ServiceName applicationContextServiceName = ContextNames.contextServiceNameOfApplication(moduleDescription.getApplicationName());
        phaseContext.getServiceTarget().addService(applicationContextServiceName, contextService).install();

        // setup the app context binder
        final SharedContextBindings sharedContextBindings = deploymentUnit.getAttachment(Attachments.EE_JNDI_SHARED_CONTEXTS_BINDINGS);
        deploymentUnit.putAttachment(Attachments.EE_JNDI_APP_CONTEXT_BINDER, new ApplicationBinder(sharedContextBindings.getJavaApp()));

        // add the java:app/AppName binding
        moduleDescription.getModuleBindingConfigurations().add(new BindingConfiguration(JNDI_NAME_java_app_AppName, new FixedInjectionSource(moduleDescription.getApplicationName())));

        // add the java:app/env binding
        moduleDescription.getModuleBindingConfigurations().add(BINDING_CONFIGURATION_java_app_env);

        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            // setup the jndi dependencies DU attachment list
            deploymentUnit.putAttachment(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES, new AttachmentList<>(ServiceName.class));
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
