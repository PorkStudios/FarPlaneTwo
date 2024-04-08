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
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.indirect.DrawElementsIndirectCommand;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;

/**
 * @author DaPorkchop_
 */
public class CPUCulledBaseInstanceRenderIndex<VertexType extends AttributeStruct> extends AbstractMultiDrawIndirectRenderIndex<VertexType> {
    private final Map<TilePos, DrawElementsIndirectCommand[]> drawCommands = DirectTilePosAccess.newPositionKeyedHashMap();
    private final DirectCommandList[][] commandLists = new DirectCommandList[MAX_LODS][RENDER_PASS_COUNT];

    public CPUCulledBaseInstanceRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, NewAttributeFormat<VoxelGlobalAttributes> sharedVertexFormat) {
        super(gl, bakeStorage, alloc, sharedVertexFormat);

        for (int level = 0; level < MAX_LODS; level++) {
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                this.commandLists[level][pass] = new DirectCommandList(alloc);
            }
        }
    }

    @Override
    public void close() {
        super.close();
        for (val lists : this.commandLists) {
            for (val list : lists) {
                list.close();
            }
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
    public void select(IFrustum frustum) {
        //clear all the lists
        for (val lists : this.commandLists) {
            for (val list : lists) {
                list.size = 0;
            }
        }

        //iterate over all the tile positions and draw commands
        this.drawCommands.forEach((pos, commands) -> {
            DirectCommandList[] commandLists = this.commandLists[pos.level()];
            if (frustum.intersectsBB(pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(), pos.maxBlockX(), pos.maxBlockY(), pos.maxBlockZ())) {
                //the tile is in the frustum, add all the draw commands to the corresponding draw lists
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    if (commands[pass] != null) {
                        commandLists[pass].add(commands[pass]);
                    }
                }
            }
        });
    }

    @Override
    public void draw(DrawMode mode, int level, int pass) {
        this.renderPosTable.flush();

        val list = this.commandLists[level][pass];
        if (list.size != 0) {
            this.gl.glBindVertexArray(this.vaos[level][pass].id());
            this.gl.glMultiDrawElementsIndirect(mode.mode(), this.bakeStorage.indexFormat.type().type(), list.address, list.size, 0);
        }
    }
}
