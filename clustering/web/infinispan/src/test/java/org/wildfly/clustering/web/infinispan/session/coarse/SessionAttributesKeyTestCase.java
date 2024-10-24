/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.coarse;

import java.io.IOException;

import org.junit.Test;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;

/**
 * Unit test for {@link SessionAttributesKey}.
 * @author Paul Ferraro
 */
public class SessionAttributesKeyTestCase {

    @Test
    public void test() throws IOException {
        SessionAttributesKey key = new SessionAttributesKey("ABC123");
        ProtoStreamTesterFactory.INSTANCE.createTester().test(key);
        new FormatterTester<>(new SessionAttributesKeyFormatter()).test(key);
    }
}
