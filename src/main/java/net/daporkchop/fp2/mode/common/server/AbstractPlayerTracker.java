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

import io.netty.util.concurrent.GlobalEventExecutor;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.server.IFarPlayerTracker;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.net.server.SPacketTileData;
import net.daporkchop.fp2.net.server.SPacketUnloadTile;
import net.daporkchop.fp2.util.threading.ServerThreadExecutor;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractPlayerTracker<POS extends IFarPos> implements IFarPlayerTracker<POS> {
    @NonNull
    protected final IFarWorld<POS, ?> world;

    protected final Map<POS, Entry> entries = new ObjObjOpenHashMap<>();
    protected final Map<EntityPlayerMP, Set<POS>> trackingPositions = new ObjObjOpenHashMap.Identity<>();

    protected final Function<POS, Entry> entryCreator = this::createEntry;
    protected final Deque<Set<POS>> setAllocator = new ArrayDeque<>();

    @Override
    public void playerAdd(@NonNull EntityPlayerMP player) {
        if (!ServerThreadExecutor.INSTANCE.isServerThread()) {
            ServerThreadExecutor.INSTANCE.execute(() -> this.playerAdd(player));
            return;
        }

        checkState(this.trackingPositions.putIfAbsent(player, this.allocateSet()) == null, player);

        FP2_LOG.debug("Added player {} to tracker in dimension {}", player.getName(), this.world.world().provider.getDimension());
    }

    @Override
    public void playerRemove(@NonNull EntityPlayerMP player) {
        if (!ServerThreadExecutor.INSTANCE.isServerThread()) {
            ServerThreadExecutor.INSTANCE.execute(() -> this.playerRemove(player));
            return;
        }

        Set<POS> trackingPositions = this.trackingPositions.remove(player);
        checkState(trackingPositions != null, player);

        //remove player from all tracking positions
        trackingPositions.stream().map(this.entries::get).forEach(entry -> entry.removePlayer(player));
        this.releaseSet(trackingPositions);

        FP2_LOG.debug("Removed player {} from tracker in dimension {}", player.getName(), this.world.world().provider.getDimension());
    }

    @Override
    public void playerMove(@NonNull EntityPlayerMP player) {
        ServerThreadExecutor.INSTANCE.checkServerThread();

        Set<POS> oldPositions = this.trackingPositions.get(player);
        checkState(oldPositions != null, player);
        Set<POS> newPositions = this.allocateSet();

        this.getPositions(player)
                .map(this::getOrCreateEntry)
                .forEach(entry -> {
                    oldPositions.remove(entry.pos);
                    newPositions.add(entry.pos);
                    entry.addPlayer(player);
                });

        oldPositions.forEach(pos -> {
            if (!newPositions.contains(pos)) {
                this.entries.get(pos).removePlayer(player);
            }
        });

        checkState(this.trackingPositions.replace(player, oldPositions, newPositions), player);
        this.releaseSet(oldPositions);
    }

    @Override
    public void tileChanged(@NonNull Compressed<POS, ?> tile) {
        if (tile.isGenerated()) {
            if (!ServerThreadExecutor.INSTANCE.isServerThread()) {
                ServerThreadExecutor.INSTANCE.execute(() -> this.tileChanged(tile));
                return;
            }

            Entry entry = this.entries.get(tile.pos());
            if (entry != null) {
                entry.tileChanged(tile);
            }
        }
    }

    @Override
    public void debug_dropAllTiles() {
        if (!ServerThreadExecutor.INSTANCE.isServerThread()) {
            ServerThreadExecutor.INSTANCE.execute(this::debug_dropAllTiles);
            return;
        }

        this.trackingPositions.forEach((player, trackingPositions) -> {
            trackingPositions.stream()
                    .map(this.entries::get)
                    .filter(Objects::nonNull)
                    .forEach(entry -> entry.removePlayer(player));

            trackingPositions.clear();
        });

        if (this.world instanceof AbstractFarWorld) {
            if (((AbstractFarWorld) this.world).notDone.isEmpty()) {
                FP2_LOG.info("Invalidating tile cache");
                ((AbstractFarWorld) this.world).tileCache.invalidateAll();
            } else {
                FP2_LOG.info("Not invalidating tile cache because some tiles are still queued");
            }
        }
    }

    protected abstract Stream<POS> getPositions(@NonNull EntityPlayerMP player);

    protected Set<POS> allocateSet() {
        return this.setAllocator.isEmpty() ? new ReferenceOpenHashSet<>() : this.setAllocator.pop();
    }

    protected void releaseSet(@NonNull Set<POS> set) {
        if (!set.isEmpty()) {
            set.clear();
        }
        this.setAllocator.push(set);
    }

    protected Entry getOrCreateEntry(@NonNull POS pos) {
        return this.entries.computeIfAbsent(pos, this.entryCreator);
    }

    protected Entry createEntry(@NonNull POS pos) {
        return new Entry(pos);
    }

    @ToString
    public class Entry {
        protected final POS pos;
        protected final Set<EntityPlayerMP> players = new ReferenceOpenHashSet<>();

        protected Compressed<POS, ?> tile;

        public Entry(@NonNull POS pos) {
            this.pos = pos;

            //attempt to get tile now
            this.tile = AbstractPlayerTracker.this.world.getTileLazy(pos);
        }

        public void addPlayer(@NonNull EntityPlayerMP player) {
            if (this.players.add(player) && this.tile != null) {
                //player was newly added and the tile has been set, send it
                //TODO: make this not be async after fixing exact generator
                GlobalEventExecutor.INSTANCE.execute(() -> NETWORK_WRAPPER.sendTo(new SPacketTileData()
                        .mode(AbstractPlayerTracker.this.world.mode()).tile(this.tile), player));
            }
        }

        public void removePlayer(@NonNull EntityPlayerMP player) {
            checkState(this.players.remove(player), player);

            if (this.players.isEmpty()) {
                //remove self from tracker
                checkState(AbstractPlayerTracker.this.entries.remove(this.pos, this), this);
            }

            NETWORK_WRAPPER.sendTo(new SPacketUnloadTile().mode(AbstractPlayerTracker.this.world.mode()).pos(this.pos), player);
        }

        public void tileChanged(@NonNull Compressed<POS, ?> tile) {
            this.tile = tile;

            //send packet to all players
            SPacketTileData packet = new SPacketTileData().mode(AbstractPlayerTracker.this.world.mode()).tile(tile);
            //TODO: make this not be async after fixing exact generator
            this.players.forEach(player -> GlobalEventExecutor.INSTANCE.execute(() -> NETWORK_WRAPPER.sendTo(packet, player)));
            //TODO: figure out what the above TODO was referring to
            //TODO: i have now figured out what it was referring to, actually fix it now
            //TODO: actually, i don't think there's any way i can reasonably fix this without breaking AsyncBlockAccess. i'll need to rework
            // how tiles are stored on the server (the issue is a deadlock when the server thread is trying to serialize a tile while a worker
            // generating said tile is waiting for the server thread to load a chunk required for generating the tile into AsyncBlockAccess)
            //this.players.forEach(player -> NETWORK_WRAPPER.sendTo(packet, player));
        }
    }
}
