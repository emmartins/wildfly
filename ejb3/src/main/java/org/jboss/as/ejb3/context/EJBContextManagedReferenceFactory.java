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

package org.jboss.as.ejb3.context;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import javax.ejb.EJBContext;

/**
 * @author Eduardo Martins
 */
public class EJBContextManagedReferenceFactory implements ContextListManagedReferenceFactory {

    public static final EJBContextManagedReferenceFactory INSTANCE = new EJBContextManagedReferenceFactory();

    private static final ManagedReference managedReference = new ManagedReference() {
        @Override
        public void release() {

        }
        @Override
        public Object getInstance() {
            return CurrentInvocationContext.getEjbContext();
        }
    };

    private EJBContextManagedReferenceFactory() {

    }

    @Override
    public ManagedReference getReference() {
        return managedReference;
    }

    @Override
    public String getInstanceClassName() {
        return EJBContext.class.getName();
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
