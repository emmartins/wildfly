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
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.ScopedJavaModuleServiceBasedNamingStoreService;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.server.deployment.Attachments.SETUP_ACTIONS;

/**
 * Processor that deploys the scoped java:module context.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public class ModuleContextProcessor implements DeploymentUnitProcessor {

    private static final JndiName JNDI_NAME_java_module_ModuleName = new JndiName("java:module/ModuleName");

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

        // setup the scoped java:module naming store
        final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(moduleDescription.getApplicationName(), moduleDescription.getModuleName());
        final ScopedJavaModuleServiceBasedNamingStoreService contextService = new ScopedJavaModuleServiceBasedNamingStoreService();
        phaseContext.getServiceTarget().addService(moduleContextServiceName, contextService)
                .addDependency(ContextNames.SHARED_MODULE_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, contextService.getSharedJavaModuleServiceBasedNamingStore())
                .addDependency(ContextNames.SHARED_COMP_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, contextService.getSharedJavaCompServiceBasedNamingStore())
                .install();

        // setup the namespace context selector
        final ServiceName applicationContextServiceName = ContextNames.contextServiceNameOfApplication(moduleDescription.getApplicationName());
        final InjectedEENamespaceContextSelector selector = new InjectedEENamespaceContextSelector();
        phaseContext.addDependency(applicationContextServiceName, NamingStore.class, selector.getAppContextInjector());
        phaseContext.addDependency(moduleContextServiceName, NamingStore.class, selector.getModuleContextInjector());
        phaseContext.addDependency(moduleContextServiceName, NamingStore.class, selector.getCompContextInjector());
        phaseContext.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, NamingStore.class, selector.getJbossContextInjector());
        phaseContext.addDependency(ContextNames.EXPORTED_CONTEXT_SERVICE_NAME, NamingStore.class, selector.getExportedContextInjector());
        phaseContext.addDependency(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, NamingStore.class, selector.getGlobalContextInjector());
        moduleDescription.setNamespaceContextSelector(selector);

        // setup the jndi dependencies DU attachment list
        deploymentUnit.putAttachment(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES, new AttachmentList<>(ServiceName.class));

        // add the arquillian setup action, so the module namespace is available in arquillian tests
        final JavaNamespaceSetup setupAction = new JavaNamespaceSetup(selector, deploymentUnit.getServiceName());
        deploymentUnit.addToAttachmentList(SETUP_ACTIONS, setupAction);
        deploymentUnit.addToAttachmentList(org.jboss.as.ee.component.Attachments.WEB_SETUP_ACTIONS, setupAction);
        deploymentUnit.putAttachment(Attachments.JAVA_NAMESPACE_SETUP_ACTION, setupAction);

        final ModuleBindingConfigurations moduleBindingConfigurations = moduleDescription.getBindingConfigurations();
        // add the java:module/ModuleName binding
        moduleBindingConfigurations.addDeploymentBinding(new BindingConfiguration(JNDI_NAME_java_module_ModuleName, new FixedInjectionSource(moduleDescription.getModuleName())));
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
