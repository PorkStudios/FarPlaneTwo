/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.core.client.render;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.event.AbstractReloadEvent;
import net.daporkchop.fp2.gl.attribute.Attribute;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayBuffer;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayFormat;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
public interface TextureUVs {
    static void reloadAll() {
        new AbstractReloadEvent<TextureUVs>() {
            @Override
            protected void handleSuccess(int total) {
                fp2().client().chat().success("§a%d texture UV caches successfully reloaded.", total);
            }

            @Override
            protected void handleFailure(int failed, int total, @NonNull Throwable cause) {
                fp2().log().error("texture UV cache reload failed", cause);
                fp2().client().chat().error("§c%d/%d texture UV cache failed to reload (check log for info)", failed, total);
            }
        }.fire();
    }

    UniformArrayFormat<QuadList> listsFormat();

    UniformArrayBuffer<QuadList> listsBuffer();

    UniformArrayFormat<PackedBakedQuad> quadsFormat();

    UniformArrayBuffer<PackedBakedQuad> quadsBuffer();

    int state2index(int state);

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    final class QuadList {
        @Attribute(vectorAxes = { "First", "Last" }, convert = Attribute.Conversion.TO_UNSIGNED)
        public final int ua_texQuadListFirst;
        public final int ua_texQuadListLast;
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    final class PackedBakedQuad {
        @Attribute(vectorAxes = { "S", "T", "P", "Q" })
        public final float ua_texQuadCoordS;
        public final float ua_texQuadCoordT;
        public final float ua_texQuadCoordP;
        public final float ua_texQuadCoordQ;

        @Attribute
        public final float ua_texQuadTint;
    }
}
