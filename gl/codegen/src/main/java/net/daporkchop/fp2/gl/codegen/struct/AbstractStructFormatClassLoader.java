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

package net.daporkchop.fp2.gl.codegen.struct;

import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.attribute.NewUniformBuffer;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeIgnore;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.codegen.struct.layout.LayoutInfo;
import net.daporkchop.fp2.gl.codegen.util.SimpleGeneratingClassLoader;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractStructFormatClassLoader<STRUCT extends AttributeStruct> extends SimpleGeneratingClassLoader {
    protected final EnumSet<AttributeTarget> validTargets;
    protected final Class<STRUCT> structClass;
    protected final LayoutInfo layoutInfo;

    protected final String attributeWriterClassInternalName;
    protected final String handleClassInternalName;
    protected final String handleUniformClassInternalName;
    protected final String uniformBufferClassInternalName;

    public AbstractStructFormatClassLoader(String prefix, EnumSet<AttributeTarget> validTargets, Class<STRUCT> structClass, LayoutInfo layoutInfo) {
        this.validTargets = validTargets;
        this.structClass = structClass;
        this.layoutInfo = layoutInfo;

        String suffix = "layout='" + layoutInfo.name() + "',struct=" + structClass.getName().replace('.', '_');

        this.attributeWriterClassInternalName = (prefix + AttributeWriter.class.getSimpleName() + "Impl_" + suffix).replace('.', '/');
        this.handleClassInternalName = (prefix + AttributeStruct.class.getSimpleName() + "Impl_" + suffix).replace('.', '/');
        this.handleUniformClassInternalName = (prefix + AttributeStruct.class.getSimpleName() + "UniformUpdateImpl_" + suffix).replace('.', '/');
        this.uniformBufferClassInternalName = (prefix + NewUniformBuffer.class.getSimpleName() + "Impl_" + suffix).replace('.', '/');
    }

    protected abstract byte[] attributeWriterClass();

    protected abstract byte[] handleClass();

    protected abstract byte[] handleUniformClass();

    protected abstract byte[] uniformBufferClass();

    @Override
    protected void registerClassGenerators(BiConsumer<String, Supplier<byte[]>> registerGenerator, Consumer<Class<?>> registerClass) {
        registerGenerator.accept(this.attributeWriterClassInternalName.replace('/', '.'), this::attributeWriterClass);
        registerGenerator.accept(this.handleClassInternalName.replace('/', '.'), this::handleClass);
        registerGenerator.accept(this.handleUniformClassInternalName.replace('/', '.'), this::handleUniformClass);
        registerGenerator.accept(this.uniformBufferClassInternalName.replace('/', '.'), this::uniformBufferClass);
    }

    protected final void forEachSetterMethod(BiConsumer<Method, String> action) {
        for (Method method : this.structClass.getMethods()) {
            checkArg((method.getModifiers() & Modifier.STATIC) == 0, "method %s may not be static!", method);

            if (method.isAnnotationPresent(AttributeSetter.class)) {
                AttributeSetter annotation = method.getAnnotation(AttributeSetter.class);
                String attributeName = annotation.value();
                if (attributeName.isEmpty()) { //fall back to method name if attribute name is unset
                    attributeName = method.getName();
                }

                action.accept(method, attributeName);
            } else if (!method.isAnnotationPresent(AttributeIgnore.class)) {
                throw new IllegalArgumentException(method.toString());
            }
        }
    }
}
