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

import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.AbstractRenderIndex;
import net.daporkchop.fp2.core.engine.client.index.RenderIndex;
import net.daporkchop.fp2.core.engine.client.index.RenderIndexType;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;
import net.daporkchop.fp2.gl.buffer.BufferAccess;
import net.daporkchop.fp2.gl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.indirect.DrawElementsIndirectCommand;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.fp2.gl.util.list.DirectDrawElementsIndirectCommandList;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Map;

import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * Render index implementation using MultiDrawIndirect with an attribute divisor for the tile position which does frustum culling on tiles on the CPU.
 *
 * @author DaPorkchop_
 */
public class CPUCulledBaseInstanceRenderIndex<VertexType extends AttributeStruct> extends AbstractRenderIndex.WithTilePosAttrib<VertexType> {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = WithTilePosAttrib.REQUIRED_EXTENSIONS
            .add(GLExtension.GL_ARB_multi_draw_indirect); //glMultiDrawElementsIndirect()

    /**
     * @author DaPorkchop_
     */
    public static final class Type extends RenderIndexType {
        public Type() {
            super(REQUIRED_EXTENSIONS);
        }

        @Override
        public <VertexType extends AttributeStruct> RenderIndex<VertexType> createRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer) {
            return new CPUCulledBaseInstanceRenderIndex<>(gl, bakeStorage, alloc, globalRenderer);
        }
    }

    private final Map<TilePos, DrawElementsIndirectCommand[]> drawCommands = DirectTilePosAccess.newPositionKeyedHashMap();
    private int selectedTilesCount = 0;

    private final LevelPassArray<DirectDrawElementsIndirectCommandList> commandLists;
    private final GLMutableBuffer commandListBuffer;
    private final int[][] commandListBufferOffsets = new int[EngineConstants.MAX_LODS][RENDER_PASS_COUNT];

    public CPUCulledBaseInstanceRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer) {
        super(gl, bakeStorage, alloc, globalRenderer);

        try {
            this.commandLists = new LevelPassArray<>((level, pass) -> new DirectDrawElementsIndirectCommandList(alloc));

            this.commandListBuffer = GLMutableBuffer.create(gl);
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        try (val ignored0 = this.commandLists;
             val ignored1 = this.commandListBuffer) {
            super.close();
        }
    }

    @Override
    protected void recomputeTile(@NonNull TilePos pos) {
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

        this.uploadCommandLists();
    }

    private void uploadCommandLists() {
        //compute total capacity of all command lists
        int totalCapacity = 0;
        for (val commandList : this.commandLists) {
            totalCapacity += commandList.capacity();
        }

        //resize buffer if needed
        if (this.commandListBuffer.capacity() != totalCapacity * DrawElementsIndirectCommand._SIZE) {
            this.commandListBuffer.capacity(totalCapacity * DrawElementsIndirectCommand._SIZE, BufferUsage.STREAM_DRAW);
        }

        //upload all the command lists
        try (val mapping = this.commandListBuffer.mapRange(BufferAccess.WRITE_ONLY, GL_MAP_INVALIDATE_BUFFER_BIT, 0L, this.commandListBuffer.capacity())) {
            for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    this.commandListBufferOffsets[level][pass] = mapping.buffer.position();
                    mapping.buffer.put(this.commandLists.get(level, pass).byteBufferView());
                }
            }
        }
    }

    @Override
    public void preservedDrawState(StatePreserver.Builder builder) {
        super.preservedDrawState(builder.vao()
                .buffer(BufferTarget.DRAW_INDIRECT_BUFFER));
    }

    @Override
    public void preDraw() {
        super.preDraw();
        this.renderPosTable.flush();
        this.gl.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.commandListBuffer.id());
    }

    @Override
    public void draw(DrawMode mode, int level, int pass, DrawShaderProgram shader, ShaderProgram.UniformSetter uniformSetter) {
        val list = this.commandLists.get(level, pass);
        int size = list.size();
        if (size != 0) {
            this.gl.glBindVertexArray(this.vaos.get(level, pass).id());
            this.gl.glMultiDrawElementsIndirect(mode.mode(), this.bakeStorage.indexFormat.type().type(), this.commandListBufferOffsets[level][pass], size, 0);
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
