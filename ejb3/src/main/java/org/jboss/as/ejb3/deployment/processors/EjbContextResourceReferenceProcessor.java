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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessorRegistry;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import javax.ejb.EJBContext;
import javax.ejb.EntityContext;
import javax.ejb.MessageDrivenContext;
import javax.ejb.SessionContext;

/**
 * Deployment processor responsible for registering the EJB Context Resource Ref Processors.
 *
 * @author John Bailey
 */
public class EjbContextResourceReferenceProcessor implements DeploymentUnitProcessor {

    /**
     * {@inheritDoc} *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEResourceReferenceProcessorRegistry registry = deploymentUnit.getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY);

        //setup ejb context jndi handlers
        registry.registerResourceReferenceProcessor(new org.jboss.as.ejb3.context.EjbContextResourceReferenceProcessor(EJBContext.class));
        registry.registerResourceReferenceProcessor(new org.jboss.as.ejb3.context.EjbContextResourceReferenceProcessor(SessionContext.class));
        registry.registerResourceReferenceProcessor(new org.jboss.as.ejb3.context.EjbContextResourceReferenceProcessor(EntityContext.class));
        registry.registerResourceReferenceProcessor(new org.jboss.as.ejb3.context.EjbContextResourceReferenceProcessor(MessageDrivenContext.class));
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }

}
