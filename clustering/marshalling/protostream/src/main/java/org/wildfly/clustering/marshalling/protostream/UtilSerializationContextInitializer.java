/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.util.EnumSet;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.MarshallerProvider.ClassPredicate;
import org.wildfly.clustering.marshalling.protostream.util.UtilMarshallerProvider;
import org.wildfly.clustering.marshalling.spi.util.UtilExternalizerProvider;

/**
 * Initializer that registers protobuf schema for java.util.* classes.
 * @author Paul Ferraro
 */
public class UtilSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public UtilSerializationContextInitializer() {
        super("java.util.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshallerProvider(new MarshallerProvider(ClassPredicate.ABSTRACT, EnumSet.allOf(UtilMarshallerProvider.class)));
        // These might be implemented as enums
        context.registerMarshallerProvider(new ExternalizerMarshallerProvider(UtilExternalizerProvider.NATURAL_ORDER_COMPARATOR));
        context.registerMarshallerProvider(new ExternalizerMarshallerProvider(UtilExternalizerProvider.REVERSE_ORDER_COMPARATOR));
    }
}
