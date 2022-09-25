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
 */

package net.daporkchop.fp2.core.client.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.api.event.ReturningEvent;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.event.AbstractReloadEvent;
import net.daporkchop.fp2.core.util.Direction;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarType;
import net.daporkchop.fp2.gl.attribute.annotation.VectorType;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;

import java.util.List;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
public interface TextureUVs {
    static void reloadAll() {
        new AbstractReloadEvent<TextureUVs>() {
            @Override
            protected void handleSuccess(int total) {
                fp2().client().chat().success("§areloaded %d texture UV cache(s)", total);
            }

            @Override
            protected void handleFailure(int failed, int total, @NonNull Throwable cause) {
                fp2().log().error("texture UV cache reload failed", cause);
                fp2().client().chat().error("§c%d/%d texture UV cache failed to reload (check log for info)", failed, total);
            }
        }.fire();
    }

    AttributeFormat<QuadList> listsFormat();

    AttributeBuffer<QuadList> listsBuffer();

    AttributeFormat<PackedBakedQuad> quadsFormat();

    AttributeBuffer<PackedBakedQuad> quadsBuffer();

    int state2index(int state);

    /**
     * @author DaPorkchop_
     */
    @Attribute(name = "texQuadList", typeVector = @VectorType(components = 2, componentType = @ScalarType(value = int.class, interpret = @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED))))
    interface QuadList {
        @AttributeSetter
        QuadList texQuadList(int texQuadListFirst, int texQuadListLast);
    }

    /**
     * @author DaPorkchop_
     */
    @Attribute(name = "texQuadList", typeVector = @VectorType(components = 2, componentType = @ScalarType(value = int.class, interpret = @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED))))
    @Attribute(name = "tint", typeScalar = @ScalarType(float.class))
    interface PackedBakedQuad {
        @AttributeSetter
        PackedBakedQuad texQuadCoord(float s, float t, float p, float q);

        @AttributeSetter
        PackedBakedQuad texQuadTint(float tint);
    }

    /**
     * @author DaPorkchop_
     */
    @AllArgsConstructor
    @Getter
    @Setter
    class StateFaceQuadRenderEvent implements ReturningEvent<List<PackedBakedQuad>> {
        @NonNull
        private final FGameRegistry registry;
        private int state;
        @NonNull
        private Direction direction;
    }
}
