/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.deployment;

import org.jboss.as.naming.InitialContext;
import org.jboss.as.naming.logging.NamingLogger;
import java.io.Serializable;

/**
 * Utility object used to easily manged the construction and management of JNDI names.
 *
 * @author Eduardo Martins
 */
public class JndiName implements Serializable, Comparable<JndiName> {

    private final String name;
    private final String scheme;

    private boolean java;
    private boolean javaComp;
    private boolean javaModule;
    private boolean javaApp;
    private boolean javaGlobal;
    private boolean javaJBoss;
    private boolean javaJBossExported;

    /**
     *
     * @param name
     */
    public JndiName(String name) {
        if (name == null) {
            throw NamingLogger.ROOT_LOGGER.nullVar("name");
        }
        final String scheme = InitialContext.getURLScheme(name);
        if (scheme == null) {
            this.name = "java:comp/env/" + name;
            this.scheme = "java";
            javaComp();
        } else {
            this.name = name;
            this.scheme = scheme;
            if ("java".equals(scheme)) {
                final String nameWithoutScheme = name.substring(5);
                if (nameWithoutScheme.startsWith("comp/")) {
                    javaComp();
                } else if (nameWithoutScheme.startsWith("module/")) {
                    javaModule();
                } else if (nameWithoutScheme.startsWith("app/")) {
                    javaApp();
                } else if (nameWithoutScheme.startsWith("global/")) {
                    javaGlobal();
                } else if (nameWithoutScheme.startsWith("jboss/")) {
                    if (nameWithoutScheme.startsWith( "exported/", "jboss/".length())) {
                        javaJBossExported();
                    } else {
                        javaJBoss();
                    }
                } else {
                    java();
                }
            }
        }
    }

    /**
     *
     * @return
     */
    public String getScheme() {
        return scheme;
    }

    /**
     *
     * @return
     */
    public String getAbsoluteName() {
        return name;
    }

    /**
     *
     * @return
     */
    public boolean isJava() {
        return java;
    }

    /**
     *
     * @return
     */
    public boolean isJavaComp() {
        return javaComp;
    }

    /**
     *
     * @return
     */
    public boolean isJavaModule() {
        return javaModule;
    }

    /**
     *
     * @return
     */
    public boolean isJavaApp() {
        return javaApp;
    }

    /**
     *
     * @return
     */
    public boolean isJavaGlobal() {
        return javaGlobal;
    }

    /**
     *
     * @return
     */
    public boolean isJavaJBoss() {
        return javaJBoss;
    }

    /**
     *
     * @return
     */
    public boolean isJavaJBossExported() {
        return javaJBossExported;
    }

    private void javaComp() {
        this.javaComp = true;
        java();
    }

    private void javaModule() {
        this.javaModule = true;
        java();
    }

    private void javaApp() {
        this.javaApp = true;
        java();
    }

    private void javaGlobal() {
        this.javaGlobal = true;
        java();
    }

    private void javaJBoss() {
        this.javaJBoss = true;
        java();
    }

    private void javaJBossExported() {
        this.javaJBossExported = true;
        javaJBoss();
    }

    private void java() {
        this.java = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JndiName name1 = (JndiName) o;

        if (!name.equals(name1.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(JndiName other) {
        return this.name.compareTo(other.name);
    }

    @Override
    public String toString() {
        return getAbsoluteName();
    }

}
