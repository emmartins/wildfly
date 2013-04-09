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
import static org.jboss.as.ejb.http.extension.EjbOverHttpMessages.MESSAGES;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.servlet.HttpConstraintElement;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.as.ejb.http.remote.HttpEJBClientMessageReceiver;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.web.AuthenticatorValve;
import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.WebServerService;
import org.jboss.as.web.deployment.WebCtxLoader;
import org.jboss.as.web.security.SecurityContextAssociationValve;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author sfcoy
 * @author Eduardo Martins
 */
class EjbOverHttpServletDeployerService implements Service<Context> {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(EjbOverHttpExtension.SUBSYSTEM_NAME);

    static final String SERVLET_NAME_PATTERN = "ejb-over-http-servlet:%s:%s";

    private final StandardContext context = new StandardContext();

    private final InjectedValue<EJBRemoteConnectorService> ejbRemoteConnectorService = new InjectedValue<EJBRemoteConnectorService>();
    private final InjectedValue<VirtualHost> virtualHostInjector = new InjectedValue<VirtualHost>();
    private final InjectedValue<WebServerService> webServerInjector = new InjectedValue<WebServerService>();
    private final InjectedValue<Realm> realmInjector = new InjectedValue<Realm>();

    private final String allowedRoleNames;
    private final String contextPath;
    private final LoginConfig loginConfig;
    private final String securityDomain;

    EjbOverHttpServletDeployerService(String allowedRoleNames, String contextPath, LoginConfig loginConfig, String securityDomain) {
        this.allowedRoleNames = allowedRoleNames;
        this.contextPath = contextPath;
        this.loginConfig = loginConfig;
        this.securityDomain = securityDomain;
    }

    InjectedValue<VirtualHost> getVirtualHostInjector() {
        return virtualHostInjector;
    }

    InjectedValue<WebServerService> getWebServerInjector() {
        return webServerInjector;
    }

    InjectedValue<Realm> getRealmInjector() {
        return realmInjector;
    }

    InjectedValue<EJBRemoteConnectorService> getEjbRemoteConnectorService() {
        return ejbRemoteConnectorService;
    }

