/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.client.render.common;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.event.ReloadEvent;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.core.util.Direction;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.lib.primitive.map.ObjIntMap;
import net.daporkchop.lib.primitive.map.open.ObjIntOpenHashMap;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Base implementation of {@link TextureUVs}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractTextureUVs extends AbstractReleasable implements TextureUVs {
    protected final FGameRegistry registry;

    protected final AttributeFormat<QuadList> listsFormat;
    protected final AttributeBuffer<QuadList> listsBuffer;

    protected final AttributeFormat<PackedBakedQuad> quadsFormat;
    protected final AttributeBuffer<PackedBakedQuad> quadsBuffer;

    protected int[] stateIdToIndexId;

    public AbstractTextureUVs(@NonNull FGameRegistry registry, @NonNull GL gl) {
        this.registry = registry;

        this.listsFormat = gl.createAttributeFormat(QuadList.class).useFor(AttributeUsage.UNIFORM_ARRAY).build();
        this.listsBuffer = this.listsFormat.createBuffer(BufferUsage.STATIC_DRAW);

        this.quadsFormat = gl.createAttributeFormat(PackedBakedQuad.class).useFor(AttributeUsage.UNIFORM_ARRAY).build();
        this.quadsBuffer = this.quadsFormat.createBuffer(BufferUsage.STATIC_DRAW);

        fp2().eventBus().registerWeak(this);
    }

    @Override
    protected void doRelease() {
        fp2().eventBus().unregister(this);

        this.quadsBuffer.close();
        this.listsBuffer.close();
    }

    @FEventHandler
    protected void onReload(@NonNull ReloadEvent<TextureUVs> event) {
        event.doReload(this::reloadUVs);
    }

    protected void reloadUVs() {
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
                    List<PackedBakedQuad> quads = fp2().eventBus().fireAndGetFirst(new StateFaceQuadRenderEvent(this.registry, state, direction)).orElse(missingTextureQuads);

                    int id = distinctQuadsToId.getOrDefault(quads, -1);
                    if (id < 0) { //allocate new ID
                        distinctQuadsToId.put(quads, id = distinctQuadsToId.size());
                        distinctQuadsById.add(quads);
                    }

                    faceIds[direction.ordinal()] = id;
                }
            } catch (Exception e) { //we couldn't process the state (likely a buggy mod block), so let's just ignore it for now
                fp2().log().error("exception while generating texture UVs for %s", e, this.registry.id2state(state));
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
            fp2().log().error("failed to generate texture UVs for %d block states, they will be replaced with missing textures:", erroredStates.size());
            erroredStates.forEach(state -> fp2().log().error(state.toString()));

            int id = stateIdToIndexId.get(0); //air
            erroredStates.forEach(state -> stateIdToIndexId.put((int) state, id));
        }

        int[] realStateIdToIndexId = new int[this.registry.states().max().getAsInt() + 1];
        stateIdToIndexId.forEach((state, indexId) -> realStateIdToIndexId[state] = indexId);
        this.stateIdToIndexId = realStateIdToIndexId;

        QuadList[] quadIdToList = new QuadList[distinctQuadsById.size()];
        List<PackedBakedQuad> quadsOut = new ArrayList<>(distinctQuadsById.size());
        for (int i = 0, len = distinctQuadsById.size(); i < len; i++) {
            List<PackedBakedQuad> quads = distinctQuadsById.get(i);
            quadIdToList[i] = new QuadList(quadsOut.size(), quadsOut.size() + quads.size());
            quadsOut.addAll(quads);
        }
        this.quadsBuffer.setContents(quadsOut.toArray(new PackedBakedQuad[0]));

        List<QuadList> listsOut = new ArrayList<>(quadIdToList.length);
        for (int[] faceIds : distinctIndicesById) {
            for (int i : faceIds) {
                listsOut.add(quadIdToList[i]);
            }
        }
        this.listsBuffer.setContents(listsOut.toArray(new QuadList[0]));
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
