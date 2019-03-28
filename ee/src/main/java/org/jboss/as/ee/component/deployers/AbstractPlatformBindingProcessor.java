/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentNamingMode;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import static org.jboss.as.ee.structure.DeploymentType.*;

/**
 * Foundation for processors which binds EE platform common resources, to all EE module and comp naming contexts.
 *
 * @author emmartins
 */
public abstract class AbstractPlatformBindingProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if(moduleDescription == null) {
            return;
        }
        if (DeploymentTypeMarker.isType(EAR, deploymentUnit)) {
            return;
        }
        addBindings(deploymentUnit, moduleDescription);
    }

    /**
     * Concrete implementations should use this method to add bindings to the module description, through {@link #addBinding(String, String, DeploymentUnit, EEModuleDescription)}
     * @param deploymentUnit
     * @param moduleDescription
     */
    protected abstract void addBindings(DeploymentUnit deploymentUnit, EEModuleDescription moduleDescription);

    /**
     *
     * @param source
     * @param target
     * @param moduleDescription
     */
    protected void addBinding(final String source, String target, DeploymentUnit deploymentUnit, EEModuleDescription moduleDescription) {
        final LookupInjectionSource injectionSource = new LookupInjectionSource(source);
        final String moduleTarget = target.startsWith("java:comp") ? "java:module"+target.substring("java:comp".length()) : target;
        if (DeploymentTypeMarker.isType(WAR, deploymentUnit)) {
            moduleDescription.getBindingConfigurations().add(new BindingConfiguration(moduleTarget, injectionSource));
            System.out.println("Added module binding "+moduleTarget+" to EE module "+moduleDescription.getApplicationName() + ":"+moduleDescription.getModuleName());
        } else {
            if (DeploymentTypeMarker.isType(APPLICATION_CLIENT, deploymentUnit)) {
                moduleDescription.getBindingConfigurations().add(new BindingConfiguration(target, injectionSource));
                System.out.println("Added module binding "+target+" to EE module "+moduleDescription.getApplicationName() + ":"+moduleDescription.getModuleName());
            } else {
                moduleDescription.getBindingConfigurations().add(new BindingConfiguration(moduleTarget, injectionSource));
                System.out.println("Added module binding "+moduleTarget+" to EE module "+moduleDescription.getApplicationName() + ":"+moduleDescription.getModuleName());
            }
            for(ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
                if(componentDescription.getNamingMode() == ComponentNamingMode.CREATE) {
                    componentDescription.getBindingConfigurations().add(new BindingConfiguration(target, injectionSource));
                    System.out.println("Added comp binding "+target+" to EE component "+componentDescription.getApplicationName() + ":"+componentDescription.getModuleName()+":"+componentDescription.getComponentName());
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
