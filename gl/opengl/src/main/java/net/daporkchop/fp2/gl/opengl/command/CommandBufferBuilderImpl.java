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

package net.daporkchop.fp2.gl.opengl.command;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.fp2.gl.command.CommandBuffer;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLExtension;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.MethodWriter;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.PassthroughMethodWriter;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.TreeMethodWriter;
import net.daporkchop.fp2.gl.opengl.command.state.CowState;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.command.uop.Uop;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public class CommandBufferBuilderImpl extends AbstractCommandBufferBuilder {
    protected static final CowState INITIAL_STATE = new CowState()
            .set(StateProperties.RASTERIZER_DISCARD, false);

    protected final ClassWriter writer;
    protected final MethodVisitor ctorVisitor;
    protected final String apiFieldName;

    protected final List<Object> fieldValues = new ArrayList<>();

    public CommandBufferBuilderImpl(@NonNull OpenGL gl) {
        super(gl, INITIAL_STATE);

        this.writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        this.writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, CLASS_NAME, null, getInternalName(CommandBufferImpl.class), null);

        this.ctorVisitor = this.writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(List.class), getType(List.class)), null, null);
        this.ctorVisitor.visitCode();
        this.ctorVisitor.visitVarInsn(ALOAD, 0);
        this.ctorVisitor.visitVarInsn(ALOAD, 1);
        this.ctorVisitor.visitMethodInsn(INVOKESPECIAL, getInternalName(CommandBufferImpl.class), "<init>", getMethodDescriptor(VOID_TYPE, getType(List.class)), false);

        this.apiFieldName = this.makeField(getType(GLAPI.class), gl.api());
    }

    @Override
    protected String makeField(@NonNull Type type, @NonNull Object value) {
        //assign a new field name
        int index = this.fieldValues.size();
        this.fieldValues.add(value);
        String name = PStrings.fastFormat("field_%04x", index);

        //define the field
        this.writer.visitField(ACC_PRIVATE | ACC_FINAL, name, type.getDescriptor(), null, null).visitEnd();

        //set the field value in the constructor
        this.ctorVisitor.visitVarInsn(ALOAD, 0); //this
        this.ctorVisitor.visitVarInsn(ALOAD, 2); //list
        this.ctorVisitor.visitLdcInsn(index); //index
        this.ctorVisitor.visitMethodInsn(INVOKEINTERFACE, getInternalName(List.class), "get", getMethodDescriptor(getType(Object.class), INT_TYPE), true); //list.get(index)
        this.ctorVisitor.visitTypeInsn(CHECKCAST, type.getInternalName()); //(type)
        this.ctorVisitor.visitFieldInsn(PUTFIELD, CLASS_NAME, name, type.getDescriptor()); //this.name = (type) list.get(index);

        return name;
    }

    @Override
    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class, InvocationTargetException.class, NoSuchMethodException.class })
    public CommandBuffer build() {
        MethodVisitor entryVisitor = this.writer.visitMethod(ACC_PUBLIC | ACC_FINAL, "execute", getMethodDescriptor(VOID_TYPE), null, null);
        entryVisitor.visitCode();

        entryVisitor.visitVarInsn(ALOAD, 0);
        entryVisitor.visitFieldInsn(GETFIELD, CLASS_NAME, this.apiFieldName, getDescriptor(GLAPI.class));
        entryVisitor.visitVarInsn(ASTORE, 1);

        TreeMethodWriter<CodegenArgs> methodWriter = new TreeMethodWriter<>(this.writer, CLASS_NAME, "execute", new CodegenArgs(1), 4, true);

        List<Uop> uops = ImmutableList.copyOf(this.uops);
        MutableState state = new MutableState();
        for (Uop uop : uops) {
            uop.depends().distinct().forEach(property -> {
                uop.state().get(property).ifPresent(value -> {
                    if (!Objects.equals(value, state.get(property).orElse(null))) {
                        property.set(uncheckedCast(value), methodWriter);
                        state.set(property, uncheckedCast(value));
                    }
                });
            });

            uop.emitCode(state.immutableSnapshot(), this, methodWriter);
        }

        String executeMethodName = methodWriter.finish();

        if (this.gl.preserveInputGlState()) { //we want to preserve the current OpenGL state
            AtomicInteger lvtIndexAllocator = new AtomicInteger(2);
            Queue<Integer> baseLvts = new ArrayDeque<>();

            //allocate buffer for temporary data to/from glGet()s
            String bufferFieldName = this.makeField(getType(ByteBuffer.class), ByteBuffer.allocateDirect(16 * DOUBLE_SIZE));
            entryVisitor.visitVarInsn(ALOAD, 0);
            entryVisitor.visitFieldInsn(GETFIELD, CLASS_NAME, bufferFieldName, getDescriptor(ByteBuffer.class));
            entryVisitor.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "pork_directBufferAddress", getMethodDescriptor(LONG_TYPE, getType(Buffer.class)), false);
            int bufferLvtIndex = lvtIndexAllocator.getAndAdd(2);
            entryVisitor.visitVarInsn(LSTORE, bufferLvtIndex);

            //back up all affected OpenGL properties
            if (GLExtension.GL_ARB_compatibility.supported(this.gl)) { //back up old attribute to the legacy attribute stack, if possible
                //api.glPushClientAttrib(GL_ALL_CLIENT_ATTRIB_BITS);
                entryVisitor.visitVarInsn(ALOAD, 1);
                entryVisitor.visitLdcInsn(OpenGLConstants.GL_ALL_CLIENT_ATTRIB_BITS);
                entryVisitor.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glPushClientAttrib", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);

                //api.glPushAttrib(GL_ALL_ATTRIB_BITS);
                entryVisitor.visitVarInsn(ALOAD, 1);
                entryVisitor.visitLdcInsn(OpenGLConstants.GL_ALL_ATTRIB_BITS);
                entryVisitor.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glPushAttrib", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
            }
            state.properties() //back up remaining attributes to the regular java stack
                    .filter(property -> !GLExtension.GL_ARB_compatibility.supported(this.gl) || !property.canBackupRestoreToLegacyAttributeStack())
                    .forEach(property -> {
                        baseLvts.add(lvtIndexAllocator.get());
                        property.backup(entryVisitor, 1, bufferLvtIndex, lvtIndexAllocator);
                    });

            //invoke execute(GLAPI)
            entryVisitor.visitVarInsn(ALOAD, 0);
            entryVisitor.visitVarInsn(ALOAD, 1);
            entryVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME, executeMethodName, getMethodDescriptor(VOID_TYPE, getType(GLAPI.class)), false);

            //restore all affected OpenGL properties from their saved values
            if (GLExtension.GL_ARB_compatibility.supported(this.gl)) { //restore old attributes from the legacy attribute stack, if possible
                //api.glPopClientAttrib();
                entryVisitor.visitVarInsn(ALOAD, 1);
                entryVisitor.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glPopClientAttrib", getMethodDescriptor(VOID_TYPE), true);

                //api.glPopAttrib();
                entryVisitor.visitVarInsn(ALOAD, 1);
                entryVisitor.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glPopAttrib", getMethodDescriptor(VOID_TYPE), true);
            }
            state.properties()
                    .filter(property -> !GLExtension.GL_ARB_compatibility.supported(this.gl) || !property.canBackupRestoreToLegacyAttributeStack())
                    .forEach(property -> property.restore(entryVisitor, 1, bufferLvtIndex, baseLvts.poll()));
        } else {
            //invoke execute(GLAPI)
            entryVisitor.visitVarInsn(ALOAD, 0);
            entryVisitor.visitVarInsn(ALOAD, 1);
            entryVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME, executeMethodName, getMethodDescriptor(VOID_TYPE, getType(GLAPI.class)), false);

            //reset all properties to their default values
            MethodWriter<CodegenArgs> passthroughWriter = new PassthroughMethodWriter<>(entryVisitor, new CodegenArgs(1));
            state.forEach((property, value) -> {
                if (!Objects.equals(value, property.def())) {
                    property.set(uncheckedCast(property.def()), passthroughWriter);
                }
            });
        }

        this.ctorVisitor.visitInsn(RETURN);
        this.ctorVisitor.visitMaxs(0, 0);
        this.ctorVisitor.visitEnd();

        entryVisitor.visitInsn(RETURN);
        entryVisitor.visitMaxs(0, 0);
        entryVisitor.visitEnd();

        this.writer.visitEnd();

        if (OpenGL.DEBUG) {
            try {
                Files.write(Paths.get(CLASS_NAME.substring(CLASS_NAME.lastIndexOf('/') + 1) + ".class"), this.writer.toByteArray());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Class<? extends CommandBuffer> clazz = uncheckedCast(ClassloadingUtils.defineHiddenClass(AbstractCommandBufferBuilder.class.getClassLoader(), this.writer.toByteArray()));
        return clazz.getDeclaredConstructor(List.class, List.class).newInstance(uops, this.fieldValues);
    }
}
