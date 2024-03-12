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

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeFactory;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.layout.LayoutInfo;
import net.daporkchop.fp2.gl.codegen.struct.layout.assignment.Std140BlockMemoryLayout;

import java.util.Set;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class AttributeFormatFactory {
    public static <STRUCT extends AttributeStruct> NewAttributeFormat getAttributeFormat(OpenGL gl, Class<STRUCT> structClass, Set<AttributeTarget> requestedSupportedTargets) {
        LayoutInfo layout = getAttributeLayout(gl, structClass, requestedSupportedTargets);
        checkArg(layout.interleaved(), "separate attribute layout isn't supported!");
        return new InterleavedStructFormatClassLoader<>(structClass, layout).createAttributeFormat(gl);
    }

    private static LayoutInfo getAttributeLayout(OpenGL gl, Class<? extends AttributeStruct> structClass, Set<AttributeTarget> requestedSupportedTargets) {
        StructAttributeType structType = StructAttributeFactory.struct(structClass);

        checkArg(!requestedSupportedTargets.isEmpty(), "at least one target must be requested");
        if (Std140BlockMemoryLayout.compatibleTargets(gl).containsAll(requestedSupportedTargets)) {
            return Std140BlockMemoryLayout.computeLayout(structType);
        }

        throw new IllegalArgumentException("unable to determine layout for " + structClass + " supporting " + requestedSupportedTargets);
    }
}
