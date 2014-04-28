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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The EE Module's bindings configuration manages the binds for the global and scoped java:module and java:app namespaces.
 * @author Eduardo Martins
 */
public class ModuleBindingConfigurations extends BindingConfigurations {

    private final List<ComponentBindingConfigurations> components = new ArrayList<>();
    private final List<BindingConfiguration> sharedComponentPlatformBindings = new ArrayList<>();

    /**
     * the jndi names for all deployment bindings in java:app and global namespaces
     */
    private final Set<JndiName> appAndGlobalJndiNames;

    /**
     * the jndi names for all deployment bindings in java:module namespace
     */
    private final Set<JndiName> moduleJndiNames = new HashSet<>();

    /**
     * the deployment bindings
     */
    private final DeploymentBindings deploymentBindings = new ModuleDeploymentBindings();

    /**
     *  @param parent
     */
    public ModuleBindingConfigurations(ModuleBindingConfigurations parent) {
        if (parent != null) {
            appAndGlobalJndiNames = parent.appAndGlobalJndiNames;
        } else {
            appAndGlobalJndiNames = new HashSet<>();
        }
    }

    @Override
    public synchronized void addAnnotationBinding(BindingConfiguration bindingConfiguration) {
        bindingConfiguration = processBindingConfiguration(bindingConfiguration);
        addToBindingNames(bindingConfiguration.getName());
        super.addAnnotationBinding(bindingConfiguration);
    }

    @Override
    public synchronized void addDeploymentBinding(BindingConfiguration bindingConfiguration) {
        bindingConfiguration = processBindingConfiguration(bindingConfiguration);
        addToBindingNames(bindingConfiguration.getName());
        super.addDeploymentBinding(bindingConfiguration);
    }

    @Override
    public synchronized void addPlatformBinding(BindingConfiguration bindingConfiguration) {
        bindingConfiguration = processBindingConfiguration(bindingConfiguration);
        addToBindingNames(bindingConfiguration.getName());
        super.addPlatformBinding(bindingConfiguration);
    }

    /**
     * Adds a module component.
     * @param component
     */
    public synchronized void addComponent(ComponentBindingConfigurations component) {
        components.add(component);
        if (component.getComponentNamingMode() == ComponentNamingMode.CREATE) {
            // add all shared platform bindings
            for (BindingConfiguration bindingConfiguration : sharedComponentPlatformBindings) {
                component.addPlatformBinding(bindingConfiguration);
            }
        }
    }

    /**
     * Adds the specified configuration to the platform bindings of all components
     * @param bindingConfiguration
     */
    public synchronized void addPlatformBindingToAllComponents(BindingConfiguration bindingConfiguration) {
        // add to module
        addPlatformBinding(bindingConfiguration);
        // and add to all components with own java:comp
        for (ComponentBindingConfigurations component : components) {
            if (component.getComponentNamingMode() == ComponentNamingMode.CREATE) {
                component.addPlatformBinding(bindingConfiguration);
            }
        }
        // keep it, new components may be added
        sharedComponentPlatformBindings.add(bindingConfiguration);
    }

    /**
     *
     * @return the deployment binding, which contains all binding jndi names to be done by the deployment, in the scope of the module.
     */
    public DeploymentBindings getDeploymentBindings() {
        return deploymentBindings;
    }

    private BindingConfiguration processBindingConfiguration(BindingConfiguration bindingConfiguration) {
        final JndiName jndiName = bindingConfiguration.getName();
        if (jndiName.isJavaComp()) {
            // convert name
            return new BindingConfiguration(convertJavaCompJndiName(jndiName), bindingConfiguration.getSource());
        } else {
            return bindingConfiguration;
        }
    }

    private void addToBindingNames(JndiName jndiName) {
        if (jndiName.isJavaModule()) {
            moduleJndiNames.add(jndiName);
        } else {
            appAndGlobalJndiNames.add(jndiName);
        }
    }

    private JndiName convertJavaCompJndiName(JndiName jndiName) {
        return new JndiName("java:module" + jndiName.getAbsoluteName().substring("java:comp".length()));
    }

    /**
     *
     */
    private class ModuleDeploymentBindings implements DeploymentBindings {
        @Override
        public boolean contains(JndiName jndiName) {
            if (jndiName.isJavaComp()) {
                jndiName = convertJavaCompJndiName(jndiName);
            }
            return moduleJndiNames.contains(jndiName) || appAndGlobalJndiNames.contains(jndiName);
        }
    }
}