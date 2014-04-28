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
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.ServiceBasedNamingStoreService;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.server.deployment.Attachments.SETUP_ACTIONS;

/**
 * Deployment processor that deploys the scoped java:module context.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public class ModuleContextProcessor implements DeploymentUnitProcessor {

    private static final JndiName JNDI_NAME_java_module_ModuleName = new JndiName("java:module/ModuleName");
    private static final JndiName JNDI_NAME_java_module_env = new JndiName("java:module/env");

    private static final BindingConfiguration BINDING_CONFIGURATION_java_module_env = new BindingConfiguration(JNDI_NAME_java_module_env, new ContextInjectionSource("env", JNDI_NAME_java_module_env));

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
        final DeploymentUnit parentDeploymentUnit = deploymentUnit.getParent();
        final SharedContextBindings sharedContextBindings = parentDeploymentUnit != null ? parentDeploymentUnit.getAttachment(Attachments.EE_JNDI_SHARED_CONTEXTS_BINDINGS) : deploymentUnit.getAttachment(Attachments.EE_JNDI_SHARED_CONTEXTS_BINDINGS);

        // setup the scoped java:module naming store
        final ServiceName moduleContextServiceName = ContextNames.contextServiceNameOfModule(moduleDescription.getApplicationName(), moduleDescription.getModuleName());
        final ServiceBasedNamingStoreService contextService = new ServiceBasedNamingStoreService(ContextNames.JAVA_MODULE_NAME);
        phaseContext.getServiceTarget().addService(moduleContextServiceName, contextService).install();

        // setup the module context binder
        final DeploymentUnit appContextDeploymentUnit = parentDeploymentUnit != null ? parentDeploymentUnit : deploymentUnit;
        final ApplicationBinder applicationBinder = appContextDeploymentUnit.getAttachment(Attachments.EE_JNDI_APP_CONTEXT_BINDER);
        final Map<String, ServiceName> sharedModuleContext = new HashMap<>(sharedContextBindings.getJavaModule());
        if (moduleDescription.isCompUsesModuleNamespace()) {
            sharedModuleContext.putAll(sharedContextBindings.getJavaComp());
        }
        deploymentUnit.putAttachment(Attachments.EE_JNDI_MODULE_CONTEXT_BINDER, new ModuleBinder(applicationBinder, sharedModuleContext));

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

        // add the java:module/ModuleName binding
        moduleDescription.getModuleBindingConfigurations().add(new BindingConfiguration(JNDI_NAME_java_module_ModuleName, new FixedInjectionSource(moduleDescription.getModuleName())));

        // add the java:module/env binding
        moduleDescription.getModuleBindingConfigurations().add(BINDING_CONFIGURATION_java_module_env);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
