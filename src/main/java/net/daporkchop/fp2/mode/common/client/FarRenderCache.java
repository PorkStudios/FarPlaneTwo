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

package net.daporkchop.fp2.mode.common.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class FarRenderCache<POS extends IFarPos, T extends IFarTile> {
    protected final AbstractFarRenderer<POS, T> renderer;
    protected final IFarRenderStrategy<POS, T> strategy;

    protected final FarRenderTree<POS, T> tree;
    protected final Map<POS, Compressed<POS, T>> tiles = new ConcurrentHashMap<>();

    protected final IntFunction<POS[]> posArray;
    protected final IntFunction<T[]> tileArray;

    public FarRenderCache(@NonNull AbstractFarRenderer<POS, T> renderer) {
        this.renderer = renderer;
        this.strategy = renderer.strategy();

        this.posArray = renderer.mode()::posArray;
        this.tileArray = renderer.mode()::tileArray;

        this.tree = new FarRenderTree<>(renderer.mode(), this.strategy, renderer.maxLevel());
    }

    public void receiveTile(@NonNull Compressed<POS, T> tileIn) {
        final int maxLevel = this.renderer.maxLevel;

        this.tiles.put(tileIn.pos(), tileIn);

        this.strategy.bakeOutputs(tileIn.pos())
                .forEach(pos -> {
                    if (pos.level() < 0 || pos.level() > maxLevel) {
                        return;
                    }

                    Compressed<POS, T>[] compressedInputTiles = uncheckedCast(this.strategy.bakeInputs(pos)
                            .map(this.tiles::get)
                            .toArray(Compressed[]::new));

                    Compressed<POS, T> tile = Arrays.stream(compressedInputTiles).filter(p -> p != null && pos.equals(p.pos())).findAny().orElse(null);
                    if (tile == null) {
                        return;
                    }

                    RENDER_WORKERS.submit(pos, () -> {
                        if (this.tiles.get(pos) != tile) { //tile was modified before this task could be executed
                            return;
                        }

                        SimpleRecycler<T> recycler = this.renderer.mode().tileRecycler();
                        T[] inputTiles = this.tileArray.apply(compressedInputTiles.length);
                        try {
                            for (int i = 0; i < inputTiles.length; i++) { //inflate tiles
                                if (compressedInputTiles[i] != null) {
                                    inputTiles[i] = compressedInputTiles[i].inflate(recycler);
                                }
                            }

                            BakeOutput output = new BakeOutput(this.strategy.renderDataSize());
                            if (this.strategy.bake(pos, inputTiles, output)) {
                                ClientThreadExecutor.INSTANCE.execute(() -> this.addTile(tile, output));
                            } else { //baked tile contains nothing, so we remove it
                                ClientThreadExecutor.INSTANCE.execute(() -> this.addTile(tile, null));
                            }
                        } finally { //release tiles again
                            for (int i = 0; i < inputTiles.length; i++) {
                                if (inputTiles[i] != null) {
                                    recycler.release(inputTiles[i]);
                                }
                            }
                        }
                    });
                });
    }

    protected void addTile(@NonNull Compressed<POS, T> tile, BakeOutput output) {
        POS pos = tile.pos();
        if (this.tiles.get(pos) != tile) {
            return;
        }

        if (output != null) { //finish bake and insert to tree
            this.strategy.executeBakeOutput(tile.pos(), output);
            this.tree.putRenderData(pos, output);
        } else { //remove tile from tree
            this.tree.removeNode(pos);
        }
    }

    public void unloadTile(@NonNull POS pos) {
        RENDER_WORKERS.submit(pos, () -> { //make sure that any in-progress bake tasks are finished before the tile is removed
            ClientThreadExecutor.INSTANCE.execute(() -> this.removeTile(pos));
        });
    }

    public void removeTile(@NonNull POS pos) {
        if (this.tiles.remove(pos) == null) {
            Constants.LOGGER.warn("Attempted to unload already non-existent tile at {}!", pos);
        }
        this.tree.removeNode(pos);
    }
}
