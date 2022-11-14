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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * @author DaPorkchop_
 */
public class JUnitMixinRedirectorService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {
    //
    // MixinServiceAbstract

    @Override
    public String getName() {
        return "JUnit Redirect";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.DEFAULT;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return null; //unsupported
    }

    @Override
    public IClassTracker getClassTracker() {
        return null; //unsupported
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null; //unsupported
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.emptyList();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return new ContainerHandleVirtual(this.getName());
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return JUnitMixinRedirectorClassloader.INSTANCE.getResourceAsStream(name);
    }

    @Override
    public void offer(IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory) {
            JUnitMixinRedirectorClassloader.INSTANCE.transformerFactory = (IMixinTransformerFactory) internal;
        }
        super.offer(internal);
    }

    //
    // IClassProvider
    //

    @Override
    public URL[] getClassPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return JUnitMixinRedirectorClassloader.INSTANCE.findClass(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, JUnitMixinRedirectorClassloader.INSTANCE);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, JUnitMixinRedirectorClassloader.class.getClassLoader());
    }

    //
    // IClassBytecodeProvider
    //

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        assert runTransformers;

        ClassNode node = new ClassNode();
        try (InputStream in = JUnitMixinRedirectorClassloader.class.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
            assert in != null : name.replace('.', '/') + ".class";

            ClassReader reader = new ClassReader(in);
            reader.accept(node, ClassReader.EXPAND_FRAMES);
        }

        return node;
    }
}
