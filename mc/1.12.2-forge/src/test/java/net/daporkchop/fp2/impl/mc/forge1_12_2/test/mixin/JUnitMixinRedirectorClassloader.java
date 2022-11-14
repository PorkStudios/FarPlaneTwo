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
 *
 */

package net.daporkchop.fp2.impl.mc.forge1_12_2.test.mixin;

import com.google.common.collect.ImmutableList;
import lombok.SneakyThrows;
import net.daporkchop.fp2.core.util.threading.lazy.LazyFutureTask;
import net.daporkchop.lib.binary.oio.StreamUtil;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author DaPorkchop_
 */
public class JUnitMixinRedirectorClassloader extends ClassLoader {
    private static final List<String> BANNED_PACKAGES = ImmutableList.of(
            "java.", "javax.", "sun.",
            "com.oracle.", "com.sun.", "jdk.", "org.jcp.xml.", "org.xml.sax.", "org.omg.", "org.w3c.dom."
    );

    public static JUnitMixinRedirectorClassloader INSTANCE = new JUnitMixinRedirectorClassloader();

    private final Map<String, CompletableFuture<Class<?>>> cache = new ConcurrentHashMap<>();

    protected IMixinTransformerFactory transformerFactory;
    protected IMixinTransformer transformer;

    protected JUnitMixinRedirectorClassloader() {
        super(JUnitMixinRedirectorClassloader.class.getClassLoader());
        INSTANCE = this;

        MixinBootstrap.init();

        Mixins.addConfiguration("mixins.fp2.at.json");
        Mixins.addConfiguration("mixins.fp2.core.json");
        Mixins.addConfiguration("mixins.fp2.fixes.json");
    }

    @Override
    protected Class<?> loadClass(String nameIn, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = this.cache.computeIfAbsent(nameIn.intern(), name -> new LazyFutureTask<Class<?>>() {
            @Override
            @SneakyThrows
            protected Class<?> compute() {
                if (BANNED_PACKAGES.stream().anyMatch(name::startsWith)) {
                    return null;
                }

                try {
                    return JUnitMixinRedirectorClassloader.this.findClass(name);
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }
        }).join();

        if (clazz != null) {
            return clazz;
        }

        return super.loadClass(nameIn, resolve);
    }

    @Override
    @SneakyThrows(IOException.class)
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (this.transformer == null) {
            this.transformer = this.transformerFactory.createTransformer();
        }

        try (InputStream in = this.getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (in == null) {
                return super.findClass(name);
            }

            byte[] code = StreamUtil.toByteArray(in);
            byte[] transformed = this.transformer.transformClassBytes(name, name, code);
            return this.defineClass(name, transformed, 0, transformed.length);
        }
    }
}
