/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public enum TimerIndexMarshaller implements ProtoStreamMarshaller<TimerIndex> {
    INSTANCE;

    private static final int NO_PARAMETERS_DECLARING_CLASS_NAME_INDEX = 1;
    private static final int TIMER_PARAMETERS_DECLARING_CLASS_NAME_INDEX = 2;
    private static final int METHOD_NAME_INDEX = 3;
    private static final int INDEX_INDEX = 4;

    private static final String DEFAULT_METHOD_NAME = "ejbTimeout";
    private static final int DEFAULT_INDEX = 0;

    @Override
    public Class<? extends TimerIndex> getJavaClass() {
        return TimerIndex.class;
    }

    @Override
    public TimerIndex readFrom(ProtoStreamReader reader) throws IOException {
        String declaringClassName = null;
        String methodName = DEFAULT_METHOD_NAME;
        int parameters = 0;
        int index = DEFAULT_INDEX;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case TIMER_PARAMETERS_DECLARING_CLASS_NAME_INDEX:
                    parameters = 1;
                case NO_PARAMETERS_DECLARING_CLASS_NAME_INDEX:
                    declaringClassName = reader.readString();
                    break;
                case METHOD_NAME_INDEX:
                    methodName = reader.readString();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new TimerIndex(declaringClassName, methodName, parameters, index);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, TimerIndex index) throws IOException {
        writer.writeString(index.getParameters() > 0 ? TIMER_PARAMETERS_DECLARING_CLASS_NAME_INDEX : NO_PARAMETERS_DECLARING_CLASS_NAME_INDEX, index.getDeclaringClassName());
        String methodName = index.getMethodName();
        if (!methodName.equals(DEFAULT_METHOD_NAME)) {
            writer.writeString(METHOD_NAME_INDEX, methodName);
        }
        if (index.getIndex() != DEFAULT_INDEX) {
            writer.writeUInt32(INDEX_INDEX, index.getIndex());
        }
    }
}
