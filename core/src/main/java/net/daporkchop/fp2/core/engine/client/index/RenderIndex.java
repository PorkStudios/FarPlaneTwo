/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.client.index;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.state.CameraState;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.client.render.state.DrawState;
import net.daporkchop.fp2.core.client.render.state.DrawStateUniforms;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.RenderConstants;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.state.StatePreserver;
import net.daporkchop.lib.common.annotation.TransferOwnership;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.primitive.lambda.IntIntObjFunction;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class RenderIndex<VertexType extends AttributeStruct> implements AutoCloseable {
    protected static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.empty();

    /**
     * The OpenGL context.
     */
    public final OpenGL gl;

    /**
     * The {@link BakeStorage} which stores the baked tile data.
     * <p>
     * This is not owned by the render index. User code is responsible for closing the storage.
     */
    public final BakeStorage<VertexType> bakeStorage;

    /**
     * The {@link DirectMemoryAllocator} which is used for allocating direct memory needed by this index.
     */
    public final DirectMemoryAllocator alloc;

    @Override
    public void close() {
        //no-op
    }

    /**
     * Notifies this index that the tiles at the given positions may have been changed in the bake storage.
     *
     * @param changedPositions the tile positions which changed
     */
    public abstract void notifyTilesChanged(Set<TilePos> changedPositions);

    /**
     * Updates the internal set of hidden tile positions.
     * <p>
     * Hidden tiles are excluded from tile selection, and will never be rendered until shown again. By default, all tiles are visible.
     *
     * @param hidden the tile positions to hide
     * @param shown  the tile positions to un-hide
     */
    public abstract void updateHidden(Set<TilePos> hidden, Set<TilePos> shown);

    /**
     * Configures the given {@link StatePreserver} builder to preserve any OpenGL state which may be modified by this render index while selecting rendered tiles.
     *
     * @param builder the {@link StatePreserver} builder to configure
     */
    public void preservedSelectState(StatePreserver.Builder builder) {
        //no-op
    }

    /**
     * Determine which tiles need to be rendered for the current frame.
     *
     * @param frustum        the current view frustum
     * @param blockedTracker a {@link TerrainRenderingBlockedTracker} for tracking which level-0 tiles are blocked from rendering
     */
    public abstract void select(IFrustum frustum, TerrainRenderingBlockedTracker blockedTracker);

    /**
     * Gets a {@link DrawableMask} indicating which LoDs and render passes currently contain drawable tiles.
     * <p>
     * Note that the returned mask may be an over-approximation.
     *
     * @return a {@link DrawableMask} indicating which LoDs and render passes currently contain drawable tiles
     */
    public DrawableMask drawableMask() {
        return DrawableMask.EVERYTHING;
    }

    /**
     * @author DaPorkchop_
     */
    public static final class DrawableMask {
        static {
            //sanity checks so that i don't end up breaking these assumptions in the future

            //noinspection ConstantValue
            assert RenderConstants.RENDER_PASS_COUNT <= Byte.SIZE;
            assert EngineConstants.MAX_LODS <= Integer.SIZE;
        }

        static final DrawableMask NOTHING = new DrawableMask(new byte[EngineConstants.MAX_LODS]);
        static final DrawableMask EVERYTHING = new DrawableMask(PArrays.filled(EngineConstants.MAX_LODS, (byte) ((1 << RenderConstants.RENDER_PASS_COUNT) - 1)));

        private final int drawableLevelMask;
        private final byte drawableLayerMask;
        private final byte[] drawableLayerMaskPerLevel;

        @Getter
        private final int minLevelToDraw; //inclusive
        @Getter
        private final int maxLevelToDraw; //exclusive

        private static int reduceDrawableLevelMask(byte[] drawableLayerMaskPerLevel) {
            int drawableLevelMask = 0;
            for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
                drawableLevelMask |= drawableLayerMaskPerLevel[level] != 0 ? (1 << level) : 0;
            }
            return drawableLevelMask;
        }

        private static byte reduceDrawableLayerMask(byte[] drawableLayerMaskPerLevel) {
            byte drawablePassMask = 0;
            for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
                drawablePassMask |= drawableLayerMaskPerLevel[level];
            }
            return drawablePassMask;
        }

        public DrawableMask(byte[] drawableLayerMaskPerLevel) {
            checkArg(drawableLayerMaskPerLevel.length == EngineConstants.MAX_LODS, drawableLayerMaskPerLevel.length);

            this.drawableLevelMask = reduceDrawableLevelMask(drawableLayerMaskPerLevel);
            this.drawableLayerMask = reduceDrawableLayerMask(drawableLayerMaskPerLevel);
            this.drawableLayerMaskPerLevel = drawableLayerMaskPerLevel;

            this.minLevelToDraw = Integer.numberOfTrailingZeros(this.drawableLevelMask);
            this.maxLevelToDraw = Integer.numberOfTrailingZeros(Integer.highestOneBit(this.drawableLevelMask) << 1);
        }

        public DrawableMask copyOfLevelRange(@NotNegative int minLevelInclusive, @NotNegative int maxLevelExclusive) {
            checkRange(EngineConstants.MAX_LODS, minLevelInclusive, maxLevelExclusive);

            int levelMask = ((1 << (maxLevelExclusive - minLevelInclusive)) - 1) << minLevelInclusive;
            if ((this.drawableLevelMask & ~levelMask) == 0) { //there is already nothing renderable outside the given range
                return this;
            }

            byte[] drawablePassMaskPerLevel = new byte[EngineConstants.MAX_LODS];
            System.arraycopy(this.drawableLayerMaskPerLevel, minLevelInclusive, drawablePassMaskPerLevel, minLevelInclusive, maxLevelExclusive - minLevelInclusive);
            return new DrawableMask(drawablePassMaskPerLevel);
        }

        public boolean shouldDrawAnything() {
            return this.drawableLevelMask != 0;
        }

        public boolean shouldDrawAnythingForLevel(@NotNegative int level) {
            checkIndex(EngineConstants.MAX_LODS, level);
            return (this.drawableLevelMask & (1 << level)) != 0;
        }

        public boolean shouldDrawAnythingForLayer(@NotNegative int layer) {
            checkIndex(RenderConstants.RENDER_PASS_COUNT, layer);
            return (this.drawableLayerMask & (1 << layer)) != 0;
        }

        public boolean shouldDrawAnythingForLevelLayer(@NotNegative int level, @NotNegative int layer) {
            checkIndex(EngineConstants.MAX_LODS, level);
            checkIndex(RenderConstants.RENDER_PASS_COUNT, layer);
            return (this.drawableLayerMaskPerLevel[level] & (1 << layer)) != 0;
        }
    }

    /**
     * Configures the given {@link StatePreserver} builder to preserve any OpenGL state which may be modified by this render index while drawing.
     *
     * @param builder the {@link StatePreserver} builder to configure
     */
    public void preservedDrawState(StatePreserver.Builder builder) {
        //no-op
    }

    /**
     * Called after {@link #select}, but before any calls to {@link #draw}.
     *
     * @param args additional draw-related state
     */
    public void preDraw(DrawArguments args) {
        //no-op
    }

    /**
     * Draws the selected tiles at the given detail level using the currently bound shader.
     *
     * @param args          additional draw-related state
     * @param level         the detail level
     * @param pass          the render pass
     * @param shader        the shader which is currently bound and is going to be rendered with
     * @param uniformSetter a handle for setting uniform values in the draw shader
     */
    public abstract void draw(DrawArguments args, int level, int pass, DrawShaderProgram shader, ShaderProgram.UniformSetter uniformSetter);

    /**
     * Called after {@link #preDraw(DrawArguments)} and after all calls to {@link #draw}.
     *
     * @param args additional draw-related state
     */
    public void postDraw(DrawArguments args) {
        //no-op
    }

    /**
     * @return the technique used by this render index to push tile positions to the shader
     */
    public abstract PosTechnique posTechnique();

    /**
     * @return debug statistics describing this render index and its current state
     */
    public abstract Stats stats();

    /**
     * Debug statistics for the render index.
     *
     * @author DaPorkchop_
     */
    @Data
    public static final class Stats {
        /**
         * The number of tiles which were selected during the previous frame. Negative if unknown/unsupported.
         */
        private final int selectedTiles;

        /**
         * The number of tiles which were considered for selection during the previous frame. Negative if unknown/unsupported.
         */
        private final int indexedTiles;

        /**
         * The number of tile positions which are explicitly marked as hidden and are excluded from selection.
         */
        private final @NotNegative int hiddenTiles;

        /**
         * The number of draw commands which were selected during the previous frame. Negative if unknown/unsupported.
         */
        private final int selectedCommands;

        /**
         * The number of draw commands which were considered for selection during the previous frame. Negative if unknown/unsupported.
         */
        private final int indexedCommands;

        /**
         * The render index implementation's name.
         */
        private final @NonNull String implName;
    }

    /**
     * Additional useful state variables available during the draw phase.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    public static final class DrawArguments {
        public final @NonNull CameraState cameraState;
        public final @NonNull UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer;
        public final @NonNull DrawState drawState;
        public final @NonNull UniformBuffer<DrawStateUniforms> drawStateUniformBuffer;

        public final @NonNull TerrainRenderingBlockedTracker blockedTracker;

        public final boolean reversedZ;
    }

    /**
     * A technique describing how shaders should access the tile position for a tile.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    public enum PosTechnique { //synced with resources/assets/fp2/shaders/util/tile_pos_technique.glsl
        /**
         * The shader declares ordinary vertex attributes for {@link net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes}.
         * <p>
         * The render index is responsible for ensuring that the corresponding attribute arrays are bound to data representing the tile position, which
         * is likely done using a vertex attribute divisor and issuing instanced draws using a corresponding BaseInstance value.
         */
        VERTEX_ATTRIBUTE(GLExtensionSet.empty()),
        /**
         * The shader declares a uniform buffer at binding location {@link RenderConstants#TILE_POS_ARRAY_UBO_BINDING}. The buffer must use the {@code std140}
         * layout, and contain an array of {@code ivec4} of length {@link RenderConstants#TILE_POS_ARRAY_UBO_ELEMENTS}.
         * <p>
         * The shader can access the tile position of the current tile by reading the uniform element at index {@code gl_DrawID}.
         */
        UNIFORM_ARRAY_DRAWID(GLExtensionSet.empty()
                .add(GLExtension.GL_ARB_uniform_buffer_object) // we obviously need UBOs in order to do this
                .add(GLExtension.GL_ARB_shader_draw_parameters) // GLSL: gl_DrawID
        ),
        /**
         * The shader declares a uniform {@code ivec4} named {@link RenderConstants#TILE_POS_UNIFORM_NAME}.
         */
        UNIFORM(GLExtensionSet.empty()),
        ;

        private final @NonNull GLExtensionSet requiredExtensions;
    }

    /**
     * A 2-dimensional array indexed by detail level and render pass.
     *
     * @author DaPorkchop_
     */
    private static abstract class AbstractCloseableArray<E extends AutoCloseable> implements Iterable<E>, AutoCloseable {
        protected final E[] delegate;

        public AbstractCloseableArray(int size) {
            this.delegate = uncheckedCast(new AutoCloseable[size]);
        }

        @Override
        public Iterator<E> iterator() {
            return Arrays.asList(this.delegate).iterator();
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            for (E value : this.delegate) {
                action.accept(value);
            }
        }

        @Override
        public void close() {
            PResourceUtil.closeAll(this);
        }

        protected void set(int index, @TransferOwnership E value) {
            //get the old value, then delete it and replace it with the new value
            try (E ignored = this.delegate[index]) {
                this.delegate[index] = value;
            } catch (Throwable t) {
                if (index >= 0 && index < this.delegate.length) {
                    //the index is in-bounds, meaning that the exception was thrown while calling close() on the old value. to avoid
                    // closing a value twice, we'll set it to null!
                    this.delegate[index] = null;
                }
                throw PResourceUtil.closeSuppressed(t, value);
            }
        }
    }

    /**
     * A 1-dimensional array indexed by detail level.
     *
     * @author DaPorkchop_
     */
    protected static final class LevelArray<E extends AutoCloseable> extends AbstractCloseableArray<E> {
        public LevelArray(IntFunction<E> creator) {
            super(EngineConstants.MAX_LODS);
            try {
                for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
                    this.delegate[level] = Objects.requireNonNull(creator.apply(level));
                }
            } catch (Throwable t) {
                throw PResourceUtil.closeAllSuppressed(t, this);
            }
        }

        /**
         * Gets the element at the given detail level.
         *
         * @param level the detail level
         * @return the element at the given detail level
         */
        public E get(@NotNegative int level) {
            return this.delegate[level];
        }

        /**
         * Sets the element at the given detail level to the given value, closing the previous value in the process.
         * <p>
         * If an exception is thrown, the new value will be closed and the array's contents are undefined.
         *
         * @param level the detail level
         * @param value the new value
         */
        public void set(@NotNegative int level, @TransferOwnership E value) {
            super.set(level, value);
        }
    }

    /**
     * A 1-dimensional array indexed by render pass.
     *
     * @author DaPorkchop_
     */
    protected static final class PassArray<E extends AutoCloseable> extends AbstractCloseableArray<E> {
        public PassArray(IntFunction<E> creator) {
            super(RenderConstants.RENDER_PASS_COUNT);
            try {
                for (int level = 0; level < RenderConstants.RENDER_PASS_COUNT; level++) {
                    this.delegate[level] = Objects.requireNonNull(creator.apply(level));
                }
            } catch (Throwable t) {
                throw PResourceUtil.closeAllSuppressed(t, this);
            }
        }

        /**
         * Gets the element at the given render pass index.
         *
         * @param pass the render pass index
         * @return the element at the given render pass index
         */
        public E get(@NotNegative int pass) {
            return this.delegate[pass];
        }

        /**
         * Sets the element at the given render pass index to the given value, closing the previous value in the process.
         * <p>
         * If an exception is thrown, the new value will be closed and the array's contents are undefined.
         *
         * @param pass  the render pass index
         * @param value the new value
         */
        public void set(@NotNegative int pass, @TransferOwnership E value) {
            super.set(pass, value);
        }
    }

    /**
     * A 2-dimensional array indexed by detail level and render pass.
     *
     * @author DaPorkchop_
     */
    protected static final class LevelPassArray<E extends AutoCloseable> extends AbstractCloseableArray<E> {
        public LevelPassArray(IntIntObjFunction<E> creator) {
            super(EngineConstants.MAX_LODS * RenderConstants.RENDER_PASS_COUNT);
            try {
                for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
                    for (int pass = 0; pass < RenderConstants.RENDER_PASS_COUNT; pass++) {
                        this.delegate[level * RenderConstants.RENDER_PASS_COUNT + pass] = Objects.requireNonNull(creator.apply(level, pass));
                    }
                }
            } catch (Throwable t) {
                throw PResourceUtil.closeAllSuppressed(t, this);
            }
        }

        /**
         * Gets the element at the given detail level and render pass index.
         *
         * @param level the detail level
         * @param pass  the render pass index
         * @return the element at the given detail level and render pass index
         */
        public E get(@NotNegative int level, @NotNegative int pass) {
            checkIndex(EngineConstants.MAX_LODS, level);
            checkIndex(RenderConstants.RENDER_PASS_COUNT, pass);
            return this.delegate[level * RenderConstants.RENDER_PASS_COUNT + pass];
        }

        /**
         * Sets the element at the given detail level and render pass index to the given value, closing the previous value in the process.
         * <p>
         * If an exception is thrown, the new value will be closed and the array's contents are undefined.
         *
         * @param level the detail level
         * @param pass  the render pass index
         * @param value the new value
         */
        public void set(@NotNegative int level, @NotNegative int pass, @TransferOwnership E value) {
            try {
                checkIndex(EngineConstants.MAX_LODS, level);
                checkIndex(RenderConstants.RENDER_PASS_COUNT, pass);
            } catch (Throwable t) {
                //If the index is out-of-bounds, we should try to close the value again since we currently own it.
                //If only there were a language which could automatically destroy owned resources when they go out of scope! Just imagine that.
                throw PResourceUtil.closeSuppressed(t, value);
            }
            super.set(level * RenderConstants.RENDER_PASS_COUNT + pass, value);
        }
    }
}
