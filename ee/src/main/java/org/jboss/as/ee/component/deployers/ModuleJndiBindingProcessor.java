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
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ClassDescriptionTraversal;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.naming.ApplicationBinder;
import org.jboss.as.ee.naming.ComponentBinder;
import org.jboss.as.ee.naming.ModuleBinder;
import org.jboss.as.ee.naming.SharedContextBindings;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.reflect.ClassIndex;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.msc.service.ServiceName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * Processor that sets up JNDI bindings that are owned by the module. It also handles class level jndi bindings
 * that belong to components that do not have their own java:comp namespace, and class level bindings declared in
 * namespaces above java:comp.
 * <p/>
 * This processor is also responsible for throwing an exception if any ee component classes have been marked as invalid.
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
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final DeploymentClassIndex classIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CLASS_INDEX);

        final String applicationName = moduleConfiguration.getApplicationName();
        final String moduleName = moduleConfiguration.getModuleName();
        final InjectionSource.ResolutionContext resolutionContext = new InjectionSource.ResolutionContext(
                true,
                moduleConfiguration.getModuleName(),
                moduleConfiguration.getModuleName(),
                moduleConfiguration.getApplicationName()
        );
        final ServiceVerificationHandler serviceVerificationHandler = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.SERVICE_VERIFICATION_HANDLER);
        final ApplicationBinder applicationBinder = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.EE_JNDI_APP_CONTEXT_BINDER);
        final ModuleBinder moduleBinder = deploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.EE_JNDI_MODULE_CONTEXT_BINDER);

        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            // put the module bindings in the app binder
            for (BindingConfiguration bindingConfiguration : eeModuleDescription.getModuleBindingConfigurations()) {
                applicationBinder.putBinding(ContextNames.bindInfoFor(applicationName, bindingConfiguration.getName()), bindingConfiguration);
            }
        } else {
            // add the module bindings
            for (BindingConfiguration bindingConfiguration : eeModuleDescription.getModuleBindingConfigurations()) {
                moduleBinder.putBinding(ContextNames.bindInfoFor(applicationName, moduleName, bindingConfiguration.getName()), bindingConfiguration);
            }
            // add all shared comp bindings to the module bindings if java:comp is java:module
            if (eeModuleDescription.isCompUsesModuleNamespace()) {
                for (BindingConfiguration bindingConfiguration : eeModuleDescription.getSharedCompBindingConfigurations()) {
                    moduleBinder.putBinding(ContextNames.bindInfoFor(applicationName, moduleName, bindingConfiguration.getName()), bindingConfiguration);
                }
            }
            // now we process all component level bindings, adding all entries not in java:comp namespace. If the module has a shared java:comp namespace then java:comp entries will also be added now
            // these are bindings that have been added via a deployment descriptor
            final DeploymentUnit parentDeploymentUnit = deploymentUnit.getParent();
            final DeploymentUnit appContextDeploymentUnit = parentDeploymentUnit != null ? parentDeploymentUnit : deploymentUnit;
            final SharedContextBindings sharedContextBindings = appContextDeploymentUnit.getAttachment(org.jboss.as.ee.naming.Attachments.EE_JNDI_SHARED_CONTEXTS_BINDINGS);
            for (final ComponentConfiguration componentConfiguration : moduleConfiguration.getComponentConfigurations()) {
                final ComponentDescription componentDescription = componentConfiguration.getComponentDescription();
                final ComponentBinder componentBinder = componentDescription.getNamingMode() == ComponentNamingMode.CREATE ? new ComponentBinder(sharedContextBindings.getJavaComp()) : null;
                componentConfiguration.setBinder(componentBinder);
                for (BindingConfiguration bindingConfiguration : componentConfiguration.getComponentDescription().getBindingConfigurations()) {
                    if (componentBinder != null && bindingConfiguration.getName().isJavaComp()) {
                        componentBinder.putBinding(ContextNames.bindInfoFor(applicationName, moduleName, componentConfiguration.getComponentName(), bindingConfiguration.getName()), bindingConfiguration);
                    } else {
                        moduleBinder.putBinding(ContextNames.bindInfoFor(applicationName, moduleName, bindingConfiguration.getName()), bindingConfiguration);
                    }
                }
                if (componentBinder != null) {
                    // add all shared comp binds
                    for (BindingConfiguration bindingConfiguration : eeModuleDescription.getSharedCompBindingConfigurations()) {
                        componentBinder.putBinding(ContextNames.bindInfoFor(applicationName, moduleName, componentConfiguration.getComponentName(), bindingConfiguration.getName()), bindingConfiguration);
                    }
                }
                if (!MetadataCompleteMarker.isMetadataComplete(phaseContext.getDeploymentUnit())) {
                    final Set<Class<?>> classConfigurations = new HashSet<>();
                    classConfigurations.add(componentConfiguration.getComponentClass());
                    for (final InterceptorDescription interceptor : componentConfiguration.getComponentDescription().getAllInterceptors()) {
                        try {
                            final ClassIndex interceptorClass = classIndex.classIndex(interceptor.getInterceptorClassName());
                            classConfigurations.add(interceptorClass.getModuleClass());
                        } catch (ClassNotFoundException e) {
                            throw ROOT_LOGGER.cannotLoadInterceptor(e, interceptor.getInterceptorClassName(), componentConfiguration.getComponentClass());
                        }
                    }
                    processClassConfigurations(applicationClasses, applicationName, moduleName, classConfigurations, componentConfiguration.getComponentName(), moduleBinder, componentBinder);
                }
            }
        }
        // finally do the app and/or module bindings
        final List<ServiceName> dependencies = deploymentUnit.getAttachmentList(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES);
        if (applicationBinder != null) {
            applicationBinder.doBindings(phaseContext, dependencies, resolutionContext, serviceVerificationHandler);
        }
        if (moduleBinder != null) {
            moduleBinder.doBindings(phaseContext, dependencies, resolutionContext, serviceVerificationHandler);
        }
    }

    private void processClassConfigurations(final EEApplicationClasses applicationClasses, final String applicationName, final String moduleName, final Set<Class<?>> classes, final String componentName, final ModuleBinder moduleBinder, final ComponentBinder componentBinder) throws DeploymentUnitProcessingException {
        final List<BindingConfiguration> moduleAnnotationBindings = new ArrayList<>();
        final List<BindingConfiguration> componentAnnotationBindings = componentBinder != null ? new ArrayList<BindingConfiguration>() : null;
        for (final Class<?> clazz : classes) {
            new ClassDescriptionTraversal(clazz, applicationClasses) {
                @Override
                protected void handle(final Class<?> currentClass, final EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
                    if (classDescription == null) {
                        return;
                    }
                    if (classDescription.isInvalid()) {
                        throw ROOT_LOGGER.componentClassHasErrors(classDescription.getClassName(), componentName, classDescription.getInvalidMessage());
                    }
                    final Set<BindingConfiguration> classLevelBindings = new HashSet<>(classDescription.getBindingConfigurations());
                    for (BindingConfiguration binding : classLevelBindings) {
                        if (componentBinder != null && binding.getName().isJavaComp()) {
                            componentAnnotationBindings.add(binding);
                        } else {
                            moduleAnnotationBindings.add(binding);
                        }
                    }
                }
            }.run();
        }
        for (BindingConfiguration bindingConfiguration : moduleAnnotationBindings) {
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(applicationName, moduleName, bindingConfiguration.getName());
            if (!moduleBinder.containsBinding(bindInfo)) {
                moduleBinder.putBinding(bindInfo, bindingConfiguration);
            }
        }
        if (componentBinder != null) {
            for (BindingConfiguration bindingConfiguration : componentAnnotationBindings) {
                final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(applicationName, moduleName, componentName, bindingConfiguration.getName());
                if (!componentBinder.containsBinding(bindInfo)) {
                    componentBinder.putBinding(bindInfo, bindingConfiguration);
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
