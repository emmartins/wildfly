/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.function.Supplier;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.group.Group;

/**
 * @author Paul Ferraro
 */
public interface InfinispanTimerManagerConfiguration<I, V> extends InfinispanConfiguration {

    TimerFactory<I, V> getTimerFactory();
    TimerRegistry<I> getRegistry();
    Marshaller<Object, V> getMarshaller();
    Supplier<I> getIdentifierFactory();
    KeyAffinityServiceFactory getKeyAffinityServiceFactory();
    CommandDispatcherFactory getCommandDispatcherFactory();
    Group<Address> getGroup();
}
