/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.Duration;

import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link Duration} instances, using the following strategy:
 * <ol>
 * <li>Marshal {@link Duration#ZERO} as zero bytes</li>
 * <li>Marshal number of seconds of duration as unsigned long</li>
 * <li>Marshal sub-second value of duration as unsigned integer, using millisecond precision, if possible</li>
 * </ol>
 * @author Paul Ferraro
 */
public enum DurationMarshaller implements FieldSetMarshaller<Duration, Duration> {
    INSTANCE;

    private static final int POSITIVE_SECONDS_INDEX = 0;
    private static final int NEGATIVE_SECONDS_INDEX = 1;
    private static final int MILLIS_INDEX = 2;
    private static final int NANOS_INDEX = 3;
    private static final int FIELDS = 4;

    @Override
    public Duration getBuilder() {
        return Duration.ZERO;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public Duration readField(ProtoStreamReader reader, int index, Duration duration) throws IOException {
        switch (index) {
            case POSITIVE_SECONDS_INDEX:
                return duration.withSeconds(reader.readUInt64());
            case NEGATIVE_SECONDS_INDEX:
                return duration.withSeconds(0 - reader.readUInt64());
            case MILLIS_INDEX:
                return duration.withNanos(reader.readUInt32() * 1_000_000);
            case NANOS_INDEX:
                return duration.withNanos(reader.readUInt32());
            default:
                return duration;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, Duration duration) throws IOException {
        long seconds = duration.getSeconds();
        // Optimize for positive values
        if (seconds > 0) {
            writer.writeUInt64(startIndex + POSITIVE_SECONDS_INDEX, seconds);
        } else if (seconds < 0) {
            writer.writeUInt64(startIndex + NEGATIVE_SECONDS_INDEX, 0 - seconds);
        }
        int nanos = duration.getNano();
        if (nanos > 0) {
            // Optimize for ms precision, if possible
            if (nanos % 1_000_000 == 0) {
                writer.writeUInt32(startIndex + MILLIS_INDEX, nanos / 1_000_000);
            } else {
                writer.writeUInt32(startIndex + NANOS_INDEX, nanos);
            }
        }
    }
}
