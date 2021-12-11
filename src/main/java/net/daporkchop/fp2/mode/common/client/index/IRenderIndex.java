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

package net.daporkchop.fp2.mode.common.client.index;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutput;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import java.util.Map;
import java.util.Optional;

/**
 * Contains baked render output for tiles at all detail levels, as well as keeping track of which tiles are renderable.
 *
 * @author DaPorkchop_
 */
public interface IRenderIndex<POS extends IFarPos, BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand> extends RefCounted {
    /**
     * Executes multiple updates in bulk.
     * <p>
     * For data updates, each position may be mapped to one of three different things:<br>
     * - an {@link Optional} containing a non-empty {@link BO}, in which case the bake output will be executed and inserted at the position<br>
     * - an empty {@link Optional}, in which case the position will be removed
     *
     * @param dataUpdates       the data updates to be applied
     * @param renderableUpdates the updates to tile renderability be applied
     */
    void update(@NonNull Iterable<Map.Entry<POS, Optional<BO>>> dataUpdates, @NonNull Iterable<Map.Entry<POS, Boolean>> renderableUpdates);

    /**
     * Should be called before issuing any draw commands.
     * <p>
     * This will determine which tiles need to be rendered for the current frame.
     */
    void select(@NonNull IFrustum frustum);

    /**
     * Draws a single render pass at the given level.
     *
     * @param builder the {@link CommandBufferBuilder} to render to
     * @param level   the level to render
     * @param pass    the pass to render
     * @param shader  the {@link DrawShaderProgram} to render with
     */
    void draw(@NonNull CommandBufferBuilder builder, int level, int pass, @NonNull DrawShaderProgram shader);

    @Override
    int refCnt();

    @Override
    IRenderIndex<POS, BO, DB, DC> retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;

    DebugStats.Renderer stats();
}
