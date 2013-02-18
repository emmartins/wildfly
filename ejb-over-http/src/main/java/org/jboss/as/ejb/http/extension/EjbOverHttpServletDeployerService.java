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

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.jboss.as.ejb.http.remote.HttpEJBClientMessageReceiver;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.deployment.WebCtxLoader;
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

    static final String SERVLET_NAME = "ejb-over-http-servlet";

    private final StandardContext containerContext = new StandardContext();

    private final InjectedValue<EJBRemoteConnectorService> ejbRemoteConnectorService = new InjectedValue<EJBRemoteConnectorService>();
    private final InjectedValue<VirtualHost> virtualHostInjector = new InjectedValue<VirtualHost>();
    private final InjectedValue<WebServer> webServerInjector = new InjectedValue<WebServer>();
    private final InjectedValue<SecurityDomainContext> securityDomainContextInjector = new InjectedValue<SecurityDomainContext>();

    private final String webContext;

    private final String securityRealm;

    EjbOverHttpServletDeployerService(String webContext, String securityRealm) {
        this.webContext = webContext;
        this.securityRealm = securityRealm;
    }

    InjectedValue<VirtualHost> getVirtualHostInjector() {
        return virtualHostInjector;
    }

    public InjectedValue<WebServer> getWebServerInjector() {
        return webServerInjector;
    }

    InjectedValue<SecurityDomainContext> getSecurityDomainContextInjector() {
        return securityDomainContextInjector;
    }

    @Override
    public synchronized Context getValue() throws IllegalStateException, IllegalArgumentException {
        return containerContext;
    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {
        try {
            Host hostContainer = virtualHostInjector.getValue().getHost();
            SecurityDomainContext securityDomainContext = securityDomainContextInjector.getOptionalValue();
            if (securityDomainContext == null)
                EjbOverHttpLogger.LOGGER.deployingServlet(webContext, hostContainer.getName());
            else
                EjbOverHttpLogger.LOGGER.deployingServlet(webContext, hostContainer.getName(),
                        securityDomainContext.getAuthenticationManager().getSecurityDomain());
            prepareWebContainerContext(hostContainer);

        } catch (Exception e) {
            throw new StartException(MESSAGES.createEjbOverHttpServletFailed(webContext), e);
        }
        try {
            LOGGER.startingService(webContext);
            containerContext.start();
        } catch (LifecycleException e) {
            throw new StartException(MESSAGES.startEjbOverHttpServletFailed(webContext), e);
        }
    }

    private void prepareWebContainerContext(Host hostContainer) throws Exception {
        containerContext.init();
        containerContext.setPath(webContext);
        containerContext.addLifecycleListener(new ContextConfig());
        containerContext.setDocBase("");

        final Loader webCtxLoader = prepareWebContextLoader(hostContainer);
        containerContext.setLoader(webCtxLoader);

        Wrapper servletWrapper = prepareServletWrapper();
        containerContext.addChild(servletWrapper);

        containerContext.addServletMapping("/", SERVLET_NAME);

        hostContainer.addChild(containerContext);
        containerContext.create();
    }

    private Loader prepareWebContextLoader(Host hostContainer) {
        final Loader webCtxLoader = new WebCtxLoader(this.getClass().getClassLoader());
        webCtxLoader.setContainer(hostContainer);
        return webCtxLoader;
    }

    private Wrapper prepareServletWrapper() {
        final EJBRemoteConnectorService ejbRemoteConnectorService = this.ejbRemoteConnectorService.getValue();
        final HttpEJBClientMessageReceiver messageReceiver = new HttpEJBClientMessageReceiver(ejbRemoteConnectorService.getExecutorService().getValue(), ejbRemoteConnectorService.getDeploymentRepositoryInjector().getValue(),
                ejbRemoteConnectorService.getEJBRemoteTransactionsRepositoryInjector().getValue(), ejbRemoteConnectorService.getAsyncInvocationCancelStatusInjector().getValue(),
                ejbRemoteConnectorService.getSupportedMarshallingStrategies());
        final EjbOverHttpRemoteServlet httpEJBRemoteServlet = new EjbOverHttpRemoteServlet(messageReceiver);
        Wrapper servletWrapper = containerContext.createWrapper();
        servletWrapper.setName("ejb-over-http-servlet");
        servletWrapper.setServletClass(EjbOverHttpRemoteServlet.class.getName());
        servletWrapper.setServlet(httpEJBRemoteServlet);
        servletWrapper.setAsyncSupported(true);
        return servletWrapper;
    }

    @Override
    public synchronized void stop(StopContext context) {
        LOGGER.stoppingService(webContext);
        stopContainerAndRemoveFromHostSafely();
        destroyContainerSafely();
    }

    private void stopContainerAndRemoveFromHostSafely() {
        try {
            containerContext.stop();
            Host hostContainer = virtualHostInjector.getValue().getHost();
            hostContainer.removeChild(containerContext);
        } catch (LifecycleException e) {
            LOGGER.failedToStopCatalinaStandardContext(webContext, e);
        }
    }

    private void destroyContainerSafely() {
        try {
            containerContext.destroy();
        } catch (Exception e) {
            LOGGER.failedToDestroyCatalinaStandardContext(webContext, e);
        }
    }

    public InjectedValue<EJBRemoteConnectorService> getEjbRemoteConnectorService() {
        return ejbRemoteConnectorService;
    }

}
