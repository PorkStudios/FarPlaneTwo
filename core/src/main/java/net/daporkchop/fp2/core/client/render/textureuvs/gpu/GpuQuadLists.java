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

package net.daporkchop.fp2.core.client.render.textureuvs.gpu;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.engine.client.RenderConstants;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.state.StatePreserver;

import java.util.List;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class GpuQuadLists implements AutoCloseable {
    /**
     * Creates a new instance of {@link GpuQuadLists}.
     *
     * @param fp2 the current {@link FP2Core} instance
     * @return a new instance of {@link GpuQuadLists}
     */
    public static GpuQuadLists createQuadLists(@NonNull FP2Core fp2) {
        val gl = fp2.client().gl();
        val globalRenderer = fp2.client().globalRenderer();

        if (gl.supports(SSBOGpuQuadLists.REQUIRED_EXTENSIONS)) {
            return new SSBOGpuQuadLists(gl, globalRenderer);
        } else {
            logUnsupported(fp2, SSBOGpuQuadLists.class, SSBOGpuQuadLists.REQUIRED_EXTENSIONS);
        }

        if (gl.supports(BufferTextureGpuQuadLists.REQUIRED_EXTENSIONS)) {
            return new BufferTextureGpuQuadLists(gl);
        } else {
            logUnsupported(fp2, BufferTextureGpuQuadLists.class, BufferTextureGpuQuadLists.REQUIRED_EXTENSIONS);
        }

        if (gl.supports(Texture2dGpuQuadLists.REQUIRED_EXTENSIONS)) {
            return new Texture2dGpuQuadLists(gl);
        } else {
            logUnsupported(fp2, Texture2dGpuQuadLists.class, Texture2dGpuQuadLists.REQUIRED_EXTENSIONS);
        }

        throw new UnsupportedOperationException("No texture UV implementations are supported! (see log)");
    }

    private static void logUnsupported(FP2Core fp2, Class<? extends GpuQuadLists> clazz, GLExtensionSet requiredExtensions) {
        fp2.log().warn("Texture UV implementation " + clazz + " isn't supported by your OpenGL implementation! " + fp2.client().gl().unsupportedMsg(requiredExtensions));
    }

    /**
     * The OpenGL context.
     */
    public final @NonNull OpenGL gl;

    /**
     * The {@link QuadsTechnique} used by this {@link TextureUVs} to push block state quad data to the shader.
     */
    public final @NonNull GpuQuadLists.QuadsTechnique quadsTechnique;

    @Override
    public abstract void close();

    /**
     * Updates the quad lists and quad data on the GPU.
     *
     * @param lists the new quad lists
     * @param quads the new quad data
     */
    public abstract void set(@NonNull List<TextureUVs.QuadList> lists, @NonNull List<TextureUVs.PackedBakedQuad> quads);

    /**
     * Configures the given {@link StatePreserver} builder to preserve any OpenGL state which may be modified by {@link #bind(OpenGL)}.
     *
     * @param builder the {@link StatePreserver} builder to configure
     */
    public void preservedBindState(StatePreserver.Builder builder) {
        //no-op
    }

    /**
     * Binds the texture UVs to the OpenGL context.
     *
     * @param gl the OpenGL context
     */
    public abstract void bind(OpenGL gl);

    /**
     * A technique describing how shaders should access the texture UVs for a block state ID.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    public enum QuadsTechnique { //synced with resources/assets/fp2/shaders/util/texture_uvs_technique.glsl
        /**
         * The shader declares the following:
         * <br>
         * <ul>
         *     <li>An SSBO at {@link RenderConstants#TEXTURE_UVS_LISTS_SSBO_BINDING}, containing an array of {@link TextureUVs.QuadList}s</li>
         *     <li>An SSBO at {@link RenderConstants#TEXTURE_UVS_QUADS_SSBO_BINDING}, containing an array of {@link TextureUVs.PackedBakedQuad}s</li>
         * </ul>
         */
        SSBO(SSBOGpuQuadLists.REQUIRED_EXTENSIONS),
        /**
         * The shader declares the following:
         * <br>
         * <ul>
         *     <li>A buffer texture at {@link RenderConstants#TEXTURE_UVS_LISTS_SAMPLERBUFFER_BINDING}, containing an array of {@link TextureUVs.QuadList}s</li>
         *     <li>A buffer texture at {@link RenderConstants#TEXTURE_UVS_QUADS_COORD_SAMPLERBUFFER_BINDING}, containing an array of {@link TextureUVs.PackedBakedQuad}s' coords</li>
         *     <li>A buffer texture at {@link RenderConstants#TEXTURE_UVS_QUADS_TINT_SAMPLERBUFFER_BINDING}, containing an array of {@link TextureUVs.PackedBakedQuad}s' tint factors</li>
         * </ul>
         */
        BUFFER_TEXTURE(BufferTextureGpuQuadLists.REQUIRED_EXTENSIONS),
        /**
         * The shader declares the following:
         * <br>
         * <ul>
         *     <li>A 2D texture at {@link RenderConstants#TEXTURE_UVS_LISTS_SAMPLER2D_BINDING}, containing an array of {@link TextureUVs.QuadList}s</li>
         *     <li>A 2D texture at {@link RenderConstants#TEXTURE_UVS_QUADS_COORD_SAMPLER2D_BINDING}, containing an array of {@link TextureUVs.PackedBakedQuad}s' coords</li>
         *     <li>A 2D texture at {@link RenderConstants#TEXTURE_UVS_QUADS_TINT_SAMPLER2D_BINDING}, containing an array of {@link TextureUVs.PackedBakedQuad}s' tint factors</li>
         * </ul>
         * <p>
         * Each of the textures is a {@link Texture2dGpuQuadLists#X_COORD_WRAP}x{@code h}-pixel grid, where image coordinates wrap around along the X axis.
         */
        TEXTURE_2D(Texture2dGpuQuadLists.REQUIRED_EXTENSIONS),
        ;

        private final @NonNull GLExtensionSet requiredExtensions;
    }
}
