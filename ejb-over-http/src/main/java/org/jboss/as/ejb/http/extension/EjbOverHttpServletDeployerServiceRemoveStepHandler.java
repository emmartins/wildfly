/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb.http.extension;

import org.apache.catalina.Context;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author sfcoy
 * @author martins
 */
public class EjbOverHttpServletDeployerServiceRemoveStepHandler extends AbstractRemoveStepHandler {

    static final EjbOverHttpServletDeployerServiceRemoveStepHandler INSTANCE
            = new EjbOverHttpServletDeployerServiceRemoveStepHandler();

    @Override
    protected void performRuntime(OperationContext operationContext, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        ModelNode contextModel
                = ConnectorResourceDefinition.CONTEXT_PATH_ATTR.resolveModelAttribute(operationContext, model);
        final String contextPath = contextModel.asString();

        ModelNode virtualHostModel = ConnectorResourceDefinition.VIRTUAL_HOST_ATTR.resolveModelAttribute(operationContext, model);
        final String virtualHost = virtualHostModel.asString();

        final ServiceName ejbOverHttpServletDeployerServiceName = EjbOverHttpServletDeployerService.SERVICE_NAME.append(virtualHost, contextPath);
        EjbOverHttpLogger.LOGGER.infof("Removing %s", ejbOverHttpServletDeployerServiceName);
        final Context context = (Context) operationContext.removeService(ejbOverHttpServletDeployerServiceName).getValue();
        if (context.getRealm() != null) {
            final ServiceName realmServiceName = WebSubsystemServices.deploymentServiceName(virtualHost, contextPath).append("realm");
            EjbOverHttpLogger.LOGGER.infof("Removing %s", realmServiceName);
            operationContext.removeService(realmServiceName);
        }

    }
}
