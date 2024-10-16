/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.InvalidClassException;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * @author Paul Ferraro
 */
public class ModuleClassLoaderMarshaller implements ClassLoaderMarshaller {

    private static final int MODULE_INDEX = 0;
    private static final int FIELDS = 1;

    private final ModuleLoader loader;
    private final Module defaultModule;

    public ModuleClassLoaderMarshaller(Module defaultModule) {
        this.loader = defaultModule.getModuleLoader();
        this.defaultModule = defaultModule;
    }

    public ModuleClassLoaderMarshaller(ModuleLoader loader) {
        this.loader = loader;
        try {
            this.defaultModule = Module.getSystemModuleLoader().loadModule("java.base");
        } catch (ModuleLoadException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ClassLoader getBuilder() {
        return this.defaultModule.getClassLoader();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public ClassLoader readField(ProtoStreamReader reader, int index, ClassLoader loader) throws IOException {
        switch (index) {
            case MODULE_INDEX:
                String moduleName = reader.readAny(String.class);
                try {
                    Module module = this.loader.loadModule(moduleName);
                    return module.getClassLoader();
                } catch (ModuleLoadException e) {
                    InvalidClassException exception = new InvalidClassException(e.getMessage());
                    exception.initCause(e);
                    throw exception;
                }
            default:
                return loader;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, ClassLoader loader) throws IOException {
        Module module = Module.forClassLoader(loader, false);
        if (module != null && !this.defaultModule.equals(module)) {
            writer.writeAny(startIndex + MODULE_INDEX, module.getName());
        }
    }
}
