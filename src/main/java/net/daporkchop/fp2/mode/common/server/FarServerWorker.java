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

package net.daporkchop.fp2.mode.common.server;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.compat.vanilla.IBlockHeightAccess;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.fp2.util.threading.futurecache.GenerationNotAllowedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class FarServerWorker<POS extends IFarPos, T extends IFarTile> implements Consumer<PriorityTask<POS>> {
    @NonNull
    protected final AbstractFarWorld<POS, T> world;

    @Override
    public void accept(PriorityTask<POS> task) {
        throw new UnsupportedOperationException();
    }

    //
    //
    // rough tasks
    //
    //

    public Compressed<POS, T> roughGetTile(POS pos) {
        Compressed<POS, T> compressedTile = this.world.loadTile(pos);
        try {
            if (compressedTile.isGenerated()) {
                return compressedTile;
            }

            if (!(FP2_DEBUG && FP2Config.debug.disableExactGeneration) && this.world.anyVanillaTerrainExistsAt(pos)) {
                //there's some terrain at the given position, let's try to generate something with it
                if (pos.level() == 0) {
                    //the position is at detail level 0, do exact generation
                    try {
                        return this.attemptRoughWithExact(pos, compressedTile, this.world.currentTime - 1L);
                    } catch (GenerationNotAllowedException e) {
                        //the terrain existed, but wasn't populated so we don't want to use it
                    }
                } else {
                    //force the tile to be scaled, which will cause this to be executed recursively
                    return this.roughScaleTile(pos, compressedTile);
                }
            }

            if (pos.level() == 0 || this.world.canGenerateRough(pos)) {
                //the tile can be generated using the rough generator
                return this.roughGenerateTile(pos, compressedTile);
            } else {
                //the tile cannot be generated using the rough generator
                //this will generate the tile and all tiles below it down to level 0 until the tile can be "generated" from scaled data
                return this.roughScaleTile(pos, compressedTile);
            }
        } finally {
            this.world.tileAvailable(compressedTile);
        }
    }

    public Compressed<POS, T> attemptRoughWithExact(POS pos, Compressed<POS, T> compressedTile, long newTimestamp) throws GenerationNotAllowedException {
        if (compressedTile.timestamp() >= newTimestamp) { //break out early if tile is already done or newer
            return compressedTile;
        }

        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        try {
            this.doExactPrefetchAndGenerate(pos, tile, false);

            if (compressedTile.set(newTimestamp, tile, tile.extra())) { //only notify world if the tile was changed
                this.world.tileChanged(compressedTile, false);
            }
        } finally {
            tileRecycler.release(tile);
        }

        return compressedTile;
    }

    public Compressed<POS, T> roughGenerateTile(POS pos, Compressed<POS, T> compressedTile) {
        checkArg(pos.level() == 0 || this.world.canGenerateRough(pos), "cannot do rough generation at %s!", pos);

        if (compressedTile.isGenerated()) { //break out early if tile is already done or newer
            return compressedTile;
        }

        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        try {
            long newTimestamp;
            IFarGeneratorRough<POS, T> generatorRough = this.world.generatorRough();
            if (generatorRough != null) { //generate tile using rough generator
                newTimestamp = Compressed.TIMESTAMP_GENERATED;
                generatorRough.generate(pos, tile);
            } else { //there is no rough generator - we'll have to fall back to using the exact generator
                newTimestamp = this.world.currentTime;
                this.doExactPrefetchAndGenerate(pos, tile, true);
            }

            if (compressedTile.set(newTimestamp, tile, tile.extra())) { //only notify world if the tile was changed
                this.world.tileChanged(compressedTile, false);
            }
        } catch (GenerationNotAllowedException e) { //impossible
            throw new IllegalStateException(e);
        } finally {
            tileRecycler.release(tile);
        }

        return compressedTile;
    }

    public Compressed<POS, T> roughScaleTile(POS pos, Compressed<POS, T> compressedTile) {
        if (compressedTile.isGenerated()) { //break out early if tile is already done or newer
            return compressedTile;
        }

        return this.scaleTile0(pos, compressedTile, Compressed.TIMESTAMP_GENERATED, false);
    }

    //
    //
    // update tasks
    //
    //

    public void updateTile(POS pos) {
        long newTimestamp = this.world.storage.dirtyTracker().dirtyTimestamp(pos);
        if (newTimestamp < 0L) {
            FP2_LOG.warn("Duplicate update task scheduled for tile at {}!", pos);
            return;
        }

        try {
            if (FP2_DEBUG && FP2Config.debug.disableExactGeneration) { //updates will always use the exact generator, so don't use them
                return;
            }

            this.updateTile(pos, newTimestamp);
        } finally {
            //remove tile from tracker again
            this.world.storage.dirtyTracker().clearDirty(pos, newTimestamp);
        }
    }

    public void updateTile(POS pos, long newTimestamp) {
        this.world.loader.doWith(Collections.singletonList(pos), compressedSrcs -> {
            Compressed<POS, T> compressedTile = compressedSrcs.get(0);
            if (compressedTile.timestamp() >= newTimestamp) { //break out early if tile is already done or newer
                return;
            }

            if (pos.level() == 0) {
                this.updateTileExact(pos, compressedTile, newTimestamp);
            } else {
                this.updateTileScale(pos, compressedTile, newTimestamp);
            }
        });
    }

    protected void updateTileExact(POS pos, Compressed<POS, T> compressedTile, long newTimestamp) {
        SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
        T tile = tileRecycler.allocate();
        try {
            this.doExactPrefetchAndGenerate(pos, tile, false);

            if (compressedTile.set(newTimestamp, tile, tile.extra())) { //only notify world if the tile was changed
                this.world.tileChanged(compressedTile, true);
            }
        } catch (GenerationNotAllowedException e) {
            //silently discard
        } finally {
            tileRecycler.release(tile);
        }
    }

    protected void doExactPrefetchAndGenerate(POS pos, T tile, boolean generationAllowed) throws GenerationNotAllowedException {
        //prefetch terrain
        IBlockHeightAccess access = generationAllowed
                ? this.world.blockAccess().prefetch(this.world.generatorExact().neededColumns(pos), world -> this.world.generatorExact().neededCubes(world, pos))
                : this.world.blockAccess().prefetchWithoutGenerating(this.world.generatorExact().neededColumns(pos), world -> this.world.generatorExact().neededCubes(world, pos));

        //generate tile
        this.world.generatorExact().generate(access, pos, tile);
    }

    protected void updateTileScale(POS pos, Compressed<POS, T> compressedTile, long newTimestamp) {
        this.scaleTile0(pos, compressedTile, newTimestamp, true);
    }

    //
    //
    // helpers
    //
    //

    public Compressed<POS, T> scaleTile0(POS pos, Compressed<POS, T> compressedTile, long newTimestamp, boolean allowScale) {
        //generate scale inputs
        this.world.loader.doWith(this.world.scaler().inputs(pos).collect(Collectors.toList()), compressedSrcs -> {
            //inflate sources
            SimpleRecycler<T> tileRecycler = this.world.mode().tileRecycler();
            T[] srcs = this.world.mode().tileArray(compressedSrcs.size());
            for (int i = 0; i < compressedSrcs.size(); i++) {
                srcs[i] = compressedSrcs.get(i).inflateValue(tileRecycler);
            }

            T dst = tileRecycler.allocate();
            try {
                //actually do scaling
                long extra = this.world.scaler().scale(srcs, dst);
                if (compressedTile.set(newTimestamp, dst, extra)) {
                    this.world.tileChanged(compressedTile, allowScale);
                }
            } finally {
                tileRecycler.release(dst);
                for (T src : srcs) {
                    if (src != null) {
                        tileRecycler.release(src);
                    }
                }
            }
        });

        return compressedTile;
    }
}
