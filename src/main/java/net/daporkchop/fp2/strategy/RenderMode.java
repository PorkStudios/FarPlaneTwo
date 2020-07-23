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
import net.daporkchop.fp2.strategy.common.IFarPlayerTracker;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.strategy.common.IFarRenderer;
import net.daporkchop.fp2.strategy.common.IFarWorld;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPlayerTracker;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;
import net.daporkchop.fp2.strategy.heightmap.HeightmapWorld;
import net.daporkchop.fp2.strategy.heightmap.render.HeightmapRenderer;
import net.daporkchop.lib.common.misc.string.PUnsafeStrings;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Defines the different modes that may be used for rendering terrain.
 * <p>
 * Note that the enum {@link #name()} values are not the same as the enum field names. The field value names are used internally, while the name values
 * themselves are shown to users.
 *
 * @author DaPorkchop_
 */
public enum RenderMode {
    HEIGHTMAP("2D") {
        @Override
        public IFarWorld createFarWorld(@NonNull WorldServer world) {
            return new HeightmapWorld(world);
        }

        @Override
        public IFarPlayerTracker createFarTracker(@NonNull IFarWorld world) {
            return new HeightmapPlayerTracker(world);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IFarRenderer createTerrainRenderer(@NonNull WorldClient world) {
            return new HeightmapRenderer(world);
        }

        @Override
        public IFarPiece readPiece(@NonNull ByteBuf src) {
            return new HeightmapPiece(src);
        }

        @Override
        public IFarPos readPos(@NonNull ByteBuf src) {
            return new HeightmapPos(src);
        }
    },
    SURFACE("3D") {
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
        public IFarRenderer createTerrainRenderer(@NonNull WorldClient world) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        public IFarPiece readPiece(@NonNull ByteBuf src) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        public IFarPos readPos(@NonNull ByteBuf src) {
            throw new UnsupportedOperationException(); //TODO
        }
    };

    private static final RenderMode[] VALUES = values();

    /**
     * Gets a {@link RenderMode} from it's ordinal value without causing any allocations.
     *
     * @param ordinal the ordinal of the {@link RenderMode} to get
     * @return the {@link RenderMode} with the given ordinal
     */
    public static RenderMode fromOrdinal(int ordinal) {
        return VALUES[ordinal];
    }

    RenderMode(@NonNull String name) {
        PUnsafeStrings.setEnumName(this, name.intern());
    }

    public abstract IFarWorld createFarWorld(@NonNull WorldServer world);

    public abstract IFarPlayerTracker createFarTracker(@NonNull IFarWorld world);

    @SideOnly(Side.CLIENT)
    public abstract IFarRenderer createTerrainRenderer(@NonNull WorldClient world);

    public abstract IFarPiece readPiece(@NonNull ByteBuf src);

    public abstract IFarPos readPos(@NonNull ByteBuf src);
}
