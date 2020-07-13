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

package net.daporkchop.fp2.strategy;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPiecePos;
import net.daporkchop.fp2.strategy.common.IFarPlayerTracker;
import net.daporkchop.fp2.strategy.common.IFarWorld;
import net.daporkchop.fp2.strategy.common.TerrainRenderer;
import net.daporkchop.fp2.strategy.flat.FlatPiece;
import net.daporkchop.fp2.strategy.flat.FlatPiecePos;
import net.daporkchop.fp2.strategy.flat.FlatPlayerTracker;
import net.daporkchop.fp2.strategy.flat.FlatTerrainRenderer;
import net.daporkchop.fp2.strategy.flat.FlatWorld;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author DaPorkchop_
 */
public enum RenderStrategy {
    @Config.Comment("Renders a simple 2D heightmap of the world. Overhangs are not supported.")
    FLAT {
        @Override
        public IFarWorld createFarWorld(@NonNull WorldServer world) {
            return new FlatWorld(world);
        }

        @Override
        public IFarPlayerTracker createFarTracker(@NonNull IFarWorld world) {
            return new FlatPlayerTracker(world);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public TerrainRenderer createTerrainRenderer(@NonNull WorldClient world) {
            return new FlatTerrainRenderer(world);
        }

        @Override
        public IFarPiece readPiece(@NonNull ByteBuf src) {
            return new FlatPiece(src);
        }

        @Override
        public IFarPiecePos readPiecePos(@NonNull ByteBuf src) {
            return new FlatPiecePos(src.readInt(), src.readInt());
        }
    },
    VOLUMETRIC {
        @Override
        public IFarWorld createFarWorld(@NonNull WorldServer world) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        public IFarPlayerTracker createFarTracker(@NonNull IFarWorld world) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        @SideOnly(Side.CLIENT)
        public TerrainRenderer createTerrainRenderer(@NonNull WorldClient world) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        public IFarPiece readPiece(@NonNull ByteBuf src) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        public IFarPiecePos readPiecePos(@NonNull ByteBuf src) {
            throw new UnsupportedOperationException(); //TODO
        }
    };

    private static final RenderStrategy[] VALUES = values();

    public static RenderStrategy fromOrdinal(int ordinal) {
        return VALUES[ordinal];
    }

    public abstract IFarWorld createFarWorld(@NonNull WorldServer world);

    public abstract IFarPlayerTracker createFarTracker(@NonNull IFarWorld world);

    @SideOnly(Side.CLIENT)
    public abstract TerrainRenderer createTerrainRenderer(@NonNull WorldClient world);

    public abstract IFarPiece readPiece(@NonNull ByteBuf src);

    public abstract IFarPiecePos readPiecePos(@NonNull ByteBuf src);
}
