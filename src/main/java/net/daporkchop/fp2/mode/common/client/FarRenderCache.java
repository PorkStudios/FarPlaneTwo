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
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.daporkchop.lib.common.util.GenericMatcher;

import java.lang.reflect.Array;
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
public class FarRenderCache<POS extends IFarPos, P extends IFarPiece> {
    protected final AbstractFarRenderer<POS, P> renderer;
    protected final IFarRenderStrategy<POS, P> strategy;

    protected final FarRenderTree<POS, P> tree;
    protected final Map<POS, Compressed<POS, P>> tiles = new ConcurrentHashMap<>();

    protected final IntFunction<POS[]> posArray;
    protected final IntFunction<P[]> pieceArray;

    public FarRenderCache(@NonNull AbstractFarRenderer<POS, P> renderer) {
        this.renderer = renderer;
        this.strategy = renderer.strategy();

        Class<POS> posClass = GenericMatcher.uncheckedFind(renderer.getClass(), AbstractFarRenderer.class, "POS");
        this.posArray = size -> uncheckedCast(Array.newInstance(posClass, size));
        Class<P> pieceClass = GenericMatcher.uncheckedFind(renderer.getClass(), AbstractFarRenderer.class, "P");
        this.pieceArray = size -> uncheckedCast(Array.newInstance(pieceClass, size));

        this.tree = new FarRenderTree<>(renderer.mode(), this.strategy, renderer.maxLevel());
    }

    public void receivePiece(@NonNull Compressed<POS, P> tileIn) {
        final int maxLevel = this.renderer.maxLevel;

        this.tiles.put(tileIn.pos(), tileIn);

        this.strategy.bakeOutputs(tileIn.pos())
                .forEach(pos -> {
                    if (pos.level() < 0 || pos.level() > maxLevel) {
                        return;
                    }

                    Compressed<POS, P>[] compressedInputTiles = uncheckedCast(this.strategy.bakeInputs(pos)
                            .map(this.tiles::get)
                            .toArray(Compressed[]::new));

                    Compressed<POS, P> tile = Arrays.stream(compressedInputTiles).filter(p -> p != null && pos.equals(p.pos())).findAny().orElse(null);
                    if (tile == null) {
                        return;
                    }

                    RENDER_WORKERS.submit(pos, () -> {
                        if (this.tiles.get(pos) != tile) { //tile was modified before this task could be executed
                            return;
                        }

                        SimpleRecycler<P> recycler = this.renderer.mode().tileRecycler();
                        P[] inputTiles = this.pieceArray.apply(compressedInputTiles.length);
                        try {
                            for (int i = 0; i < inputTiles.length; i++) { //inflate tiles
                                if (compressedInputTiles[i] != null) {
                                    inputTiles[i] = compressedInputTiles[i].inflate(recycler);
                                }
                            }

                            BakeOutput output = new BakeOutput(this.strategy.renderDataSize());
                            if (this.strategy.bake(pos, inputTiles, output)) {
                                ClientThreadExecutor.INSTANCE.execute(() -> this.addPiece(tile, output));
                            } else { //baked tile contains nothing, so we remove it
                                ClientThreadExecutor.INSTANCE.execute(() -> this.addPiece(tile, null));
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

    protected void addPiece(@NonNull Compressed<POS, P> piece, BakeOutput output) {
        POS pos = piece.pos();
        if (this.tiles.get(pos) != piece) {
            return;
        }

        if (output != null) { //finish bake and insert to tree
            this.strategy.executeBakeOutput(piece.pos(), output);
            this.tree.putRenderData(pos, output);
        } else { //remove piece from tree
            this.tree.removeNode(pos);
        }
    }

    public void unloadPiece(@NonNull POS pos) {
        RENDER_WORKERS.submit(pos, () -> { //make sure that any in-progress bake tasks are finished before the piece is removed
            ClientThreadExecutor.INSTANCE.execute(() -> this.removePiece(pos));
        });
    }

    public void removePiece(@NonNull POS pos) {
        if (this.tiles.remove(pos) == null) {
            Constants.LOGGER.warn("Attempted to unload already non-existent piece at {}!", pos);
        }
        this.tree.removeNode(pos);
    }
}
