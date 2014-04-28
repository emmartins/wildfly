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

package org.jboss.as.ejb3.timerservice;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Jaikiran Pai
 * @author Eduardo Martins
 */
public class TimerServiceManagedReferenceFactory implements ContextListManagedReferenceFactory {

    public static final TimerServiceManagedReferenceFactory INSTANCE = new TimerServiceManagedReferenceFactory();

    private final TimerServiceManagedReference managedReference = new TimerServiceManagedReference();

    private TimerServiceManagedReferenceFactory() {

    }

    @Override
    public ManagedReference getReference() {
        return managedReference;
    }

    @Override
    public String getInstanceClassName() {
        return javax.ejb.TimerService.class.getName();
    }

    /**
     * A {@link ManagedReference} to a {@link javax.ejb.TimerService}
     */
    private static class TimerServiceManagedReference implements ManagedReference {

        @Override
        public void release() {

        }

        @Override
        public Object getInstance() {
            // get the current invocation context and the EJBComponent out of it
            final InterceptorContext currentInvocationContext = CurrentInvocationContext.get();
            final EJBComponent ejbComponent = (EJBComponent) currentInvocationContext.getPrivateData(Component.class);
            if (ejbComponent == null) {
                throw EjbLogger.ROOT_LOGGER.failToGetEjbComponent(currentInvocationContext);
            }
            return ejbComponent.getTimerService();
        }
    }

    /**
     *
     * @param serviceTarget
     * @param verificationHandler
     * @return
     */
    public static ServiceController<?> bind(String jndiName, ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        return ContextNames.bindInfoFor(jndiName).builder(serviceTarget, verificationHandler).installService(INSTANCE, INSTANCE);
    }
}
