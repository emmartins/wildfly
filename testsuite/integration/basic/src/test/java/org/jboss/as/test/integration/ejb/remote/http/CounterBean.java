package org.jboss.as.test.integration.ejb.remote.http;

import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.Remote;
import javax.ejb.Stateful;

@Stateful
@Remote(CounterRemote.class)
public class CounterBean implements CounterRemote {

    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public int addAndGet(int delta) {
        return counter.addAndGet(delta);
    }


}
