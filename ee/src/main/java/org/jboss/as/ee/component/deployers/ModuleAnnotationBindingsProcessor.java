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
import org.jboss.as.ee.component.ClassDescriptionTraversal;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.naming.ComponentBindingConfigurations;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassIndex;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;

import java.util.HashSet;
import java.util.Set;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * Processor which adds JNDI bindings defined by annotations found on EE component classes.
 *
 * @author Eduardo Martins
 */
public class ModuleAnnotationBindingsProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final DeploymentClassIndex classIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CLASS_INDEX);
        for (final ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
            final ComponentBindingConfigurations componentBindingsConfiguration = componentDescription.getBindingConfigurations();
            if (!MetadataCompleteMarker.isMetadataComplete(phaseContext.getDeploymentUnit())) {
                final Set<Class<?>> classConfigurations = new HashSet<>();
                final Class<?> componentClass;
                try {
                    componentClass = classIndex.classIndex(componentDescription.getComponentClassName()).getModuleClass();
                    classConfigurations.add(componentClass);
                } catch (ClassNotFoundException e) {
                    throw ROOT_LOGGER.couldNotLoadComponentClass(e, componentDescription.getComponentClassName());
                }
                for (final InterceptorDescription interceptor : componentDescription.getAllInterceptors()) {
                    try {
                        final ClassIndex interceptorClass = classIndex.classIndex(interceptor.getInterceptorClassName());
                        classConfigurations.add(interceptorClass.getModuleClass());
                    } catch (ClassNotFoundException e) {
                        throw ROOT_LOGGER.cannotLoadInterceptor(e, interceptor.getInterceptorClassName(), componentClass);
                    }
                }
                for (final Class<?> clazz : classConfigurations) {
                    new ClassDescriptionTraversal(clazz, applicationClasses) {
                        @Override
                        protected void handle(final Class<?> currentClass, final EEModuleClassDescription classDescription) throws DeploymentUnitProcessingException {
                            if (classDescription == null) {
                                return;
                            }
                            if (classDescription.isInvalid()) {
                                throw ROOT_LOGGER.componentClassHasErrors(classDescription.getClassName(), componentDescription.getComponentName(), classDescription.getInvalidMessage());
                            }
                            for (BindingConfiguration binding : classDescription.getBindingConfigurations()) {
                                componentBindingsConfiguration.addAnnotationBinding(binding);
                            }
                        }
                    }.run();
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
