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

package net.daporkchop.fp2.core.server.world.level;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.storage.external.FStorageCategoryFactory;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.FWorldServer;
import net.daporkchop.fp2.api.world.level.FLevelServer;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.server.event.GetCoordinateLimitsEvent;
import net.daporkchop.fp2.core.server.event.GetExactFBlockLevelEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.world.level.AbstractLevel;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of {@link FLevelServer}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractLevelServer<F extends FP2Core, IMPL_LEVEL> extends AbstractLevel<F, IMPL_LEVEL, FWorldServer> implements FLevelServer, IFarLevelServer {
    private final FStorageCategory storageCategory;

    private final Map<IFarRenderMode<?, ?>, IFarTileProvider<?, ?>> loadedTileProviders = new IdentityHashMap<>();

    private final IntAxisAlignedBB coordLimits;

    private final ExactFBlockLevelHolder exactBlockLevelHolder;

    @SneakyThrows(FStorageException.class)
    public AbstractLevelServer(@NonNull F fp2, IMPL_LEVEL implLevel, @NonNull FWorldServer world, @NonNull Identifier id, @NonNull FGameRegistry registry) {
        super(fp2, implLevel, world, id);

        this.storageCategory = world.storageCategory().openOrCreateCategory("level_" + id, callback -> {
            //copy token to a byte[]
            byte[] newToken = registry.registryToken();
            callback.setToken(newToken);

            Optional<byte[]> existingToken = callback.getExistingToken();
            return !existingToken.isPresent() || Arrays.equals(newToken, existingToken.get())
                    ? FStorageCategoryFactory.ConfigurationResult.CREATE_IF_MISSING //category doesn't exist or has a matching storage version
                    : FStorageCategoryFactory.ConfigurationResult.DELETE_EXISTING_AND_CREATE; //category exists but has a mismatched storage version, re-create it
        });

        try {
            //determine the level's coordinate limits
            this.coordLimits = this.fp2().eventBus().fireAndGetFirst(new GetCoordinateLimitsEvent(this)).get();

            //create an exactFBlockHolder for the level
            this.exactBlockLevelHolder = this.fp2().eventBus().fireAndGetFirst(new GetExactFBlockLevelEvent(this)).get();

            //open a tile provider for each registered render mode
            IFarRenderMode.REGISTRY.forEachEntry((_name, mode) -> this.loadedTileProviders.put(mode, mode.tileProvider(this)));
        } catch (Exception e) {
            //something went wrong, try to close storage category
            try {
                this.storageCategory.close();
            } catch (Exception e1) {
                e.addSuppressed(e);
            }

            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        }
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public void close() {
        //try-with-resources to ensure that everything is closed
        try (FStorageCategory storageCategory = this.storageCategory;
             ExactFBlockLevelHolder exactBlockLevelHolder = this.exactBlockLevelHolder) {

            class State implements BiConsumer<IFarRenderMode<?, ?>, IFarTileProvider<?, ?>> {
                Throwable cause = null;

                @Override
                public void accept(IFarRenderMode<?, ?> mode, IFarTileProvider<?, ?> tileProvider) {
                    try {
                        tileProvider.close();
                    } catch (Throwable t) {
                        if (this.cause == null) {
                            this.cause = t; //this is the first exception which occurred, save it for later
                        } else {
                            this.cause.addSuppressed(t); //this is not the first exception to occur, append it to the first one
                        }
                    }
                }
            }

            //close all the tile providers, then clear the map
            State state = new State();
            this.loadedTileProviders.forEach(state);
            this.loadedTileProviders.clear();

            if (state.cause != null) { //an exception occurred while closing one of the tile providers
                PUnsafe.throwException(state.cause); //rethrow exception
                throw new AssertionError(); //impossible
            }
        }
    }

    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarTileProvider<POS, T> tileProviderFor(@NonNull IFarRenderMode<POS, T> mode) throws NoSuchElementException {
        IFarTileProvider<POS, T> tileProvider = uncheckedCast(this.loadedTileProviders.get(mode));
        if (tileProvider == null) {
            throw new NoSuchElementException("cannot get tile provider for invalid mode: " + mode);
        }
        return tileProvider;
    }

    @Override
    public void forEachTileProvider(@NonNull BiConsumer<? super IFarRenderMode<?, ?>, ? super IFarTileProvider<?, ?>> action) {
        this.loadedTileProviders.forEach(action);
    }
}
