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
import net.daporkchop.fp2.client.gl.shader.reload.ShaderMacros;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.uniform.UniformAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.uniform.UniformAttributeFormat;
import net.daporkchop.fp2.gl.binding.DrawBinding;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.command.DrawCommand;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutput;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

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

    protected final UniformAttributeFormat<GlStateUniformAttributes> uniformFormat;
    protected final UniformAttributeBuffer<GlStateUniformAttributes> uniformBuffer;

    protected final ShaderMacros.Mutable macros = new ShaderMacros.Mutable(FP2Client.GLOBAL_SHADER_MACROS);

    public AbstractRenderStrategy(@NonNull IFarRenderMode<POS, T> mode, @NonNull GL gl) {
        this.mode = mode;
        this.gl = gl;

        this.uniformFormat = gl.createUniformFormat(GlStateUniformAttributes.class);
        this.uniformBuffer = this.uniformFormat.createBuffer(BufferUsage.STATIC_DRAW);
    }

    @Override
    public IFarRenderStrategy<POS, T, BO, DB, DC> retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        this.uniformBuffer.close();
    }

    protected void preRender() {
        this.macros.define("FP2_FOG_ENABLED", glGetBoolean(GL_FOG));
        this.macros.define("FP2_FOG_MODE", glGetInteger(GL_FOG_MODE));
    }
}
