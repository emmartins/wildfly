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

package org.jboss.as.naming.service;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StopContext;

import java.util.concurrent.atomic.AtomicInteger;

import static org.jboss.as.naming.logging.NamingLogger.ROOT_LOGGER;

/**
 * A binder service which "ownership" may be shared.
 * Unless forced, the shared binder service only stops after all "owners" release it.
 *
 * @author Eduardo Martins
 */
public class SharedBinderService extends BinderService {

    /**
     * the context of a suspended stop
     */
    private volatile StopContext stopContext;



    /**
     * the bind's service name
     */
    private final ServiceName serviceName;

    /**
     *
     * @param name
     * @param source
     */
    public SharedBinderService(final String name, Object source, final ServiceName serviceName) {
        super(name, source);
        this.serviceName = serviceName;
    }

    /**
     * Retrieves the binds' service name.
     * @return
     */
    public ServiceName getServiceName() {
        return serviceName;
    }

    @Override
    public synchronized void stop(StopContext context) {
        ROOT_LOGGER.warnf("SharedBinderService %s stopping", serviceName);
        if (!isOwned()) {
            // no owners, proceed with the service stop
            super.stop(context);
        } else {
            ROOT_LOGGER.warnf("SharedBinderService %s stopping suspended", serviceName);
            // there are still owners, suspend the service stop
            stopContext = context;
            stopContext.asynchronous();
        }
    }

    @Override
    public synchronized void stopNow() {
        ROOT_LOGGER.warnf("SharedBinderService stopNow() %s ", serviceName);
        if (isOwned()) {
            ownersCount.set(0);
            noOwners();
        }
    }

    /**
     * The bind has no more owners, if the bind is up then the binder service is stopped.
     */
    private void noOwners() {
        ROOT_LOGGER.warnf("SharedBinderService noOwners() %s ", serviceName);
        if (stopContext != null) {
            // suspended stop, resume it
            ROOT_LOGGER.warnf("SharedBinderService %s stopping resume", serviceName);
            super.stop(stopContext);
            stopContext.complete();
            stopContext = null;
        } else {
            super.stopNow();
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("SharedBinderService[name=").append(name).append(", source=").append(source).append(", owners=").append(ownersCount.get()).append(']').toString();
    }

    public synchronized void acquire() {
        ownersCount.incrementAndGet();
        ROOT_LOGGER.warnf("SharedBinderService %s acquired", getServiceName());
    }

    public synchronized void release() {
        ROOT_LOGGER.warnf("SharedBinderService %s release", getServiceName());
        if (isOwned()) {
            ownersCount.decrementAndGet();
            if (!isOwned()) {
                noOwners();
            }
        }
    }

    private final AtomicInteger ownersCount = new AtomicInteger(0);

    private boolean isOwned() {
        return ownersCount.get() > 0;
    }

}
