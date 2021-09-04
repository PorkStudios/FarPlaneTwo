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

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.client.IFarTileCache;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.tile.TileSnapshot;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Default implementation of {@link IFarTileCache}.
 *
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
//TODO: this still has some race conditions - it's possible that addListener/removeListener might cause the listener to be notified twice for tiles that are
// received/unloaded during the initial notification pass
public class FarTileCache<POS extends IFarPos, T extends IFarTile> extends AbstractReleasable implements IFarTileCache<POS, T>, Function<POS, TileSnapshot<POS, T>> {
    protected final Map<POS, TileSnapshot<POS, T>> tiles = new ConcurrentHashMap<>();
    protected final Collection<Listener<POS, T>> listeners = new CopyOnWriteArraySet<>();

    @Override
    public void receiveTile(@NonNull TileSnapshot<POS, T> tile) {
        this.assertNotReleased();
        this.tiles.compute(tile.pos(), (pos, old) -> {
            if (old == null) {
                this.listeners.forEach(listener -> listener.tileAdded(tile));
            } else {
                this.listeners.forEach(listener -> listener.tileModified(tile));
            }
            return tile;
        });
    }

    @Override
    public void unloadTile(@NonNull POS _pos) {
        this.assertNotReleased();
        this.tiles.computeIfPresent(_pos, (pos, old) -> {
            this.listeners.forEach(listener -> listener.tileRemoved(pos));
            return null;
        });
    }

    @Override
    public void addListener(@NonNull Listener<POS, T> listener, boolean notifyForExisting) {
        this.assertNotReleased();
        checkState(this.listeners.add(listener), "duplicate listener: %s", listener);
        if (notifyForExisting) {
            this.tiles.forEach((pos, tile) -> listener.tileAdded(tile));
        }
    }

    @Override
    public void removeListener(@NonNull Listener<POS, T> listener, boolean notifyRemoval) {
        this.assertNotReleased();
        checkState(this.listeners.remove(listener), "unknown listener: %s", listener);
        if (notifyRemoval) {
            this.tiles.forEach((pos, tile) -> listener.tileRemoved(pos));
        }
    }

    @Override
    public TileSnapshot<POS, T> getTileCached(@NonNull POS position) {
        this.assertNotReleased();
        return this.tiles.get(position);
    }

    @Override
    public Stream<TileSnapshot<POS, T>> getTilesCached(@NonNull Stream<POS> position) {
        this.assertNotReleased();
        return position.map(this);
    }

    /**
     * @deprecated internal API, do not touch!
     */
    @Override
    @Deprecated
    public TileSnapshot<POS, T> apply(@NonNull POS pos) {
        return this.tiles.get(pos);
    }

    @Override
    protected void doRelease() {
        this.tiles.forEach((pos, tile) -> this.listeners.forEach(listener -> listener.tileRemoved(pos)));
        this.tiles.clear();
        this.listeners.clear();
    }
}
