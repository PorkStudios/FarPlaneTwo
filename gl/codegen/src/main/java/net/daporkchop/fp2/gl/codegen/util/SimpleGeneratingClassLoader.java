/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.fp2.gl.codegen.util;

import lombok.Getter;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author DaPorkchop_
 */
public abstract class SimpleGeneratingClassLoader extends GeneratingClassLoader {
    @Getter(lazy = true)
    private final Map<String, Object> entries = this.buildEntries();

    private Map<String, Object> buildEntries() {
        Map<String, Object> map = new TreeMap<>();
        this.registerClassGenerators(
                (name, generator) -> map.put(name.intern(), generator),
                clazz -> {
                    //check to make sure that the class doesn't come from one of this classloader's parents, because if it does there's no need to load it ourselves
                    for (ClassLoader parent = this.getParent(); parent != null; parent = parent.getParent()) {
                        if (clazz.getClassLoader() == parent) {
                            return;
                        }
                    }

                    map.put(clazz.getName(), clazz);
                });
        return map;
    }

    protected abstract void registerClassGenerators(BiConsumer<String, Supplier<byte[]>> registerGenerator, Consumer<Class<?>> registerClass);

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Object value = this.entries().remove(name);
        if (value != null) {
            if (value instanceof Class) {
                return (Class<?>) value;
            } else if (value instanceof Supplier) {
                return this.defineClass(name, PorkUtil.<Supplier<byte[]>>uncheckedCast(value).get());
            } else {
                throw new IllegalArgumentException(PorkUtil.className(value));
            }
        }

        throw new ClassNotFoundException(name);
    }
}
