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

package net.daporkchop.fp2.core.engine.client.index.attribdivisor;

import lombok.val;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.indirect.DrawElementsIndirectCommand;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.fp2.gl.util.list.DirectDrawElementsIndirectCommandList;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;

/**
 * Render index implementation using MultiDrawIndirect with an attribute divisor for the tile position which does frustum culling on tiles on the CPU.
 *
 * @author DaPorkchop_
 */
public class CPUCulledBaseInstanceRenderIndex<VertexType extends AttributeStruct> extends AbstractMultiDrawIndirectRenderIndex<VertexType> {
    private final Map<TilePos, DrawElementsIndirectCommand[]> drawCommands = DirectTilePosAccess.newPositionKeyedHashMap();
    private int selectedTilesCount = 0;

    private final LevelPassArray<DirectDrawElementsIndirectCommandList> commandLists;

    public CPUCulledBaseInstanceRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, AttributeFormat<VoxelGlobalAttributes> sharedVertexFormat) {
        super(gl, bakeStorage, alloc, sharedVertexFormat);

        try {
            this.commandLists = new LevelPassArray<>((level, pass) -> new DirectDrawElementsIndirectCommandList(alloc));
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        try (val ignored = this.commandLists) {
            super.close();
        }
    }

    private Consumer<TilePos> recomputeTile() {
        return pos -> {
            BakeStorage.Location[] locations = this.bakeStorage.find(pos);
            if (locations == null || this.hiddenPositions.contains(pos)) {
                //the position doesn't have any render data data associated with it or is hidden, and therefore can be entirely omitted from the index
                this.renderPosTable.remove(pos);
                this.drawCommands.remove(pos);
            } else {
                //configure the render commands which will be used when selecting this
                int baseInstance = this.renderPosTable.add(pos);

                DrawElementsIndirectCommand[] commands = new DrawElementsIndirectCommand[RENDER_PASS_COUNT];
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    BakeStorage.Location location = locations[pass];
                    if (location != null) {
                        val command = commands[pass] = new DrawElementsIndirectCommand();
                        command.count = location.count;
                        command.baseInstance = baseInstance;
                        command.firstIndex = location.firstIndex;
                        command.baseVertex = location.baseVertex;
                        command.instanceCount = 1;
                    }
                }
                this.drawCommands.put(pos, commands);
            }
        };
    }

    @Override
    public void notifyTilesChanged(Set<TilePos> changedPositions) {
        changedPositions.forEach(this.recomputeTile());
    }

    @Override
    public void updateHidden(Set<TilePos> hidden, Set<TilePos> shown) {
        super.updateHidden(hidden, shown);

        //recompute the draw commands for all affected tiles, so that all tiles which got hidden get excluded from the index
        hidden.forEach(this.recomputeTile());
        shown.forEach(this.recomputeTile());
    }

    @Override
    public void select(IFrustum frustum, TerrainRenderingBlockedTracker blockedTracker) {
        //clear all the lists
        this.commandLists.forEach(DirectDrawElementsIndirectCommandList::clear);

        //iterate over all the tile positions and draw commands
        this.selectedTilesCount = 0;
        this.drawCommands.forEach((pos, commands) -> {
            int level = pos.level();
            if ((level > 0 || !blockedTracker.renderingBlocked(pos.x(), pos.y(), pos.z()))
                && frustum.intersectsBB(pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(), pos.maxBlockX(), pos.maxBlockY(), pos.maxBlockZ())) {
                //the tile is in the frustum, add all the draw commands to the corresponding draw lists
                this.selectedTilesCount++;

                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    if (commands[pass] != null) {
                        this.commandLists.get(level, pass).add(commands[pass]);
                    }
                }
            }
        });
    }

    @Override
    public void preservedDrawState(StatePreserver.Builder builder) {
        super.preservedDrawState(builder
                .vao());
    }

    @Override
    public void preDraw() {
        super.preDraw();
        this.renderPosTable.flush();
    }

    @Override
    public void draw(DrawMode mode, int level, int pass, DrawShaderProgram shader, ShaderProgram.UniformSetter uniformSetter) {
        val list = this.commandLists.get(level, pass);
        int size = list.size();
        if (size != 0) {
            long indirect = PUnsafe.pork_directBufferAddress(list.byteBufferView());
            this.gl.glBindVertexArray(this.vaos.get(level, pass).id());
            this.gl.glMultiDrawElementsIndirect(mode.mode(), this.bakeStorage.indexFormat.type().type(), indirect, size, 0);
        }
    }

    @Override
    public PosTechnique posTechnique() {
        return PosTechnique.VERTEX_ATTRIBUTE;
    }

    @Override
    public DebugStats.Renderer stats() {
        return DebugStats.Renderer.builder()
                .selectedTiles(this.selectedTilesCount)
                .indexedTiles(this.drawCommands.size())
                .build();
    }
}
