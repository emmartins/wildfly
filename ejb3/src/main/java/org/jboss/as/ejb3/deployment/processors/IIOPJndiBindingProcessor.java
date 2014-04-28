/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.FixedInjectionSource;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ejb3.iiop.handle.HandleDelegateImpl;
import org.jboss.as.jacorb.deployment.JacORBDeploymentMarker;
import org.jboss.as.naming.ImmediateManagedReferenceFactory;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;

/**
 * Processor responsible for binding IIOP related resources to JNDI.
 * </p>
 * Unlike other resource injections this binding happens for all eligible components,
 * regardless of the presence of the {@link javax.annotation.Resource} annotation.
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class IIOPJndiBindingProcessor implements DeploymentUnitProcessor {

    private static final JndiName JNDI_NAME = new JndiName("java:comp/HandleDelegate");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // do not bind on EAR's modules or if jacORB not present
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit) || !JacORBDeploymentMarker.isJacORBDeployment(deploymentUnit)) {
            return;
        }
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (moduleDescription == null) {
            return;
        }
        // create the java:comp/HandleDelegate binding configuration
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final Object value = new HandleDelegateImpl(module.getClassLoader());
        final ImmediateManagedReferenceFactory immediateManagedReferenceFactory = new ImmediateManagedReferenceFactory(value);
        final FixedInjectionSource fixedInjectionSource = new FixedInjectionSource(immediateManagedReferenceFactory, module.getClassLoader());
        moduleDescription.getBindingConfigurations().addPlatformBindingToAllComponents(new BindingConfiguration(JNDI_NAME, fixedInjectionSource));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
