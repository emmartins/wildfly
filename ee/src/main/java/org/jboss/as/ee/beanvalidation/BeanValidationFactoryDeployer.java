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
package org.jboss.as.ee.beanvalidation;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.FixedInjectionSource;
import org.jboss.as.ee.naming.ModuleBindingConfigurations;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;

import javax.validation.ValidatorFactory;

/**
 * Creates a bean validation factory and adds it to the deployment and binds it to JNDI.
 * <p/>
 * We use a lazy wrapper around the ValidatorFactory to stop it being initialized until it is used.
 * TODO: it would be neat if hibernate validator could make use of our annotation scanning etc
 *
 * @author Stuart Douglas
 */
public class BeanValidationFactoryDeployer implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        if(module == null || moduleDescription == null) {
            return;
        }
        final LazyValidatorFactory factory  = new LazyValidatorFactory(module.getClassLoader());
        deploymentUnit.putAttachment(BeanValidationAttachments.VALIDATOR_FACTORY, factory);

        // add the bindings to the module description (except for EARs)
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        final ModuleBindingConfigurations moduleBindingConfigurations = moduleDescription.getBindingConfigurations();
        moduleBindingConfigurations.addPlatformBindingToAllComponents(new BindingConfiguration("java:comp/ValidatorFactory", new FixedInjectionSource(factory)));
        moduleBindingConfigurations.addPlatformBindingToAllComponents(new BindingConfiguration("java:comp/Validator", new FixedInjectionSource(new ValidatorJndiInjectable(factory), factory)));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        ValidatorFactory validatorFactory = context.getAttachment(BeanValidationAttachments.VALIDATOR_FACTORY);
        if ((validatorFactory != null) && (!WeldDeploymentMarker.isPartOfWeldDeployment(context))) {
            // If the ValidatorFactory is not CDI-enabled, close it here. Otherwise, it's
            // closed via CdiValidatorFactoryService before the Weld service is stopped.
            validatorFactory.close();
        }
        context.removeAttachment(BeanValidationAttachments.VALIDATOR_FACTORY);
    }

}
