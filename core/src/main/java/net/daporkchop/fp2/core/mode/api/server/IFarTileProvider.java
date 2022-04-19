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

package net.daporkchop.fp2.core.mode.api.server;

import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarCoordLimits;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.core.mode.api.server.storage.IFarTileStorage;
import net.daporkchop.fp2.core.mode.api.server.tracking.IFarTrackerManager;
import net.daporkchop.fp2.core.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.core.server.world.IFarLevelServer;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * @author DaPorkchop_
 */
public interface IFarTileProvider<POS extends IFarPos, T extends IFarTile> extends Closeable {
    IFarLevelServer world();

    CompletableFuture<ITileHandle<POS, T>> requestLoad(@NonNull POS pos);

    CompletableFuture<ITileHandle<POS, T>> requestUpdate(@NonNull POS pos);

    IFarCoordLimits<POS> coordLimits();

    /**
     * @return the (possibly {@code null}) {@link IFarGeneratorRough} used for rough generation of far terrain
     */
    IFarGeneratorRough<POS, T> generatorRough();

    /**
     * @return the {@link IFarGeneratorExact} used for block-accurate generation of far terrain
     */
    IFarGeneratorExact<POS, T> generatorExact();

    /**
     * @return the {@link IFarScaler} used for downscaling the far terrain tiles
     */
    IFarScaler<POS, T> scaler();

    /**
     * @return the {@link IFarTrackerManager} used by this world
     */
    IFarTrackerManager<POS, T> trackerManager();

    /**
     * @return the {@link IFarTileStorage} used by this world
     */
    IFarTileStorage<POS, T> storage();

    /**
     * @return the {@link IFarRenderMode} that this world is used by
     */
    IFarRenderMode<POS, T> mode();

    /**
     * @return the current world timestamp
     */
    long currentTimestamp();

    @Override
    void close();

    /**
     * Fired to create a new {@link IFarTileProvider}.
     *
     * @author DaPorkchop_
     */
    interface CreationEvent<POS extends IFarPos, T extends IFarTile> extends IFarServerResourceCreationEvent<POS, T, IFarTileProvider<POS, T>> {
    }
}
