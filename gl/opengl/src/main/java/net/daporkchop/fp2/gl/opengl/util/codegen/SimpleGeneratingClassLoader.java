/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.util.codegen;

import lombok.Getter;
import lombok.NonNull;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author DaPorkchop_
 */
public abstract class SimpleGeneratingClassLoader extends GeneratingClassLoader {
    @Getter(lazy = true)
    private final Map<String, Supplier<byte[]>> classGenerators = this.buildClassGenerators();

    private Map<String, Supplier<byte[]>> buildClassGenerators() {
        Map<String, Supplier<byte[]>> map = new TreeMap<>();
        this.registerClassGenerators((name, generator) -> map.put(name.intern(), generator));
        return map;
    }

    protected abstract void registerClassGenerators(@NonNull BiConsumer<String, Supplier<byte[]>> register);

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Supplier<byte[]> generator = this.classGenerators().remove(name);
        if (generator != null) {
            return this.defineClass(name, generator.get());
        }

        throw new ClassNotFoundException(name);
    }
}
