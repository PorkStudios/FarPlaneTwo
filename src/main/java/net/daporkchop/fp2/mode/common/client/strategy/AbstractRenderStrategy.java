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

package net.daporkchop.fp2.mode.common.client.strategy;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.FP2Client;
import net.daporkchop.fp2.client.GlStateUniformAttributes;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.client.TextureUVs;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.uniform.UniformBuffer;
import net.daporkchop.fp2.gl.attribute.uniform.UniformFormat;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.draw.command.DrawCommand;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.mode.common.client.strategy.texture.LightmapTextureAttribute;
import net.daporkchop.fp2.mode.common.client.strategy.texture.TerrainTextureAttribute;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Base implementation of {@link IFarRenderStrategy}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractRenderStrategy<POS extends IFarPos, T extends IFarTile, BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand> extends AbstractRefCounted implements IFarRenderStrategy<POS, T, BO, DB, DC> {
    protected final Allocator alloc = new DirectMemoryAllocator();

    protected final IFarRenderMode<POS, T> mode;
    protected final GL gl;

    protected final UniformFormat<GlStateUniformAttributes> uniformFormat;
    protected final UniformBuffer<GlStateUniformAttributes> uniformBuffer;

    protected final TextureFormat2D<TerrainTextureAttribute> textureFormatTerrain;
    protected final Texture2D<TerrainTextureAttribute> textureTerrain;
    protected final TextureFormat2D<LightmapTextureAttribute> textureFormatLightmap;
    protected final Texture2D<LightmapTextureAttribute> textureLightmap;

    protected final TextureUVs textureUVs;

    protected final ShaderMacros.Mutable macros = new ShaderMacros.Mutable(FP2Client.GLOBAL_SHADER_MACROS);

    public AbstractRenderStrategy(@NonNull IFarRenderMode<POS, T> mode, @NonNull GL gl) {
        this.mode = mode;
        this.gl = gl;

        this.uniformFormat = gl.createUniformFormat(GlStateUniformAttributes.class);
        this.uniformBuffer = this.uniformFormat.createBuffer(BufferUsage.STATIC_DRAW);

        this.textureFormatTerrain = gl.createTextureFormat2D(TerrainTextureAttribute.class);
        this.textureTerrain = this.textureFormatTerrain.wrapExternalTexture(MC.getTextureMapBlocks().getGlTextureId());
        this.textureFormatLightmap = gl.createTextureFormat2D(LightmapTextureAttribute.class);
        this.textureLightmap = this.textureFormatLightmap.wrapExternalTexture(MC.getTextureManager().getTexture(MC.entityRenderer.locationLightMap).getGlTextureId());

        this.textureUVs = new TextureUVs(gl);
    }

    @Override
    public IFarRenderStrategy<POS, T, BO, DB, DC> retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        this.textureUVs.close();
        this.uniformBuffer.close();
    }

    protected void preRender() {
        this.macros.define("FP2_FOG_ENABLED", glGetBoolean(GL_FOG));
        this.macros.define("FP2_FOG_MODE", glGetInteger(GL_FOG_MODE));
    }
}
