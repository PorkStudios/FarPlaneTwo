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
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FLevelServer;
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of {@link FLevelServer}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractLevelServer<F extends FP2Core, WORLD> extends AbstractLevel<F, WORLD> implements FLevelServer, IFarLevelServer {
    private final FStorageCategory storageCategory;

    private final Map<IFarRenderMode<?, ?>, IFarTileProvider<?, ?>> loadedTileProviders = new ConcurrentHashMap<>();

    private final IntAxisAlignedBB coordLimits;

    private final ExactFBlockLevelHolder exactBlockLevelHolder;

    public AbstractLevelServer(@NonNull F fp2, WORLD implWorld, @NonNull FStorageCategory storageCategory) {
        super(fp2, implWorld);

        this.storageCategory = storageCategory;

        this.coordLimits = this.fp2().eventBus().fireAndGetFirst(new GetCoordinateLimitsEvent(this)).get();
        this.exactBlockLevelHolder = this.fp2().eventBus().fireAndGetFirst(new GetExactFBlockLevelEvent(this)).get();
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public void close() {
        checkState(this.loadedTileProviders.isEmpty(), "some tile providers are still loaded: %s", this.loadedTileProviders.keySet());

        this.exactBlockLevelHolder.close();
        this.storageCategory.close();
    }

    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarTileProvider<POS, T> loadTileProvider(@NonNull IFarRenderMode<POS, T> modeIn) {
        return uncheckedCast(this.loadedTileProviders.compute(modeIn, (mode, tileProvider) -> {
            checkState(tileProvider == null, "tile provider for %s is already loaded!", mode);

            FStorageCategory category = null;
            try {
                category = this.storageCategory.openOrCreateCategory("mode_" + mode.name(), callback -> {
                    //copy token to a byte[]
                    byte[] newToken = ByteBuffer.allocate(INT_SIZE).order(ByteOrder.LITTLE_ENDIAN).putInt(mode.storageVersion()).array();
                    callback.setToken(newToken);

                    Optional<byte[]> existingToken = callback.getExistingToken();
                    return !existingToken.isPresent() || Arrays.equals(newToken, existingToken.get())
                            ? FStorageCategoryFactory.ConfigurationResult.CREATE_IF_MISSING //category doesn't exist or has a matching storage version
                            : FStorageCategoryFactory.ConfigurationResult.DELETE_EXISTING_AND_CREATE; //category exists but has a mismatched storage version, re-create it
                });

                return mode.tileProvider(this, category);
            } catch (Exception e) {
                //something went wrong, try to close the category
                try {
                    if (category != null) {
                        category.close();
                    }
                } catch (Exception e1) {
                    e.addSuppressed(e1);
                }

                PUnsafe.throwException(e); //rethrow exception
                throw new AssertionError(); //impossible
            }
        }));
    }

    @Override
    public void unloadTileProvider(@NonNull IFarRenderMode<?, ?> modeIn) throws NoSuchElementException {
        this.loadedTileProviders.compute(modeIn, (mode, tileProvider) -> {
            if (tileProvider == null) {
                throw new NoSuchElementException("tile provider isn't loaded: " + mode);
            }

            tileProvider.close();
            return null; //return null to remove the provider from the map
        });
    }

    @Override
    public <POS extends IFarPos, T extends IFarTile> IFarTileProvider<POS, T> tileProviderFor(@NonNull IFarRenderMode<POS, T> mode) throws NoSuchElementException {
        IFarTileProvider<POS, T> tileProvider = uncheckedCast(this.loadedTileProviders.get(mode));
        if (tileProvider == null) {
            throw new NoSuchElementException("tile provider isn't loaded: " + mode);
        }
        return tileProvider;
    }

    @Override
    public void forEachLoadedTileProvider(@NonNull Consumer<IFarTileProvider<?, ?>> action) {
        //iterate once, and then do a computeIfPresent to ensure we hold a lock
        /*this.loadedTileProviders.keySet().forEach(key -> this.loadedTileProviders.computeIfPresent(key, (mode, tileProvider) -> {
            action.accept(tileProvider);
            return tileProvider;
        }));*/

        this.loadedTileProviders.values().forEach(action);
    }
}
