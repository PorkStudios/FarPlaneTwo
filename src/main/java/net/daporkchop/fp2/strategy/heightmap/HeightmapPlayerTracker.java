/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.strategy.heightmap;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.Config;
import net.daporkchop.fp2.net.server.SPacketChunkData;
import net.daporkchop.fp2.net.server.SPacketUnloadChunk;
import net.daporkchop.fp2.strategy.common.IFarPlayerTracker;
import net.daporkchop.fp2.strategy.common.IFarWorld;
import net.daporkchop.lib.common.math.BinMath;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.IdentityHashMap;
import java.util.Map;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
@Accessors(fluent = true)
public class HeightmapPlayerTracker implements IFarPlayerTracker {
    protected final HeightmapWorld world;
    protected final Map<EntityPlayerMP, LongSet> tracking = new IdentityHashMap<>();

    public HeightmapPlayerTracker(@NonNull IFarWorld world) {
        this.world = (HeightmapWorld) world;
    }

    @Override
    public void playerAdd(@NonNull EntityPlayerMP player) {
        this.tracking.put(player, new LongOpenHashSet());
    }

    @Override
    public void playerRemove(@NonNull EntityPlayerMP player) {
        this.tracking.remove(player);
    }

    @Override
    public void playerMove(@NonNull EntityPlayerMP player) {
        LongSet prev = this.tracking.get(player);
        LongSet next = new LongOpenHashSet();
        int dist = Config.renderDistance / HeightmapConstants.HEIGHT_VOXELS;
        int baseX = floorI(player.posX) / HeightmapConstants.HEIGHT_VOXELS;
        int baseZ = floorI(player.posZ) / HeightmapConstants.HEIGHT_VOXELS;

        for (int dx = -dist; dx <= dist; dx++) {
            for (int dz = -dist; dz <= dist; dz++) {
                int x = baseX + dx;
                int z = baseZ + dz;
                long l = BinMath.packXY(x, z);
                if (!prev.remove(l)) {
                    //chunk wasn't loaded before, we should load and send it
                    HeightmapChunk chunk = this.world.getChunkNowOrLoadAsync(new HeightmapChunkPos(x, z));
                    if (chunk != null) {
                        NETWORK_WRAPPER.sendTo(new SPacketChunkData().chunk(chunk), player);
                    } else {
                        continue; //don't add to next, to indicate that the chunk hasn't been sent yet
                    }
                }
                next.add(l);
            }
        }
        for (long l : prev) {
            //unload all previously loaded chunks
            //TODO: this should PROBABLY be asynchronous
            NETWORK_WRAPPER.sendTo(new SPacketUnloadChunk().pos(new HeightmapChunkPos(BinMath.unpackX(l), BinMath.unpackY(l))), player);
        }
        checkState(this.tracking.replace(player, prev, next));
    }
}
