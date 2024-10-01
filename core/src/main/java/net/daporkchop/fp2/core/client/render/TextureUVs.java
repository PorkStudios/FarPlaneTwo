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

package net.daporkchop.fp2.core.client.render;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.api.event.ReturningEvent;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.render.textureuvs.gpu.GpuQuadLists;
import net.daporkchop.fp2.core.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeIgnore;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarType;
import net.daporkchop.fp2.gl.attribute.annotation.VectorType;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.List;

/**
 * @author DaPorkchop_
 */
public interface TextureUVs {
    static void reloadAll(@NonNull FP2Client client) {
        client.textureUVsReloadListeners().dispatcher().reloadUVs();
        client.chat().success("§areloaded texture UV cache(s)"); //TODO: determine total count or handle failure

        /*new AbstractReloadEvent<TextureUVs>() {
            @Override
            protected void handleSuccess(int total) {
                fp2().client().chat().success("§areloaded %d texture UV cache(s)", total);
            }

            @Override
            protected void handleFailure(int failed, int total, @NonNull Throwable cause) {
                fp2().log().error("texture UV cache reload failed", cause);
                fp2().client().chat().error("§c%d/%d texture UV cache failed to reload (check log for info)", failed, total);
            }
        }.fire();*/
    }

    GpuQuadLists gpuQuadLists();

    int state2index(int state);

    /**
     * @author DaPorkchop_
     */
    @Data
    final class QuadList {
        private final @NotNegative int texQuadListFirst;
        private final @NotNegative int texQuadListLast;
    }

    /**
     * @author DaPorkchop_
     */
    @Attribute(name = "texQuadList", typeVector = @VectorType(components = 2, componentType = @ScalarType(value = int.class, interpret = @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED))))
    interface QuadListAttribute extends AttributeStruct {
        @AttributeSetter
        QuadListAttribute texQuadList(int texQuadListFirst, int texQuadListLast);

        @AttributeIgnore
        default QuadListAttribute copyFrom(QuadList quadList) {
            return this.texQuadList(quadList.texQuadListFirst(), quadList.texQuadListLast());
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Data
    final class PackedBakedQuad {
        private final float texQuadCoordS;
        private final float texQuadCoordT;
        private final float texQuadCoordP;
        private final float texQuadCoordQ;

        private final float texQuadTint;
    }

    /**
     * @author DaPorkchop_
     */
    @Attribute(name = "texQuadCoord", typeVector = @VectorType(components = 4, componentType = @ScalarType(float.class)))
    @Attribute(name = "texQuadTint", typeScalar = @ScalarType(float.class))
    interface PackedBakedQuadAttribute extends AttributeStruct {
        @AttributeSetter
        PackedBakedQuadAttribute texQuadCoord(float s, float t, float p, float q);

        @AttributeSetter
        PackedBakedQuadAttribute texQuadTint(float tint);

        @AttributeIgnore
        default PackedBakedQuadAttribute copyFrom(PackedBakedQuad quad) {
            return this.texQuadCoord(quad.texQuadCoordS(), quad.texQuadCoordT(), quad.texQuadCoordP(), quad.texQuadCoordQ())
                    .texQuadTint(quad.texQuadTint());
        }
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

    /**
     * @author DaPorkchop_
     */
    interface ReloadListener {
        @CalledFromClientThread
        void reloadUVs();
    }
}
