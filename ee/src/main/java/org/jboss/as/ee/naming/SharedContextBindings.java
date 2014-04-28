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

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.util.Map;

/**
 *
 * Provides bindings in the shared java:comp, java:module and java:app contexts.
 * @author Eduardo Martins
 */
public class SharedContextBindings  {

    private final Map<String, ServiceName> javaApp;
    private final Map<String, ServiceName> javaModule;
    private final Map<String, ServiceName> javaComp;

    public SharedContextBindings(Map<String, ServiceName> javaApp, Map<String, ServiceName> javaModule, Map<String, ServiceName> javaComp) {
        this.javaApp = javaApp;
        this.javaModule = javaModule;
        this.javaComp = javaComp;
    }

    /**
     * Retrieves the bindings in the shared java:app context.
     * @return
     */
    public Map<String, ServiceName> getJavaApp() {
        return javaApp;
    }

    /**
     * Retrieves the bindings in the shared java:module context.
     * @return
     */
    public Map<String, ServiceName> getJavaModule() {
        return javaModule;
    }

    /**
     * Retrieves the bindings in the shared java:comp context.
     * @return
     */
    public Map<String, ServiceName> getJavaComp() {
        return javaComp;
    }

    /**
     * Processor that gathers the shared jndi contexts bindings and stores it as a DU attachment.
     */
    public static class Processor implements DeploymentUnitProcessor {

        @Override
        public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            if (deploymentUnit.getParent() != null) {
                return;
            }
            final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            if (moduleDescription == null) {
                return;
            }
            try {
                final ServiceController<ServiceBasedNamingStore> sharedAppContextServiceController = (ServiceController<ServiceBasedNamingStore>) phaseContext.getServiceRegistry().getService(ContextNames.SHARED_APP_CONTEXT_SERVICE_NAME);
                final Map<String, ServiceName> sharedAppContextBindings = sharedAppContextServiceController.getService().getValue().getBoundServicesMap();
                final ServiceController<ServiceBasedNamingStore> sharedModuleContextServiceController = (ServiceController<ServiceBasedNamingStore>) phaseContext.getServiceRegistry().getService(ContextNames.SHARED_MODULE_CONTEXT_SERVICE_NAME);
                final Map<String, ServiceName> sharedModuleContextBindings = sharedModuleContextServiceController.getService().getValue().getBoundServicesMap();
                final ServiceController<ServiceBasedNamingStore> sharedCompContextServiceController = (ServiceController<ServiceBasedNamingStore>) phaseContext.getServiceRegistry().getService(ContextNames.SHARED_COMP_CONTEXT_SERVICE_NAME);
                final Map<String, ServiceName>  sharedCompContextBindings = sharedCompContextServiceController.getService().getValue().getBoundServicesMap();
                deploymentUnit.putAttachment(Attachments.EE_JNDI_SHARED_CONTEXTS_BINDINGS, new SharedContextBindings(sharedAppContextBindings, sharedModuleContextBindings, sharedCompContextBindings));
            } catch (Throwable e) {
                throw EeLogger.ROOT_LOGGER.failedToObtainSharedContextBindings(e);
            }
        }

        @Override
        public void undeploy(DeploymentUnit context) {

        }

    }
}
