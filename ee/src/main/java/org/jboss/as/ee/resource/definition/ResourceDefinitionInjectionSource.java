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

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.metadata.javaee.spec.PropertiesMetaData;
import org.jboss.metadata.javaee.spec.PropertyMetaData;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eduardo Martins
 */
public abstract class ResourceDefinitionInjectionSource extends InjectionSource {

    protected final String jndiName;
    protected final Map<String, String> properties = new HashMap<>();

    public ResourceDefinitionInjectionSource(final String jndiName) {
        if (jndiName.startsWith("java:")) {
            this.jndiName = jndiName;
        } else {
            this.jndiName = "java:comp/env/" + jndiName;
        }
    }

    public String getJndiName() {
        return jndiName;
    }

    public void addProperties(final String[] annotationProperties) {
        if (annotationProperties != null) {
            for (String annotationProperty : annotationProperties) {
                final int index = annotationProperty.indexOf('=');
                if (index != -1) {
                    final String propertyName = annotationProperty.substring(0, index);
                    final String propertyValue = annotationProperty.length() > index ? annotationProperty.substring(index + 1, annotationProperty.length()) : "";
                    this.properties.put(propertyName, propertyValue);
                } else {
                    this.properties.put(annotationProperty,"");
                }
            }
        }
    }

    public void addProperties(final PropertiesMetaData descriptorProperties) {
        if (descriptorProperties != null) {
            for (PropertyMetaData descriptorProperty : descriptorProperties) {
                this.properties.put(descriptorProperty.getName(), descriptorProperty.getValue());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceDefinitionInjectionSource that = (ResourceDefinitionInjectionSource) o;

        if (!jndiName.equals(that.jndiName)) return false;
        if (!properties.equals(that.properties)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = jndiName.hashCode();
        result = 31 * result + properties.hashCode();
        return result;
    }
}