    @Override
    public synchronized Context getValue() throws IllegalStateException, IllegalArgumentException {
        return context;
    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {

        final Host host = getVirtualHostInjector().getValue().getHost();
        EjbOverHttpLogger.LOGGER.deployingServlet(contextPath, host.getName());

        try {

            if (securityDomain != null) {
                context.addValve(new SecurityContextAssociationValve(securityDomain, host.getName()+contextPath, null));
            }
            context.addLifecycleListener(new ContextConfig(webServerInjector.getValue()));
            context.setPath(contextPath);
            context.setDocBase("");
            context.setInstanceManager(new LocalInstanceManager(this.ejbRemoteConnectorService.getValue()));
            final Loader webCtxLoader = new WebCtxLoader(this.getClass().getClassLoader());
            webCtxLoader.setContainer(host);
            context.setLoader(webCtxLoader);

            // setup the servlet
            final EJBRemoteConnectorService ejbRemoteConnectorService = this.ejbRemoteConnectorService.getValue();
            final HttpEJBClientMessageReceiver messageReceiver = new HttpEJBClientMessageReceiver(ejbRemoteConnectorService.getExecutorService().getValue(), ejbRemoteConnectorService.getDeploymentRepositoryInjector().getValue(),
                    ejbRemoteConnectorService.getEJBRemoteTransactionsRepositoryInjector().getValue(), ejbRemoteConnectorService.getAsyncInvocationCancelStatusInjector().getValue(),
                    ejbRemoteConnectorService.getSupportedMarshallingStrategies());
            final EjbOverHttpRemoteServlet httpEJBRemoteServlet = new EjbOverHttpRemoteServlet(messageReceiver);
            Wrapper servletWrapper = context.createWrapper();
            servletWrapper.setName(String.format(SERVLET_NAME_PATTERN, host.getName(), contextPath));
            servletWrapper.setServletClass(EjbOverHttpRemoteServlet.class.getName());
            servletWrapper.setServlet(httpEJBRemoteServlet);
            servletWrapper.setAsyncSupported(true);
            context.addChild(servletWrapper);
            context.addServletMapping("/", servletWrapper.getName());

            if (allowedRoleNames != null) {
                for(String roleName : allowedRoleNames.split(",")) {
                    context.addSecurityRole(roleName);
                }
                HttpConstraintElement hce = new HttpConstraintElement(TransportGuarantee.NONE,"*");
                ServletSecurityElement sse = new ServletSecurityElement(hce);
                servletWrapper.setServletSecurity(sse);
            }
            final Realm realm = realmInjector.getOptionalValue();
            if (realm != null) {
                context.setRealm(realm);
            }
            if(loginConfig != null) {
                context.setLoginConfig(loginConfig);
            }

            host.addChild(context);
            context.create();

        } catch (Exception e) {
            throw new StartException(MESSAGES.createEjbOverHttpServletFailed(contextPath), e);
        }

        try {
            LOGGER.startingService(host.getName(), contextPath);
            context.start();
        } catch (LifecycleException e) {
            throw new StartException(MESSAGES.startEjbOverHttpServletFailed(host.getName(), contextPath), e);
        }
    }

    private static class ContextConfig extends org.apache.catalina.startup.ContextConfig {

        final WebServerService webServerService;

        public ContextConfig(WebServerService webServerService) {
            this.webServerService = webServerService;
        }

        protected void completeConfig() {

            super.completeConfig();
            super.resolveServletSecurity();
            super.validateSecurityRoles();

            // Configure configure global authenticators.
            if (ok) {
                Map<String, AuthenticatorValve> authenValves = webServerService.getAuthenValves();
                if (!authenValves.isEmpty()) {
                    Map<String, Valve> authenvalves = new HashMap<String, Valve>();
                    for (String name : authenValves.keySet()) {
                        // Instantiate valve and add properties.
                        AuthenticatorValve authenvalve = (AuthenticatorValve) authenValves.get(name);
                        Valve valve = null;
                        try {
                            valve = (Valve) authenvalve.classz.newInstance();
                        } catch (InstantiationException e) {
                            ok = false;
                            break;
                        } catch (IllegalAccessException e) {
                            ok = false;
                            break;
                        }
                        if (authenvalve.properties != null) {
                            for (String pro: authenvalve.properties.keySet()) {
                                IntrospectionUtils.setProperty(valve, pro, authenvalve.properties.get(pro));
                            }
                        }
                        authenvalves.put(name, valve);
                    }
                    if (ok) {
                        setCustomAuthenticators(authenvalves);
                    }
                }
            }

            super.authenticatorConfig();
        }
    }

    @Override
    public synchronized void stop(StopContext stopContext) {
        final Host host = getVirtualHostInjector().getValue().getHost();
        LOGGER.stoppingService(contextPath, host.getName());
        try {
            host.removeChild(context);
            context.stop();
        } catch (LifecycleException e) {
            LOGGER.failedToStopCatalinaStandardContext(contextPath, host.getName(), e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            LOGGER.failedToDestroyCatalinaStandardContext(contextPath, host.getName(), e);
        }
    }

    private static class LocalInstanceManager implements InstanceManager {

        private final EJBRemoteConnectorService ejbRemoteConnectorService;

        LocalInstanceManager(EJBRemoteConnectorService ejbRemoteConnectorService) {
            this.ejbRemoteConnectorService = ejbRemoteConnectorService;
        }

        @Override
        public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
            if(className.equals(EjbOverHttpRemoteServlet.class.getName()) == false) {
                return Class.forName(className).newInstance();
            }
            final HttpEJBClientMessageReceiver messageReceiver = new HttpEJBClientMessageReceiver(ejbRemoteConnectorService.getExecutorService().getValue(), ejbRemoteConnectorService.getDeploymentRepositoryInjector().getValue(),
                    ejbRemoteConnectorService.getEJBRemoteTransactionsRepositoryInjector().getValue(), ejbRemoteConnectorService.getAsyncInvocationCancelStatusInjector().getValue(),
                    ejbRemoteConnectorService.getSupportedMarshallingStrategies());
            EjbOverHttpRemoteServlet wccs = new EjbOverHttpRemoteServlet(messageReceiver);
            return wccs;
        }

        @Override
        public Object newInstance(String fqcn, ClassLoader classLoader) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
            return Class.forName(fqcn, false, classLoader).newInstance();
        }

        @Override
        public Object newInstance(Class<?> c) throws IllegalAccessException, InvocationTargetException, NamingException, InstantiationException {
            return c.newInstance();
        }

        @Override
        public void newInstance(Object o) throws IllegalAccessException, InvocationTargetException, NamingException {
            throw new IllegalStateException();
        }

        @Override
        public void destroyInstance(Object o) throws IllegalAccessException, InvocationTargetException {
        }
    }

}
