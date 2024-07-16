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

package net.daporkchop.fp2.gl.codegen.draw.index;

import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.codegen.struct.attribute.JavaPrimitiveType;
import net.daporkchop.fp2.gl.codegen.util.SimpleGeneratingClassLoader;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public final class IndexFormatClassLoader extends SimpleGeneratingClassLoader {
    private final IndexType type;
    private final JavaPrimitiveType primitiveType;

    private final String formatClassInternalName;
    private final String writerClassInternalName;

    private static JavaPrimitiveType primitiveType(IndexType type) {
        switch (type) {
            case UNSIGNED_BYTE:
                return JavaPrimitiveType.UNSIGNED_BYTE;
            case UNSIGNED_SHORT:
                return JavaPrimitiveType.UNSIGNED_SHORT;
            case UNSIGNED_INT:
                return JavaPrimitiveType.UNSIGNED_INT;
            default:
                throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    public IndexFormatClassLoader(IndexType type) {
        super(null); //inherit from the bootstrap class loader, this will force us to explicitly register a dependency on every class which could be referenced
        this.type = type;
        this.primitiveType = primitiveType(type);

        this.formatClassInternalName = (IndexFormat.class.getSimpleName() + "Impl$" + type).replace('.', '/');
        this.writerClassInternalName = (IndexWriter.class.getSimpleName() + "Impl$" + type).replace('.', '/');
    }

    public IndexFormat createIndexFormat() {
        return (IndexFormat) MethodHandles.publicLookup().findConstructor(
                        this.loadClass(this.formatClassInternalName.replace('/', '.')),
                        MethodType.methodType(void.class, IndexType.class))
                .invoke(this.type);
    }

    @Override
    protected void registerClassGenerators(BiConsumer<String, Supplier<byte[]>> registerGenerator, Consumer<Class<?>> registerClass) {
        registerGenerator.accept(this.formatClassInternalName.replace('/', '.'), this::formatClass);
        registerGenerator.accept(this.writerClassInternalName.replace('/', '.'), this::writerClass);

        registerClass.accept(PUnsafe.class);
        registerClass.accept(DirectMemoryAllocator.class);

        registerClass.accept(IndexType.class);
        registerClass.accept(IndexFormat.class);
        registerClass.accept(IndexWriter.class);
        registerClass.accept(AbstractIndexFormat.class);
        registerClass.accept(AbstractIndexWriter.class);
    }

    private byte[] formatClass() {
        return generateClass(ACC_PUBLIC | ACC_FINAL, this.formatClassInternalName, getInternalName(AbstractIndexFormat.class), null, cv -> {
            generatePassthroughCtor(cv, getInternalName(AbstractIndexFormat.class), getType(IndexType.class));

            //NewIndexWriter createWriter(DirectMemoryAllocator alloc)
            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "createWriter", getMethodDescriptor(getType(IndexWriter.class), getType(DirectMemoryAllocator.class)), mv -> {
                mv.visitTypeInsn(NEW, this.writerClassInternalName);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, this.writerClassInternalName, "<init>", getMethodDescriptor(VOID_TYPE, getType(IndexFormat.class), getType(DirectMemoryAllocator.class)), false);
                return ARETURN;
            });
        });
    }

    private byte[] writerClass() {
        return generateClass(ACC_PUBLIC | ACC_FINAL, this.writerClassInternalName, getInternalName(AbstractIndexWriter.class), null, cv -> {
            generatePassthroughCtor(cv, getInternalName(AbstractIndexWriter.class), getType(IndexFormat.class), getType(DirectMemoryAllocator.class));

            //void set(long address, long index, int value)
            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "set", getMethodDescriptor(VOID_TYPE, LONG_TYPE, LONG_TYPE, INT_TYPE), mv -> {
                mv.visitInsn(ACONST_NULL);
                this.generateComputeAddress(mv, 1, 3);
                mv.visitVarInsn(ILOAD, 5);
                this.primitiveType.unsafePut(mv);
                return RETURN;
            });

            //int get(long address, long index)
            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "get", getMethodDescriptor(INT_TYPE, LONG_TYPE, LONG_TYPE), mv -> {
                mv.visitInsn(ACONST_NULL);
                this.generateComputeAddress(mv, 1, 3);
                this.primitiveType.unsafeGet(mv);
                return IRETURN;
            });

            //void copySingle(long address, long src, long dst)
            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "copySingle", getMethodDescriptor(VOID_TYPE, LONG_TYPE, LONG_TYPE, LONG_TYPE), mv -> {
                mv.visitInsn(ACONST_NULL);
                this.generateComputeAddress(mv, 1, 3);
                mv.visitInsn(ACONST_NULL);
                this.generateComputeAddress(mv, 1, 5);
                this.primitiveType.unsafeGet(mv);
                this.primitiveType.unsafePut(mv);
                return RETURN;
            });
        });
    }

    private void generateComputeAddress(MethodVisitor mv, int addressLvt, int indexLvt) {
        mv.visitVarInsn(LLOAD, addressLvt); //address + (index * type.size())
        mv.visitVarInsn(LLOAD, indexLvt);
        generateMultiplyConstant(mv, (long) this.type.size());
        mv.visitInsn(LADD);
    }
}
