/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.common;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Base implementation of {@link IFarRenderMode}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractFarRenderMode implements IFarRenderMode {
    protected final Cached<Recycler<Tile>> recyclerRef = Cached.threadLocal(() -> Recycler.unbounded(this::newTile, Tile::reset), ReferenceStrength.SOFT);

    @Getter
    protected final int storageVersion;
    @Getter
    protected final int maxLevels;
    @Getter
    protected final int tileShift;

    protected abstract AbstractExactGeneratorCreationEvent exactGeneratorCreationEvent(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider);

    protected abstract AbstractRoughGeneratorCreationEvent roughGeneratorCreationEvent(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider);

    protected abstract Tile newTile();

    @Override
    public IFarGeneratorExact exactGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider) {
        return fp2().eventBus().fireAndGetFirst(this.exactGeneratorCreationEvent(world, provider))
                .orElseThrow(() -> new IllegalStateException(PStrings.fastFormat(
                        "no exact generator available for world '%s', mode:%s",
                        world.id(),
                        this.name()
                )));
    }

    @Override
    public IFarGeneratorRough roughGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider) {
        return fp2().eventBus().fireAndGetFirst(this.roughGeneratorCreationEvent(world, provider)).orElse(null);
    }

    protected abstract AbstractTileProviderCreationEvent tileProviderCreationEvent(@NonNull IFarLevelServer world);

    @Override
    public IFarTileProvider tileProvider(@NonNull IFarLevelServer world) {
        return fp2().eventBus().fireAndGetFirst(this.tileProviderCreationEvent(world))
                .orElseThrow(() -> new IllegalStateException(PStrings.fastFormat(
                        "No tile provider available for world '%s', mode:%s",
                        world.id(),
                        this.name()
                )));
    }

    @Override
    public Recycler<Tile> tileRecycler() {
        return this.recyclerRef.get();
    }

    @Override
    public String toString() {
        return this.name();
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    protected abstract class AbstractExactGeneratorCreationEvent implements IFarGeneratorExact.CreationEvent {
        @NonNull
        protected final IFarLevelServer world;
        @NonNull
        protected final IFarTileProvider provider;

        @Override
        public IFarRenderMode mode() {
            return AbstractFarRenderMode.this;
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    protected abstract class AbstractRoughGeneratorCreationEvent implements IFarGeneratorRough.CreationEvent {
        @NonNull
        protected final IFarLevelServer world;
        @NonNull
        protected final IFarTileProvider provider;

        @Override
        public IFarRenderMode mode() {
            return AbstractFarRenderMode.this;
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    protected abstract class AbstractTileProviderCreationEvent implements IFarTileProvider.CreationEvent {
        @NonNull
        protected final IFarLevelServer world;

        @Override
        public IFarRenderMode mode() {
            return AbstractFarRenderMode.this;
        }
    }
}
