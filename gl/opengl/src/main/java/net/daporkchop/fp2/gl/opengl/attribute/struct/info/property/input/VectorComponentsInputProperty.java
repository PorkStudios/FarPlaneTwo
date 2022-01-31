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

package net.daporkchop.fp2.gl.opengl.attribute.struct.info.property.input;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.ComponentType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.property.StructProperty;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class VectorComponentsInputProperty implements StructProperty.Components {
    private final ComponentType componentType;
    private final Field[] fields;

    public VectorComponentsInputProperty(@NonNull Field[] fields) {
        Class<?> type = fields[0].getType();
        for (Field field : fields) {
            checkArg(field.getType() == type, "mismatched type: %s (expected %s)", field, type);
        }

        this.componentType = ComponentType.from(type);
        this.fields = fields;
    }

    @Override
    public int components() {
        return this.fields.length;
    }

    @Override
    public void load(@NonNull MethodVisitor mv, int structLvtIndex, int lvtIndexAllocator, @NonNull LoadCallback callback) {
        callback.accept(structLvtIndex, lvtIndexAllocator, componentIndex -> {
            Field field = this.fields[componentIndex];

            mv.visitVarInsn(ALOAD, structLvtIndex);
            mv.visitFieldInsn(GETFIELD, getInternalName(field.getDeclaringClass()), field.getName(), getDescriptor(field.getType()));
        });
    }
}
