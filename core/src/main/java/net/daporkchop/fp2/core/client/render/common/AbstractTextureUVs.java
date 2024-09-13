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

package net.daporkchop.fp2.core.client.render.common;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.util.listener.ListenerList;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.lib.common.misc.release.AbstractReleasable;
import net.daporkchop.lib.primitive.map.ObjIntMap;
import net.daporkchop.lib.primitive.map.open.ObjIntOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base implementation of {@link TextureUVs}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractTextureUVs extends AbstractReleasable implements TextureUVs, TextureUVs.ReloadListener {
    protected final FP2Core fp2;
    protected final FGameRegistry registry;

    protected final AttributeBuffer<QuadListAttribute> listsBuffer;
    protected final AttributeBuffer<PackedBakedQuadAttribute> quadsBuffer;

    protected int[] stateIdToIndexId;

    private final ListenerList<TextureUVs.ReloadListener>.Handle reloadListenerHandle;

    public AbstractTextureUVs(@NonNull FP2Core fp2, @NonNull FGameRegistry registry) {
        this.fp2 = fp2;
        this.registry = registry;

        this.listsBuffer = fp2.client().globalRenderer().uvQuadListSSBOFormat.createBuffer();
        this.quadsBuffer = fp2.client().globalRenderer().uvPackedQuadSSBOFormat.createBuffer();

        this.reloadListenerHandle = fp2.client().textureUVsReloadListeners().add(this);
    }

    @Override
    protected void doRelease() {
        this.reloadListenerHandle.close();

        this.quadsBuffer.close();
        this.listsBuffer.close();
    }

    @Override
    public void reloadUVs() {
        ObjIntMap<List<PackedBakedQuad>> distinctQuadsToId = new ObjIntOpenHashMap<>();
        List<List<PackedBakedQuad>> distinctQuadsById = new ArrayList<>();

        ObjIntMap<int[]> distinctIndicesToId = new IntArrayEqualsMap();
        List<int[]> distinctIndicesById = new ArrayList<>();

        Int2IntMap stateIdToIndexId = new Int2IntOpenHashMap();

        List<PackedBakedQuad> missingTextureQuads = this.missingTextureQuads();

        IntList erroredStates = new IntArrayList();

        this.registry.states().forEach(state -> {
            int[] faceIds = new int[6];

            try {
                for (Direction direction : Direction.VALUES) {
                    List<PackedBakedQuad> quads = this.fp2.eventBus().fireAndGetFirst(new StateFaceQuadRenderEvent(this.registry, state, direction)).orElse(missingTextureQuads);

                    int id = distinctQuadsToId.getOrDefault(quads, -1);
                    if (id < 0) { //allocate new ID
                        distinctQuadsToId.put(quads, id = distinctQuadsToId.size());
                        distinctQuadsById.add(quads);
                    }

                    faceIds[direction.ordinal()] = id;
                }
            } catch (Exception e) { //we couldn't process the state (likely a buggy mod block), so let's just ignore it for now
                this.fp2.log().error("exception while generating texture UVs for %s", e, this.registry.id2state(state));
                erroredStates.add(state);
                return;
            }

            int id = distinctIndicesToId.get(faceIds);
            if (id < 0) { //allocate new ID
                distinctIndicesToId.put(faceIds, id = distinctIndicesToId.size());
                distinctIndicesById.add(faceIds);
            }

            stateIdToIndexId.put(state, id);
        });

        if (!erroredStates.isEmpty()) { //some block states failed!
            this.fp2.log().error("failed to generate texture UVs for %d block states, they will be replaced with missing textures:", erroredStates.size());
            erroredStates.forEach(state -> this.fp2.log().error(state.toString()));

            int id = stateIdToIndexId.get(0); //air
            erroredStates.forEach(state -> stateIdToIndexId.put((int) state, id));
        }

        int[] realStateIdToIndexId = new int[this.registry.statesCount()];
        stateIdToIndexId.forEach((state, indexId) -> realStateIdToIndexId[state] = indexId);
        this.stateIdToIndexId = realStateIdToIndexId;

        DirectMemoryAllocator alloc = new DirectMemoryAllocator(false);

        QuadList[] quadIdToList = new QuadList[distinctQuadsById.size()];
        try (val quadsOut = this.quadsBuffer.format().createWriter(alloc)) {
            for (int i = 0, len = distinctQuadsById.size(); i < len; i++) {
                List<PackedBakedQuad> quads = distinctQuadsById.get(i);
                quadIdToList[i] = new QuadList(quadsOut.size(), quadsOut.size() + quads.size());
                quads.forEach(quad -> quadsOut.append().copyFrom(quad).close());
            }

            this.quadsBuffer().set(quadsOut, BufferUsage.STATIC_DRAW);
        }

        try (val listsOut = this.listsBuffer.format().createWriter(alloc)) {
            for (int[] faceIds : distinctIndicesById) {
                for (int i : faceIds) {
                    listsOut.append().copyFrom(quadIdToList[i]).close();
                }
            }

            this.listsBuffer().set(listsOut, BufferUsage.STATIC_DRAW);
        }
    }

    @Override
    public int state2index(int state) {
        return this.stateIdToIndexId[state];
    }

    /**
     * @return the {@link PackedBakedQuad}s used as a fallback when no other textures are available
     */
    protected abstract List<PackedBakedQuad> missingTextureQuads();

    /**
     * @author DaPorkchop_
     */
    private static class IntArrayEqualsMap extends ObjIntOpenHashMap<int[]> {
        @Override
        protected int hash0(int[] key) {
            return Arrays.hashCode(key);
        }

        @Override
        protected boolean equals0(int[] k1, int[] k2) {
            return Arrays.equals(k1, k2);
        }
    }
}
