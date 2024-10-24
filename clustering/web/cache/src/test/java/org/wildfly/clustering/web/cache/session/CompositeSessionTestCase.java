/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.session.Session;

/**
 * Unit test for {@link CompositeSession}.
 *
 * @author paul
 */
public class CompositeSessionTestCase {
    private final String id = "session";
    private final InvalidatableSessionMetaData metaData = mock(InvalidatableSessionMetaData.class);
    private final SessionAttributes attributes = mock(SessionAttributes.class);
    private final Remover<String> remover = mock(Remover.class);
    private final LocalContextFactory<Object> localContextFactory = mock(LocalContextFactory.class);
    private final AtomicReference<Object> localContextRef = new AtomicReference<>();

    private final Session<Object> session = new CompositeSession<>(this.id, this.metaData, this.attributes, this.localContextRef, this.localContextFactory, this.remover);

    @Test
    public void getId() {
        assertSame(this.id, this.session.getId());
    }

    @Test
    public void getAttributes() {
        assertSame(this.attributes, this.session.getAttributes());
    }

    @Test
    public void getMetaData() {
        assertSame(this.metaData, this.session.getMetaData());
    }

    @Test
    public void invalidate() {
        when(this.metaData.invalidate()).thenReturn(true);

        this.session.invalidate();

        verify(this.remover).remove(this.id);
        reset(this.remover);

        when(this.metaData.invalidate()).thenReturn(false);

        this.session.invalidate();

        verify(this.remover, never()).remove(this.id);
    }

    @Test
    public void isValid() {
        when(this.metaData.isValid()).thenReturn(true);

        assertTrue(this.session.isValid());

        when(this.metaData.isValid()).thenReturn(false);

        assertFalse(this.session.isValid());
    }

    @Test
    public void close() {
        when(this.metaData.isValid()).thenReturn(true);

        this.session.close();

        verify(this.attributes).close();
        verify(this.metaData).close();

        reset(this.metaData, this.attributes);

        // Verify that session is not mutated if invalid
        when(this.metaData.isValid()).thenReturn(false);

        this.session.close();

        verify(this.attributes, never()).close();
        verify(this.metaData, never()).close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getLocalContext() {
        Object expected = new Object();
        when(this.localContextFactory.createLocalContext()).thenReturn(expected);

        Object result = this.session.getLocalContext();

        assertSame(expected, result);

        reset(this.localContextFactory);

        result = this.session.getLocalContext();

        verifyNoInteractions(this.localContextFactory);

        assertSame(expected, result);
    }
}
