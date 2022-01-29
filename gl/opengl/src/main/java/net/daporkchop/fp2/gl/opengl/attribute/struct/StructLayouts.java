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

package net.daporkchop.fp2.gl.opengl.attribute.struct;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.TextureStructLayout;
import net.daporkchop.lib.common.math.PMath;

import java.util.List;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class StructLayouts {
    public <S> InterleavedStructLayout<S> vertexAttributesInterleaved(@NonNull OpenGL gl, @NonNull StructInfo<S> structInfo, boolean unpacked) {
        List<StructMember<S>> members = structInfo.members();
        int memberCount = structInfo.members().size();

        long[] memberOffsets = new long[memberCount];
        long[][] memberComponentOffsets = new long[memberCount][];

        long alignment = gl.vertexAttributeAlignment();

        long offset = 0L;
        for (int i = 0; i < memberCount; i++) {
            StructMember.Stage stage = unpacked ? members.get(i).unpackedStage : members.get(i).packedStage;

            memberOffsets[i] = offset;
            offset += PMath.roundUp(stage.components() * (long) stage.componentType().stride(), alignment);

            long componentOffset = 0L;
            long[] componentOffsets = memberComponentOffsets[i] = new long[stage.components()];
            for (int component = 0; component < stage.components(); component++, componentOffset += stage.componentType().stride()) {
                componentOffsets[component] = componentOffset;
            }
        }

        return InterleavedStructLayout.<S>builder()
                .structInfo(structInfo)
                .layoutName("vertex_attribute_interleaved")
                .unpacked(unpacked)
                .memberOffsets(memberOffsets)
                .memberComponentOffsets(memberComponentOffsets)
                .stride(offset)
                .build();
    }

    public <S> TextureStructLayout<S> texture(@NonNull OpenGL gl, @NonNull StructInfo<S> structInfo) {
        List<StructMember<S>> members = structInfo.members();
        checkArg(members.size() == 1, "expected exactly one attribute, but found %d! %s", members.size(), structInfo);

        StructMember.Stage stage = members.get(0).packedStage;

        return TextureStructLayout.<S>builder()
                .structInfo(structInfo)
                .layoutName("texture")
                .unpacked(false)
                .componentStride(stage.componentType().stride())
                .stride(stage.components() * (long) stage.componentType().stride())
                .build();
    }
}
