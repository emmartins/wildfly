package org.jboss.as.test.integration.ejb.remote.http;

public interface CounterRemote {

    int addAndGet(int delta);

}
