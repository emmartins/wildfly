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

import static org.jboss.as.ejb.http.extension.EjbOverHttpLogger.LOGGER;

import java.util.List;

import org.apache.catalina.Context;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

/**
 * @author sfcoy
 */
public class EjbOverHttpServletDeployerServiceAddStepHandler extends AbstractAddStepHandler {

    static final EjbOverHttpServletDeployerServiceAddStepHandler INSTANCE = new EjbOverHttpServletDeployerServiceAddStepHandler();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        ConnectorResourceDefinition.VIRTUAL_HOST_ATTR.validateAndSet(operation, model);
        ConnectorResourceDefinition.CONTEXT_ATTR.validateAndSet(operation, model);
        ConnectorResourceDefinition.SECURITY_DOMAIN_ATTR.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  final ServiceVerificationHandler verificationHandler,
                                  final List<ServiceController<?>> newControllers) throws OperationFailedException {
        if (context.isNormalServer()) {

            ModelNode virtualHostModel
                    = ConnectorResourceDefinition.VIRTUAL_HOST_ATTR.resolveModelAttribute(context, model);
            final String virtualHost = virtualHostModel.asString();

            ModelNode contextModel
                    = ConnectorResourceDefinition.CONTEXT_ATTR.resolveModelAttribute(context, model);
            final String webContext = "/" + contextModel.asString();

            ModelNode securityRealmModel
                    = ConnectorResourceDefinition.SECURITY_DOMAIN_ATTR.resolveModelAttribute(context, model);
            final String securityRealm = securityRealmModel.isDefined() ? securityRealmModel.asString() : null;

            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(OperationContext context, ModelNode operation) {

                    final EjbOverHttpServletDeployerService ejbOverHttpContextService = new EjbOverHttpServletDeployerService(webContext,
                            securityRealm);

                    context.getServiceTarget().addService(WebSubsystemServices.JBOSS_WEB.append
                            (EjbOverHttpExtension.SUBSYSTEM_NAME).append(webContext), ejbOverHttpContextService);


                    ServiceBuilder<Context> ejbOverHttpServletDeployerServiceServiceBuilder =
                            context.getServiceTarget().addService(EjbOverHttpServletDeployerService.SERVICE_NAME.append(webContext), ejbOverHttpContextService);
                    ejbOverHttpServletDeployerServiceServiceBuilder.addDependency(EJBRemoteConnectorService.SERVICE_NAME, EJBRemoteConnectorService.class, ejbOverHttpContextService.getEjbRemoteConnectorService())
                    .addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(virtualHost), VirtualHost.class,
                            ejbOverHttpContextService.getVirtualHostInjector())
                    .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class,
                                    ejbOverHttpContextService.getWebServerInjector());

                    if (securityRealm != null)
                        ejbOverHttpServletDeployerServiceServiceBuilder.addDependency(SecurityDomainService.SERVICE_NAME.append(securityRealm), SecurityDomainContext.class,
                                ejbOverHttpContextService.getSecurityDomainContextInjector());

                    newControllers.add(ejbOverHttpServletDeployerServiceServiceBuilder.addListener(verificationHandler)
                            .setInitialMode(ServiceController.Mode.ACTIVE)
                            .install());
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);

            context.stepCompleted();
        } else
            LOGGER.ejbOverHttpServiceNotAvailable();
    }
}
