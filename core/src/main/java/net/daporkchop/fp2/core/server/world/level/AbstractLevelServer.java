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

package net.daporkchop.fp2.core.server.world.level;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.storage.external.FStorageCategoryFactory;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FLevelServer;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.server.event.GetCoordinateLimitsEvent;
import net.daporkchop.fp2.core.server.event.GetExactFBlockLevelEvent;
import net.daporkchop.fp2.core.server.world.AbstractWorldServer;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.world.level.AbstractLevel;

import java.util.Arrays;
import java.util.Optional;

/**
 * Base implementation of {@link FLevelServer}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractLevelServer<F extends FP2Core,
        IMPL_WORLD, WORLD extends AbstractWorldServer<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL>,
        IMPL_LEVEL, LEVEL extends AbstractLevelServer<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL>> extends AbstractLevel<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL> implements FLevelServer, IFarLevelServer {
    private final FStorageCategory storageCategory;

    private final IFarTileProvider loadedTileProvider;

    private final IntAxisAlignedBB coordLimits;

    private final ExactFBlockLevelHolder exactBlockLevelHolder;

    public AbstractLevelServer(@NonNull F fp2, IMPL_LEVEL implLevel, @NonNull WORLD world, @NonNull Identifier id, @NonNull FGameRegistry registry) {
        super(fp2, implLevel, world, id);

        //open a storage category for this level
        this.storageCategory = this.getForInit(() -> world.storageCategory().openOrCreateCategory("level_" + id, callback -> {
            //copy token to a byte[]
            byte[] newToken = registry.registryToken();
            callback.setToken(newToken);

            Optional<byte[]> existingToken = callback.getExistingToken();
            return !existingToken.isPresent() || Arrays.equals(newToken, existingToken.get())
                    ? FStorageCategoryFactory.ConfigurationResult.CREATE_IF_MISSING //category doesn't exist or has a matching storage version
                    : FStorageCategoryFactory.ConfigurationResult.DELETE_EXISTING_AND_CREATE; //category exists but has a mismatched storage version, re-create it
        }));

        //determine the level's coordinate limits
        this.coordLimits = this.getForInit(() -> this.fp2().eventBus().fireAndGetFirst(new GetCoordinateLimitsEvent(this)).get());

        //create an exactFBlockHolder for the level
        this.exactBlockLevelHolder = this.getForInit(() -> this.fp2().eventBus().fireAndGetFirst(new GetExactFBlockLevelEvent(this)).get());

        //open a tile provider for each registered render mode
        this.loadedTileProvider = this.getForInit(() -> this.fp2().eventBus().fireAndGetFirst(new TileProviderCreationEvent(this))
                .orElseThrow(() -> new IllegalStateException("no tile provider available for world '" + this.id() + '\'')));
    }

    @Override
    protected void doClose() throws Exception {
        //try-with-resources to ensure that everything is closed
        //noinspection Convert2MethodRef,EmptyTryBlock
        try (AutoCloseable closeSuper = () -> super.doClose();
             FStorageCategory storageCategory = this.storageCategory;
             ExactFBlockLevelHolder exactBlockLevelHolder = this.exactBlockLevelHolder;
             IFarTileProvider loadedTileProvider = this.loadedTileProvider) {
        }
    }

    @Override
    public IFarTileProvider tileProvider() {
        return this.loadedTileProvider;
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    private static final class TileProviderCreationEvent implements IFarTileProvider.CreationEvent {
        @NonNull
        private final IFarLevelServer world;
    }
}
