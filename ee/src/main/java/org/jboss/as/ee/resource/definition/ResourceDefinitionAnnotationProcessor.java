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
package org.jboss.as.ee.resource.definition;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import java.util.List;

import static org.jboss.as.ee.EeMessages.MESSAGES;

/**
 * The foundation to create processors wrt deployment of classes annotated with EE Resource Definitions, as defined by EE.5.18.
 *
 * @author Eduardo Martins
 */
public abstract class ResourceDefinitionAnnotationProcessor implements DeploymentUnitProcessor {

    /**
     *
     * @return
     */
    protected abstract DotName getAnnotationDotName();

    /**
     *
     * @return
     */
    protected abstract DotName getAnnotationCollectionDotName();

    protected abstract ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance) throws DeploymentUnitProcessingException;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) {
            return;
        }
        final DotName annotationName = getAnnotationDotName();
        for (AnnotationInstance annotationInstance : index.getAnnotations(annotationName)) {
            final List<BindingConfiguration> bindingConfigurations = getAnnotatedClassBindingConfigurations(moduleDescription, annotationInstance);
            final ResourceDefinitionInjectionSource injectionSource = processAnnotation(annotationInstance);
            bindingConfigurations.add(new BindingConfiguration(injectionSource.getJndiName(),injectionSource));
        }
        final DotName collectionAnnotationName = getAnnotationCollectionDotName();
        if (collectionAnnotationName != null) {
            for (AnnotationInstance annotationInstance : index.getAnnotations(collectionAnnotationName)) {
                final AnnotationInstance[] nestedAnnotationInstances = annotationInstance.value().asNestedArray();
                if (nestedAnnotationInstances != null && nestedAnnotationInstances.length > 0) {
                    final List<BindingConfiguration> bindingConfigurations = getAnnotatedClassBindingConfigurations(moduleDescription, annotationInstance);
                    for (AnnotationInstance nestedAnnotationInstance : nestedAnnotationInstances) {
                        final ResourceDefinitionInjectionSource injectionSource = processAnnotation(nestedAnnotationInstance);
                        bindingConfigurations.add(new BindingConfiguration(injectionSource.getJndiName(),injectionSource));
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    private List<BindingConfiguration> getAnnotatedClassBindingConfigurations(EEModuleDescription moduleDescription, AnnotationInstance annotationInstance) throws DeploymentUnitProcessingException {
        final AnnotationTarget target = annotationInstance.target();
        if (!(target instanceof ClassInfo)) {
            throw MESSAGES.classOnlyAnnotation(annotationInstance.toString(), target);
        }
        final ClassInfo classInfo = (ClassInfo) target;
        return moduleDescription.addOrGetLocalClassDescription(classInfo.name().toString()).getBindingConfigurations();
    }

    /**
     * Utility class to help handle resource definition annotation elements
     */
    public static class AnnotationElement {

        public static final String NAME = "name";
        public static final String PROPERTIES = "properties";

        public static boolean asOptionalBoolean(final AnnotationInstance annotation, String property) {
            AnnotationValue value = annotation.value(property);
            return value == null ? true : value.asBoolean();
        }

        public static int asOptionalInt(AnnotationInstance annotation, String string) {
            AnnotationValue value = annotation.value(string);
            return value == null ? -1 : value.asInt();
        }

        public static int asOptionalInt(final AnnotationInstance annotation, String property, int defaultValue) {
            AnnotationValue value = annotation.value(property);
            return value == null ? defaultValue : value.asInt();
        }

        public static String asOptionalString(final AnnotationInstance annotation, String property) {
            return asOptionalString(annotation, property, "");
        }

        public static String asOptionalString(final AnnotationInstance annotation, String property, String defaultValue) {
            AnnotationValue value = annotation.value(property);
            return value == null ? defaultValue : value.asString().isEmpty() ? defaultValue : value.asString();
        }

        public static String[] asOptionalStringArray(final AnnotationInstance annotation, String property) {
            AnnotationValue value = annotation.value(property);
            return value == null ? new String[0] : value.asStringArray();
        }

        public static String asRequiredString(final AnnotationInstance annotationInstance, final String attributeName) {
            final AnnotationValue nameValue = annotationInstance.value(attributeName);
            if (nameValue == null) {
                throw MESSAGES.annotationAttributeMissing(annotationInstance.name().toString(), attributeName);
            }
            final String nameValueAsString = nameValue.asString();
            if (nameValueAsString.isEmpty()) {
                throw MESSAGES.annotationAttributeMissing(annotationInstance.name().toString(), attributeName);
            }
            return nameValueAsString;
        }

    }

}
