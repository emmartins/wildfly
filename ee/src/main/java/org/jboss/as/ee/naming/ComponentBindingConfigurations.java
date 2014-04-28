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
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.naming.deployment.JndiName;

import java.util.HashSet;
import java.util.Set;

/**
 * The EE Component's bindings configuration.
 * @author Eduardo Martins
 */
public class ComponentBindingConfigurations extends BindingConfigurations {

    /**
     * the component's module binding configurations
     */
    private final ModuleBindingConfigurations moduleBindingsConfiguration;

    /**
     * the component's naming mode
     */
    private final ComponentNamingMode componentNamingMode;

    /**
     * the jndi names for all deployment bindings in java:comp namespace, in the scope of the component
     */
    private final Set<JndiName> compJndiNames = new HashSet<>();

    /**
     * the deployment bindings
     */
    private final DeploymentBindings deploymentBindings = new ComponentDeploymentBindings();

    /**
     *
     * @param moduleBindingsConfiguration
     * @param componentNamingMode
     */
    public ComponentBindingConfigurations(ModuleBindingConfigurations moduleBindingsConfiguration, ComponentNamingMode componentNamingMode) {
        this.moduleBindingsConfiguration = moduleBindingsConfiguration;
        this.moduleBindingsConfiguration.addComponent(this);
        this.componentNamingMode = componentNamingMode;
    }

    /**
     *
     * @return the component's naming mode
     */
    public ComponentNamingMode getComponentNamingMode() {
        return componentNamingMode;
    }

    @Override
    public synchronized void addAnnotationBinding(BindingConfiguration bindingConfiguration) {
        if (addToModule(bindingConfiguration.getName())) {
            moduleBindingsConfiguration.addAnnotationBinding(bindingConfiguration);
        } else {
            super.addAnnotationBinding(bindingConfiguration);
            compJndiNames.add(bindingConfiguration.getName());
        }
    }

    @Override
    public synchronized void addDeploymentBinding(BindingConfiguration bindingConfiguration) {
        if (addToModule(bindingConfiguration.getName())) {
            moduleBindingsConfiguration.addDeploymentBinding(bindingConfiguration);
        } else {
            super.addDeploymentBinding(bindingConfiguration);
            compJndiNames.add(bindingConfiguration.getName());
        }
    }

    @Override
    public synchronized void addPlatformBinding(BindingConfiguration bindingConfiguration) {
        if (addToModule(bindingConfiguration.getName())) {
            moduleBindingsConfiguration.addPlatformBinding(bindingConfiguration);
        } else {
            super.addPlatformBinding(bindingConfiguration);
            compJndiNames.add(bindingConfiguration.getName());
        }
    }

    /**
     *
     * @return the deployment binding, which contains all binding jndi names to be done by the deployment, in the scope of the component.
     */
    public DeploymentBindings getDeploymentBindings() {
        return deploymentBindings;
    }

    private boolean addToModule(JndiName jndiName) {
        return !jndiName.isJavaComp() || componentNamingMode != ComponentNamingMode.CREATE;
    }

    /**
     *
     */
    private class ComponentDeploymentBindings implements DeploymentBindings {
        @Override
        public boolean contains(JndiName jndiName) {
            if (addToModule(jndiName)) {
                return moduleBindingsConfiguration.getDeploymentBindings().contains(jndiName);
            } else {
                return compJndiNames.contains(jndiName);
            }
        }
    }
}
