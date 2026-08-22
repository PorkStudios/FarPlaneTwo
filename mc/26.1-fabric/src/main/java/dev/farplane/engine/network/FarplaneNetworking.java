/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2026 DaPorkchop_
 * Portions Copyright (c) 2026 FarPlane contributors
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

package dev.farplane.engine.network;

import dev.farplane.Farplane;
import dev.farplane.engine.Tile;
import dev.farplane.engine.TilePos;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric networking for FarPlane tile data synchronization.
 * <p>
 * Handles sending tile data from server to client.
 *
 * @author FarPlane contributors
 */
public class FarplaneNetworking {
    public static final Identifier TILE_DATA_ID = Identifier.fromNamespaceAndPath(Farplane.MOD_ID, "tile_data");
    public static final Identifier TILE_UNLOAD_ID = Identifier.fromNamespaceAndPath(Farplane.MOD_ID, "tile_unload");
    public static final Identifier CONFIG_SYNC_ID = Identifier.fromNamespaceAndPath(Farplane.MOD_ID, "config_sync");

    /**
     * Registers all network payloads.
     */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(
                TileDataPayload.TYPE,
                TileDataPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                TileUnloadPayload.TYPE,
                TileUnloadPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                ConfigSyncPayload.TYPE,
                ConfigSyncPayload.STREAM_CODEC
        );

        Farplane.LOGGER.info("[FarPlane] Network payloads registered");
    }

    /**
     * Sends tile data from server to a specific player.
     */
    public static void sendTileData(ServerPlayer player, TilePos pos, Tile tile) {
        // For v1, we send a simplified representation
        // Full implementation will serialize the tile properly
        ServerPlayNetworking.send(player, new TileDataPayload(pos, tile.count()));
    }

    /**
     * Sends a tile unload notification from server to a specific player.
     */
    public static void sendTileUnload(ServerPlayer player, TilePos pos) {
        ServerPlayNetworking.send(player, new TileUnloadPayload(pos));
    }

    /**
     * Payload for tile data.
     */
    public record TileDataPayload(TilePos pos, int voxelCount) implements CustomPacketPayload {
        public static final Type<TileDataPayload> TYPE = new Type<>(TILE_DATA_ID);

        public static final StreamCodec<FriendlyByteBuf, TileDataPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeInt(payload.pos().level());
                            buf.writeInt(payload.pos().x());
                            buf.writeInt(payload.pos().y());
                            buf.writeInt(payload.pos().z());
                            buf.writeInt(payload.voxelCount());
                            // TODO: serialize actual tile data
                        },
                        buf -> {
                            int level = buf.readInt();
                            int x = buf.readInt();
                            int y = buf.readInt();
                            int z = buf.readInt();
                            int voxelCount = buf.readInt();
                            return new TileDataPayload(new TilePos(level, x, y, z), voxelCount);
                        }
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Payload for tile unload.
     */
    public record TileUnloadPayload(TilePos pos) implements CustomPacketPayload {
        public static final Type<TileUnloadPayload> TYPE = new Type<>(TILE_UNLOAD_ID);

        public static final StreamCodec<FriendlyByteBuf, TileUnloadPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeInt(payload.pos().level());
                            buf.writeInt(payload.pos().x());
                            buf.writeInt(payload.pos().y());
                            buf.writeInt(payload.pos().z());
                        },
                        buf -> {
                            int level = buf.readInt();
                            int x = buf.readInt();
                            int y = buf.readInt();
                            int z = buf.readInt();
                            return new TileUnloadPayload(new TilePos(level, x, y, z));
                        }
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Payload for config synchronization.
     */
    public record ConfigSyncPayload(int maxLevels, int cutoffDistance) implements CustomPacketPayload {
        public static final Type<ConfigSyncPayload> TYPE = new Type<>(CONFIG_SYNC_ID);

        public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeInt(payload.maxLevels());
                            buf.writeInt(payload.cutoffDistance());
                        },
                        buf -> {
                            int maxLevels = buf.readInt();
                            int cutoffDistance = buf.readInt();
                            return new ConfigSyncPayload(maxLevels, cutoffDistance);
                        }
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
