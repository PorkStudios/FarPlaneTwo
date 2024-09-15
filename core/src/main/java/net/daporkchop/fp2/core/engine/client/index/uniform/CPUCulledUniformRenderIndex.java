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

package net.daporkchop.fp2.core.engine.client.index.uniform;

import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.RenderConstants;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.AbstractRenderIndex;
import net.daporkchop.fp2.core.engine.client.index.RenderIndex;
import net.daporkchop.fp2.core.engine.client.index.RenderIndexType;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.lib.common.util.PArrays;

import java.util.Map;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class CPUCulledUniformRenderIndex<VertexType extends AttributeStruct> extends AbstractRenderIndex.OnlyVertexTypeAttribs<VertexType> {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = OnlyVertexTypeAttribs.REQUIRED_EXTENSIONS
            .add(GLExtension.GL_ARB_draw_elements_base_vertex);

    /**
     * @author DaPorkchop_
     */
    public static final class Type extends RenderIndexType {
        public Type() {
            super(REQUIRED_EXTENSIONS);
        }

        @Override
        public <VertexType extends AttributeStruct> RenderIndex<VertexType> createRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer) {
            return new CPUCulledUniformRenderIndex<>(gl, bakeStorage, alloc);
        }
    }

    //TODO: this isn't even remotely as efficient as it could be, but i don't care
    protected final Map<TilePos, BakeStorage.Location[]> allLocations = DirectTilePosAccess.newPositionKeyedHashMap();
    protected final Map<TilePos, BakeStorage.Location[]>[] selectedLocations = uncheckedCast(PArrays.filledFrom(MAX_LODS, Map[]::new, DirectTilePosAccess::newPositionKeyedHashMap));

    public CPUCulledUniformRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc) {
        super(gl, bakeStorage, alloc);
    }

    @Override
    protected void recomputeTile(@NonNull TilePos pos) {
        BakeStorage.Location[] locations = this.bakeStorage.find(pos);
        if (locations == null || this.hiddenPositions.contains(pos)) {
            //the position doesn't have any render data data associated with it or is hidden, and therefore can be entirely omitted from the index
            this.allLocations.remove(pos);
        } else {
            this.allLocations.put(pos, locations);
        }
    }

    @Override
    public void select(IFrustum frustum, TerrainRenderingBlockedTracker blockedTracker) {
        //clear all the lists
        for (val level : this.selectedLocations) {
            level.clear();
        }

        //iterate over all the tile positions and draw commands
        this.allLocations.forEach((pos, commands) -> {
            int level = pos.level();
            if ((level > 0 || !blockedTracker.renderingBlocked(pos.x(), pos.y(), pos.z()))
                && frustum.intersectsBB(pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(), pos.maxBlockX(), pos.maxBlockY(), pos.maxBlockZ())) {
                this.selectedLocations[pos.level()].put(pos, commands);
            }
        });
    }

    @Override
    public void preservedDrawState(StatePreserver.Builder builder) {
        super.preservedDrawState(builder.vao());
    }

    @Override
    public void draw(DrawMode mode, int level, int pass, DrawShaderProgram shader, ShaderProgram.UniformSetter uniformSetter) {
        val selectedLocations = this.selectedLocations[level];
        if (!selectedLocations.isEmpty()) {
            val indexFormat = this.bakeStorage.indexFormat.type();
            int indexType = indexFormat.type();
            int indexSize = indexFormat.size();

            this.gl.glBindVertexArray(this.vaos.get(level, pass).id());

            int uniformLocation = shader.uniformLocation(RenderConstants.TILE_POS_UNIFORM_NAME);

            selectedLocations.forEach((pos, commands) -> {
                val command = commands[pass];
                if (command != null) {
                    uniformSetter.set4i(uniformLocation, pos.x(), pos.y(), pos.z(), pos.level());

                    //TODO: if we make all the indices absolute, we won't have to rely on ARB_draw_elements_base_vertex
                    this.gl.glDrawElementsBaseVertex(mode.mode(), command.count, indexType, command.firstIndex * (long) indexSize, command.baseVertex);
                }
            });
        }
    }

    @Override
    public PosTechnique posTechnique() {
        return PosTechnique.UNIFORM;
    }

    @Override
    public DebugStats.Renderer stats() {
        return DebugStats.Renderer.builder()
                .selectedTiles(Stream.of(this.selectedLocations).mapToInt(Map::size).sum())
                .indexedTiles(this.allLocations.size())
                .build();
    }
}
