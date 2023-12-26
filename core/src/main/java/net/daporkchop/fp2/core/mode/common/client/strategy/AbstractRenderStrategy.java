/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.common.client.strategy;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.event.generic.FChangedEvent;
import net.daporkchop.fp2.api.event.generic.FReloadEvent;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.render.GlobalUniformAttributes;
import net.daporkchop.fp2.core.client.render.LevelRenderer;
import net.daporkchop.fp2.core.client.render.RenderInfo;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.client.shader.ReloadableShaderProgram;
import net.daporkchop.fp2.core.client.shader.ShaderMacros;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.common.client.AbstractFarRenderer;
import net.daporkchop.fp2.core.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.core.mode.common.client.index.IRenderIndex;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelRange;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
import net.daporkchop.fp2.gl.command.CommandBuffer;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.common.util.exception.AlreadyReleasedException;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of {@link IFarRenderStrategy}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractRenderStrategy<BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand> extends AbstractRefCounted implements IFarRenderStrategy<BO, DB, DC> {
    protected final Allocator alloc = new DirectMemoryAllocator();

    protected final AbstractFarRenderer farRenderer;
    protected final LevelRenderer levelRenderer;
    protected final IFarRenderMode mode;
    protected final GL gl;

    protected final AttributeFormat<GlobalUniformAttributes> uniformFormat;
    protected final AttributeBuffer<GlobalUniformAttributes> uniformBuffer;

    protected final TextureFormat2D textureFormatTerrain;
    protected final Texture2D textureTerrain;
    protected final TextureFormat2D textureFormatLightmap;
    protected final Texture2D textureLightmap;

    protected final TextureUVs textureUVs;

    protected final ShaderMacros.Mutable macros = new ShaderMacros.Mutable(fp2().client().globalShaderMacros());
    protected ShaderMacros.Immutable lastMacrosSnapshot;

    protected CommandBuffer commandBuffer;

    public AbstractRenderStrategy(@NonNull AbstractFarRenderer farRenderer) {
        this.farRenderer = farRenderer;
        this.levelRenderer = farRenderer.levelRenderer();
        this.mode = farRenderer.mode();
        this.gl = farRenderer.gl();

        this.uniformFormat = this.gl.createAttributeFormat(GlobalUniformAttributes.class).useFor(AttributeUsage.UNIFORM).build();
        this.uniformBuffer = this.uniformFormat.createBuffer(BufferUsage.STATIC_DRAW);

        this.textureFormatTerrain = this.gl.createTextureFormat2D(
                this.gl.createPixelFormat().rgba().type(PixelFormatChannelType.FLOATING_POINT).range(PixelFormatChannelRange.ZERO_TO_ONE).build(),
                "terrain"
        ).build();
        this.textureTerrain = this.textureFormatTerrain.wrapExternalTexture(this.levelRenderer.terrainTextureId());
        this.textureFormatLightmap = this.gl.createTextureFormat2D(
                this.gl.createPixelFormat().rgba().type(PixelFormatChannelType.FLOATING_POINT).range(PixelFormatChannelRange.ZERO_TO_ONE).build(),
                "lightmap"
        ).build();
        this.textureLightmap = this.textureFormatLightmap.wrapExternalTexture(this.levelRenderer.lightmapTextureId());

        this.textureUVs = this.levelRenderer.textureUVs();

        this.macros.define("T_SHIFT", this.mode.tileShift());

        fp2().eventBus().registerWeak(this);
    }

    @Override
    public IFarRenderStrategy<BO, DB, DC> retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        //close uniform buffer
        try (AttributeBuffer<GlobalUniformAttributes> uniformBuffer = this.uniformBuffer) {
            fp2().eventBus().unregister(this);
        }
    }

    @Override
    public void render(@NonNull IRenderIndex<BO, DB, DC> index, @NonNull RenderInfo renderInfo) {
        //rebuild command buffer if needed
        if (this.commandBuffer == null || this.lastMacrosSnapshot != this.macros.snapshot()) {
            this.rebuildCommandBuffer(index);
        }

        //update uniforms
        try (GlobalUniformAttributes globalUniformAttributes = this.uniformBuffer.setToSingle()) {
            renderInfo.configureGlobalUniformAttributes(globalUniformAttributes);
        }
        assert this.uniformBuffer.capacity() == 1;

        //execute command buffer
        this.commandBuffer.execute();
    }

    protected void rebuildCommandBuffer(@NonNull IRenderIndex<BO, DB, DC> index) {
        if (this.commandBuffer != null) { //close the existing command buffer, if any
            this.commandBuffer.close();
            this.commandBuffer = null;
        }

        this.lastMacrosSnapshot = this.macros.snapshot();

        CommandBufferBuilder builder = this.gl.createCommandBuffer();
        this.render(builder, index);
        this.commandBuffer = builder.build();
    }

    public abstract void render(@NonNull CommandBufferBuilder builder, @NonNull IRenderIndex<BO, DB, DC> index);

    @FEventHandler(constrain = @Constrain(monitor = true))
    protected void onShaderReloadComplete(FReloadEvent<ReloadableShaderProgram<?>> event) {
        this.lastMacrosSnapshot = null; //force command buffer rebuild on next frame
    }

    @FEventHandler
    protected void onConfigChanged(FChangedEvent<FP2Config> event) {
        this.lastMacrosSnapshot = null; //reversed-z settings may have changed, force command buffer rebuild on next frame
    }

    @FEventHandler
    protected void onReloadCommandBuffer(FReloadEvent<CommandBuffer> event) {
        //command buffer rebuild was explicitly requested, do it immediately (this assumes we're on the client thread...)
        event.doReload(() -> this.rebuildCommandBuffer(uncheckedCast(this.farRenderer.bakeManager().index())));
    }
}
