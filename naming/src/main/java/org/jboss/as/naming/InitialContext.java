/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import org.jboss.as.naming.context.NamespaceContextSelector;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * @author Eduardo Martins
 * @author John Bailey
 */
public class InitialContext extends NamingContext {

    public InitialContext(Hashtable environment) throws NamingException {
        super(environment);
    }

    protected Context findContext(final Name name, final ParsedName parsedName) throws NamingException {
        if(parsedName.namespace() == null || parsedName.namespace().isEmpty()) {
            return null;
        }
        final NamespaceContextSelector selector = NamespaceContextSelector.getCurrentSelector();
        if (selector == null) {
            throw new NameNotFoundException(name.toString());
        }
        final Context namespaceContext = selector.getContext(parsedName.namespace());
        if (namespaceContext == null) {
            throw new NameNotFoundException(name.toString());
        }
        return namespaceContext;
    }

    private ParsedName parse(final Name name) throws NamingException {
        final Name remaining;
        final String namespace;
        if (name.isEmpty()) {
            namespace = null;
            remaining = name;
        } else {
            final String first = name.get(0);
            if (first.startsWith("java:")) {
                final String theRest = first.substring(5);
                if (theRest.startsWith("/")) {
                    namespace = "";
                    remaining = getNameParser(theRest).parse(theRest);
                } else if (theRest.equals("jboss") && name.size() > 1 && name.get(1).equals("exported")) {
                    namespace = "jboss/exported";
                    remaining = name.getSuffix(2);
                } else {
                    namespace = theRest;
                    remaining = name.getSuffix(1);
                }
            } else {
                namespace = null;
                remaining = name;
            }
        }

        return new ParsedName() {
            public String namespace() {
                return namespace;
            }

            public Name remaining() {
                return remaining;
            }
        };
    }

    public Object lookup(final Name name, boolean dereference) throws NamingException {
        final ParsedName parsedName = parse(name);
        final Context namespaceContext = findContext(name, parsedName);
        if (namespaceContext == null)
            return super.lookup(parsedName.remaining(), dereference);
        else
            return namespaceContext.lookup(parsedName.remaining());
    }

    public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
        final ParsedName parsedName = parse(name);
        final Context namespaceContext = findContext(name, parsedName);
        if (namespaceContext == null)
            return super.listBindings(parsedName.remaining());
        else
            return namespaceContext.listBindings(parsedName.remaining());
    }

    public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
        final ParsedName parsedName = parse(name);
        final Context namespaceContext = findContext(name, parsedName);
        if (namespaceContext == null)
            return super.list(parsedName.remaining());
        else
            return namespaceContext.list(parsedName.remaining());
    }

    public void bind(Name name, Object object) throws NamingException {
        final ParsedName parsedName = parse(name);
        final Context namespaceContext = findContext(name, parsedName);
        if (namespaceContext == null)
            super.bind(parsedName.remaining(), object);
        else
            namespaceContext.bind(parsedName.remaining(), object);
    }

    public void rebind(Name name, Object object) throws NamingException {
        final ParsedName parsedName = parse(name);
        final Context namespaceContext = findContext(name, parsedName);
        if (namespaceContext == null)
            super.rebind(parsedName.remaining(), object);
        else
            namespaceContext.rebind(parsedName.remaining(), object);
    }

    public void unbind(Name name) throws NamingException {
        final ParsedName parsedName = parse(name);
        final Context namespaceContext = findContext(name, parsedName);
        if (namespaceContext == null)
            super.unbind(parsedName.remaining());
        else
            namespaceContext.unbind(parsedName.remaining());
    }

    public void destroySubcontext(Name name) throws NamingException {
        final ParsedName parsedName = parse(name);
        final Context namespaceContext = findContext(name, parsedName);
        if (namespaceContext == null)
            super.destroySubcontext(parsedName.remaining());
        else
            namespaceContext.destroySubcontext(parsedName.remaining());
    }

    public Context createSubcontext(Name name) throws NamingException {
        final ParsedName parsedName = parse(name);
        final Context namespaceContext = findContext(name, parsedName);
        if (namespaceContext == null)
            return super.createSubcontext(parsedName.remaining());
        else
            return namespaceContext.createSubcontext(parsedName.remaining());
    }

    protected interface ParsedName {
        String namespace();
        Name remaining();
    }

}