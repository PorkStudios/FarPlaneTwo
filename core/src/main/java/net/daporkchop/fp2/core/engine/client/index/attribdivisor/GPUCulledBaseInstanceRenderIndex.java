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
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.shader.NewReloadableShaderProgram;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.postable.PerLevelRenderPosTable;
import net.daporkchop.fp2.core.engine.client.index.postable.RenderPosTable;
import net.daporkchop.fp2.core.engine.client.index.postable.SimpleRenderPosTable;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.indirect.DrawElementsIndirectCommand;
import net.daporkchop.fp2.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;

/**
 * Render index implementation using MultiDrawIndirect with an attribute divisor for the tile position which does frustum culling on tiles on the GPU.
 *
 * @author DaPorkchop_
 */
public class GPUCulledBaseInstanceRenderIndex<VertexType extends AttributeStruct> extends AbstractMultiDrawIndirectRenderIndex<VertexType> {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = AbstractMultiDrawIndirectRenderIndex.REQUIRED_EXTENSIONS
            .addAll(ComputeShaderProgram.REQUIRED_EXTENSIONS)
            .add(GLExtension.GL_ARB_shader_storage_buffer_object)
            .add(GLExtension.GL_ARB_shader_image_load_store); //glMemoryBarrier()

    /*
     * Implementation overview:
     *
     * On the OpenGL side, we use three buffers:
     * - A buffer containing a list of all baked tile positions (undefined contents for unloaded tiles)
     * - rawDrawList, a buffer containing RENDER_PASS_COUNT indirect draw commands per tile position (zero for unloaded or hidden tiles, or when the tile
     *   has no data for the corresponding render pass). This is managed by the CPU, and is updated every time the tile data changes.
     * - culledDrawList, buffer containing RENDER_PASS_COUNT indirect draw commands per tile position. On each frame, we populate this based on rawDrawList
     *   (omitting commands when the corresponding tile position is outside the view frustum) and then use it as the source for indirect multidraw commands.
     *   It is never accessed directly by the CPU.
     *
     * On each frame, we do the following:
     * 1. Upload the tile positions and raw draw lists, if changed
     * 2. Invalidate culledDrawList
     * 3. Fill the culledDrawList buffer using a compute shader
     */

    private final Map<TilePos, DrawElementsIndirectCommand[]> drawCommands = DirectTilePosAccess.newPositionKeyedHashMap();
    private final LevelPassArray<DirectCommandList> commandLists;
    private final LevelPassArray<GLMutableBuffer> glBuffers;

    private final NewReloadableShaderProgram<ComputeShaderProgram> indirectTileFrustumCullingShaderProgram;
    private final int tilesCulledPerWorkGroup = 16 * 16; //TODO: a nicer way to acquire this without hardcoding it

    public GPUCulledBaseInstanceRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, NewAttributeFormat<VoxelGlobalAttributes> sharedVertexFormat, GlobalRenderer globalRenderer) {
        super(gl, bakeStorage, alloc, sharedVertexFormat);

        try {
            this.indirectTileFrustumCullingShaderProgram = Objects.requireNonNull(globalRenderer.indirectTileFrustumCullingShaderProgram);
            //this.tilesCulledPerWorkGroup = this.indirectTileFrustumCullingShaderProgram.get().workGroupSize().invocations();

            this.commandLists = new LevelPassArray<>((level, pass) -> new DirectCommandList(alloc));
            this.glBuffers = new LevelPassArray<>((level, pass) -> null); //TODO
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        try (val ignored0 = this.commandLists) {
            super.close();
        }
    }

    @Override
    protected RenderPosTable constructRenderPosTable(NewAttributeFormat<VoxelGlobalAttributes> sharedVertexFormat) {
        val step = this.tilesCulledPerWorkGroup; //TODO: this will always be uninitialized if i stop hardcoding the value
        val growFunction = Allocator.GrowFunction.sqrt2(step);
        return new PerLevelRenderPosTable(level -> new SimpleRenderPosTable(this.gl, sharedVertexFormat, this.alloc, growFunction));
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
        this.renderPosTable.flush();

        //iterate over all the tile positions and draw commands
        /*this.drawCommands.forEach((pos, commands) -> {
            DirectCommandList[] commandLists = this.commandLists[pos.level()];
            if ((pos.level() > 0 || !blockedTracker.renderingBlocked(pos.x(), pos.y(), pos.z()))
                && frustum.intersectsBB(pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(), pos.maxBlockX(), pos.maxBlockY(), pos.maxBlockZ())) {
                //the tile is in the frustum, add all the draw commands to the corresponding draw lists
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    if (commands[pass] != null) {
                        commandLists[pass].add(commands[pass]);
                    }
                }
            }
        });*/
    }

    @Override
    public void draw(DrawMode mode, int level, int pass, DrawShaderProgram shader, ShaderProgram.UniformSetter uniformSetter) {
        val list = this.commandLists.get(level, pass);
        if (list.size != 0) {
            //this.gl.glBindVertexArray(this.vaos.vao(level, pass).id());
            this.gl.glMultiDrawElementsIndirect(mode.mode(), this.bakeStorage.indexFormat.type().type(), list.address, list.size, 0);
        }
    }

    @Override
    public PosTechnique posTechnique() {
        return PosTechnique.VERTEX_ATTRIBUTE;
    }
}
