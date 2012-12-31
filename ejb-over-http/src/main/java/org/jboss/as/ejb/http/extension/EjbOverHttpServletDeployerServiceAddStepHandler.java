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
import org.apache.catalina.Realm;
import org.apache.catalina.deploy.LoginConfig;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.WebServerService;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.as.web.security.JBossWebRealmService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author sfcoy
 * @author martins
 */
public class EjbOverHttpServletDeployerServiceAddStepHandler extends AbstractAddStepHandler {

    static final EjbOverHttpServletDeployerServiceAddStepHandler INSTANCE = new EjbOverHttpServletDeployerServiceAddStepHandler();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        ConnectorResourceDefinition.ALLOWED_ROLE_NAMES_ATTR.validateAndSet(operation, model);
        ConnectorResourceDefinition.CONTEXT_PATH_ATTR.validateAndSet(operation, model);
        ConnectorResourceDefinition.LOGIN_AUTH_METHOD_ATTR.validateAndSet(operation, model);
        ConnectorResourceDefinition.LOGIN_REALM_NAME_ATTR.validateAndSet(operation, model);
        ConnectorResourceDefinition.SECURITY_DOMAIN_ATTR.validateAndSet(operation, model);
        ConnectorResourceDefinition.VIRTUAL_HOST_ATTR.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext operationContext, ModelNode operation, ModelNode model,
                                  final ServiceVerificationHandler verificationHandler,
                                  final List<ServiceController<?>> newControllers) throws OperationFailedException {
        if (operationContext.isNormalServer()) {

            ModelNode allowedRoleNamesModel = ConnectorResourceDefinition.ALLOWED_ROLE_NAMES_ATTR.resolveModelAttribute(operationContext, model);
            final String allowedRoleNames = allowedRoleNamesModel.isDefined() ? allowedRoleNamesModel.asString() : null;

            ModelNode contextPathModel = ConnectorResourceDefinition.CONTEXT_PATH_ATTR.resolveModelAttribute(operationContext, model);
            final String contextPath = contextPathModel.asString();

            ModelNode loginAuthMethodModel = ConnectorResourceDefinition.LOGIN_AUTH_METHOD_ATTR.resolveModelAttribute(operationContext, model);
            final String loginAuthMethod = loginAuthMethodModel.isDefined() ? loginAuthMethodModel.asString() : null;

            ModelNode loginRealmNameModel = ConnectorResourceDefinition.LOGIN_REALM_NAME_ATTR.resolveModelAttribute(operationContext, model);
            final String loginRealmName = loginRealmNameModel.isDefined() ? loginRealmNameModel.asString() : null;

            ModelNode securityDomainModel = ConnectorResourceDefinition.SECURITY_DOMAIN_ATTR.resolveModelAttribute(operationContext, model);
            final String securityDomain = securityDomainModel.isDefined() ? securityDomainModel.asString() : null;

            ModelNode virtualHostModel = ConnectorResourceDefinition.VIRTUAL_HOST_ATTR.resolveModelAttribute(operationContext, model);
            final String virtualHost = virtualHostModel.asString();

            final LoginConfig loginConfig;
            if (loginAuthMethod !=null && loginRealmName != null) {
                loginConfig = new LoginConfig();
                loginConfig.setAuthMethod(loginAuthMethod);
                loginConfig.setRealmName(loginRealmName);
            } else {
                loginConfig = null;
            }

            operationContext.addStep(new OperationStepHandler() {

                @Override
                public void execute(OperationContext operationContext, ModelNode operation) {

                    final ServiceName realmServiceName;
                    if (securityDomain != null) {
                        // to use servlet auth a realm msc service is needed
                        realmServiceName = WebSubsystemServices.deploymentServiceName(virtualHost, contextPath).append("realm");
                        final JBossWebRealmService realmService = new JBossWebRealmService(null);
                        final ServiceBuilder<Realm> realmServiceBuilder = operationContext.getServiceTarget().addService(realmServiceName, realmService)
                                .addDependency(DependencyType.REQUIRED, SecurityDomainService.SERVICE_NAME.append(securityDomain), SecurityDomainContext.class,
                                        realmService.getSecurityDomainContextInjector());
                        newControllers.add(realmServiceBuilder.addListener(verificationHandler)
                                .setInitialMode(ServiceController.Mode.ACTIVE)
                                .install());
                    } else {
                        realmServiceName = null;
                    }

                    final EjbOverHttpServletDeployerService ejbOverHttpContextService = new EjbOverHttpServletDeployerService(allowedRoleNames, contextPath, loginConfig, securityDomain);
                    ServiceBuilder<Context> ejbOverHttpServletDeployerServiceServiceBuilder =
                            operationContext.getServiceTarget().addService(EjbOverHttpServletDeployerService.SERVICE_NAME.append(virtualHost, contextPath), ejbOverHttpContextService);
                    ejbOverHttpServletDeployerServiceServiceBuilder.addDependency(EJBRemoteConnectorService.SERVICE_NAME, EJBRemoteConnectorService.class, ejbOverHttpContextService.getEjbRemoteConnectorService())
                    .addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(virtualHost), VirtualHost.class,
                            ejbOverHttpContextService.getVirtualHostInjector())
                    .addDependency(WebSubsystemServices.JBOSS_WEB, WebServerService.class,
                                    ejbOverHttpContextService.getWebServerInjector());
                    if (realmServiceName != null) {
                        ejbOverHttpServletDeployerServiceServiceBuilder.addDependency(realmServiceName, Realm.class, ejbOverHttpContextService.getRealmInjector());
                    }
                    newControllers.add(ejbOverHttpServletDeployerServiceServiceBuilder.addListener(verificationHandler)
                            .setInitialMode(ServiceController.Mode.ACTIVE)
                            .install());
                    operationContext.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);

            operationContext.stepCompleted();
        } else
            LOGGER.ejbOverHttpServiceNotAvailable();
    }
}
