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

package net.daporkchop.fp2.gl.opengl.command.state;

import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.command.CodegenArgs;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.MethodWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * A {@link StateProperty} with an associated value.
 * <p>
 * Property values are always {@link Optional}. An empty {@link Optional} indicates that we don't care what the property is set to.
 *
 * @author DaPorkchop_
 */
public interface StateValueProperty<T> extends StateProperty {
    @Override
    default Stream<StateValueProperty<?>> depends(@NonNull State state) {
        return Stream.of(this);
    }

    /**
     * @return the property's default value
     */
    T def();

    /**
     * Generates JVM bytecode for setting the property to the given value.
     *
     * @param value  the value
     * @param writer the {@link MethodWriter} to which code should be written
     */
    void set(@NonNull T value, @NonNull MethodWriter<CodegenArgs> writer);

    void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator);

    void restore(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, int lvtIndexBase);

    /**
     * Checks whether or not this property's value can be backed up to and restored from the legacy OpenGL attribute stack using {@link GLAPI#glPushAttrib(int)}/{@link GLAPI#glPushClientAttrib(int)}
     * and {@link GLAPI#glPopAttrib()}/{@link GLAPI#glPopClientAttrib()}. This is used as an optimization to avoid having to manually back up and restore property values to the Java stack using
     * {@link #backup(MethodVisitor, int, int, AtomicInteger)} and {@link #restore(MethodVisitor, int, int, int)} when the legacy attribute stack is available.
     *
     * @return whether or not this property's value can be backed up to and restored from the legacy OpenGL attribute stack
     */
    default boolean canBackupRestoreToLegacyAttributeStack() {
        return false;
    }
}
