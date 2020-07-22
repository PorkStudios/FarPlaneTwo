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

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.Config;
import net.daporkchop.fp2.net.server.SPacketPieceData;
import net.daporkchop.fp2.net.server.SPacketUnloadPiece;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPlayerTracker;
import net.daporkchop.fp2.strategy.common.IFarWorld;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
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
    protected final Map<EntityPlayerMP, Set<HeightmapPos>> tracking = new IdentityHashMap<>();

    public HeightmapPlayerTracker(@NonNull IFarWorld world) {
        this.world = (HeightmapWorld) world;
    }

    @Override
    public void playerAdd(@NonNull EntityPlayerMP player) {
        this.tracking.put(player, new ObjectOpenHashSet<>());
    }

    @Override
    public void playerRemove(@NonNull EntityPlayerMP player) {
        this.tracking.remove(player);
    }

    @Override
    public void playerMove(@NonNull EntityPlayerMP player) {
        int dist = Config.renderDistance >> HEIGHTMAP_SHIFT;
        int baseX = floorI(player.posX) >> HEIGHTMAP_SHIFT;
        int baseZ = floorI(player.posZ) >> HEIGHTMAP_SHIFT;

        Set<HeightmapPos> prev = this.tracking.get(player);
        Set<HeightmapPos> next = new ObjectOpenHashSet<>((dist * 2 + 2) * (dist * 2 + 2));

        int level = 3;
        for (int dx = -dist; dx <= dist; dx += 1 << level) {
            for (int dz = -dist; dz <= dist; dz += 1 << level) {
                int x = baseX + dx;
                int z = baseZ + dz;
                HeightmapPos pos = new HeightmapPos(x >> level, z >> level, level);
                if (!prev.remove(pos)) {
                    //piece wasn't loaded before, we should load and send it
                    HeightmapPiece piece = this.world.getPieceNowOrLoadAsync(pos);
                    if (piece != null) {
                        //FP2.LOGGER.info(PStrings.fastFormat("Sending piece %d,%d@%d", piece.x(), piece.z(), piece.level()));
                        NETWORK_WRAPPER.sendTo(new SPacketPieceData().piece(piece), player);
                    } else {
                        continue; //don't add to next, to indicate that the piece hasn't been sent yet
                    }
                }
                next.add(pos);
            }
        }

        for (HeightmapPos pos : prev) {//unload all previously loaded pieces
            //FP2.LOGGER.info(PStrings.fastFormat("Sending unload piece %d,%d@%d", pos.x(), pos.z(), pos.level()));
            NETWORK_WRAPPER.sendTo(new SPacketUnloadPiece().pos(pos), player);
        }
        checkState(this.tracking.replace(player, prev, next));
    }

    @Override
    public void pieceChanged(@NonNull IFarPiece pieceIn) {
        HeightmapPiece piece = (HeightmapPiece) pieceIn;
        HeightmapPos pos = piece.pos();
        this.tracking.forEach((player, curr) -> {
            if (curr.contains(pos)) {
                NETWORK_WRAPPER.sendTo(new SPacketPieceData().piece(piece), player);
            }
        });
    }
}
