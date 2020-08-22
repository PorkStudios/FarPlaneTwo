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
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.strategy.common.client.IFarRenderer;
import net.daporkchop.fp2.strategy.common.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.strategy.common.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.strategy.common.server.IFarPlayerTracker;
import net.daporkchop.fp2.strategy.common.server.scale.IFarScaler;
import net.daporkchop.fp2.strategy.common.server.IFarStorage;
import net.daporkchop.fp2.strategy.common.server.IFarWorld;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.server.HeightmapPlayerTracker;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;
import net.daporkchop.fp2.strategy.heightmap.server.HeightmapStorage;
import net.daporkchop.fp2.strategy.heightmap.server.HeightmapWorld;
import net.daporkchop.fp2.strategy.heightmap.server.gen.exact.CCHeightmapGenerator;
import net.daporkchop.fp2.strategy.heightmap.server.gen.exact.VanillaHeightmapGenerator;
import net.daporkchop.fp2.strategy.heightmap.server.gen.rough.CWGHeightmapGenerator;
import net.daporkchop.fp2.strategy.heightmap.client.HeightmapRenderer;
import net.daporkchop.fp2.strategy.heightmap.server.scale.HeightmapScalerMax;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.PriorityCollection;
import net.daporkchop.lib.common.misc.string.PUnsafeStrings;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Function;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Defines the different modes that may be used for rendering terrain.
 * <p>
 * Note that the enum {@link #name()} values are not the same as the enum field names. The field value names are used internally, while the name values
 * themselves are shown to users.
 *
 * @author DaPorkchop_
 */
@Getter
public enum RenderMode {
    HEIGHTMAP("2D", 6) {
        @Override
        protected void registerDefaultGenerators() {
            //rough
            this.generatorsRough().add(-100, world -> Constants.isCwgWorld(world) ? new CWGHeightmapGenerator() : null);

            //exact
            this.generatorsExact().add(-100, world -> Constants.isCubicWorld(world) ? new CCHeightmapGenerator() : null);
            this.generatorsExact().add(100, world -> new VanillaHeightmapGenerator());
        }

        @Override
        public IFarScaler createScaler(@NonNull WorldServer world) {
            return new HeightmapScalerMax();
        }

        @Override
        public IFarStorage createStorage(@NonNull WorldServer world) {
            return new HeightmapStorage(world);
        }

        @Override
        public IFarWorld createWorld(@NonNull WorldServer world) {
            return new HeightmapWorld(world);
        }

        @Override
        public IFarPlayerTracker createPlayerTracker(@NonNull IFarWorld world) {
            return new HeightmapPlayerTracker(uncheckedCast(world));
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IFarRenderer createRenderer(@NonNull WorldClient world) {
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

        @Override
        public IFarPiece piece(@NonNull IFarPos posIn) {
            return new HeightmapPiece((HeightmapPos) posIn);
        }

        @Override
        public IFarPiece[] pieceArray(int size) {
            return new HeightmapPiece[size];
        }
    },
    SURFACE("3D", -1) {
        @Override
        protected void registerDefaultGenerators() {
            //TODO
        }

        @Override
        public IFarScaler createScaler(@NonNull WorldServer world) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        public IFarStorage createStorage(@NonNull WorldServer world) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        public IFarWorld createWorld(@NonNull WorldServer world) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        public IFarPlayerTracker createPlayerTracker(@NonNull IFarWorld world) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IFarRenderer createRenderer(@NonNull WorldClient world) {
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

        @Override
        public IFarPiece piece(@NonNull IFarPos pos) {
            throw new UnsupportedOperationException(); //TODO
        }

        @Override
        public IFarPiece[] pieceArray(int size) {
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

    private final PriorityCollection<Function<WorldServer, IFarGeneratorRough>> generatorsRough = new PriorityCollection<>();
    private final PriorityCollection<Function<WorldServer, IFarGeneratorExact>> generatorsExact = new PriorityCollection<>();

    private final int storageVersion;

    RenderMode(@NonNull String name, int storageVersion) {
        PUnsafeStrings.setEnumName(this, name.intern());
        this.storageVersion = storageVersion;

        this.registerDefaultGenerators();
    }

    protected abstract void registerDefaultGenerators();

    /**
     * {@link #generatorsRough}, but with an unchecked generic cast
     */
    public <POS extends IFarPos, P extends IFarPiece<POS>> PriorityCollection<Function<WorldServer, IFarGeneratorRough<POS, P>>> uncheckedGeneratorsRough() {
        return uncheckedCast(this.generatorsRough);
    }

    /**
     * {@link #generatorsExact}, but with an unchecked generic cast
     */
    public <POS extends IFarPos, P extends IFarPiece<POS>> PriorityCollection<Function<WorldServer, IFarGeneratorExact<POS, P>>> uncheckedGeneratorsExact() {
        return uncheckedCast(this.generatorsExact);
    }

    /**
     * Creates a new {@link IFarScaler} for the given {@link WorldServer}.
     *
     * @param world the {@link WorldServer} to create an {@link IFarScaler} for
     * @return a new {@link IFarScaler} for the given {@link WorldServer}
     */
    public abstract IFarScaler createScaler(@NonNull WorldServer world);

    /**
     * {@link #createScaler(WorldServer)}, but with an unchecked generic cast
     */
    public <POS extends IFarPos, P extends IFarPiece<POS>> IFarScaler<POS, P> uncheckedCreateScaler(@NonNull WorldServer world) {
        return uncheckedCast(this.createScaler(world));
    }

    /**
     * Creates a new {@link IFarStorage} for the given {@link WorldServer}.
     *
     * @param world the {@link WorldServer} to create an {@link IFarStorage} for
     * @return a new {@link IFarStorage} for the given {@link WorldServer}
     */
    public abstract IFarStorage createStorage(@NonNull WorldServer world);

    /**
     * {@link #createStorage(WorldServer)}, but with an unchecked generic cast
     */
    public <POS extends IFarPos, P extends IFarPiece<POS>> IFarStorage<POS, P> uncheckedCreateStorage(@NonNull WorldServer world) {
        return uncheckedCast(this.createStorage(world));
    }

    /**
     * Creates a new {@link IFarWorld} for the given {@link WorldServer}.
     *
     * @param world the {@link WorldServer} to create an {@link IFarWorld} for
     * @return a new {@link IFarWorld} for the given {@link WorldServer}
     */
    public abstract IFarWorld createWorld(@NonNull WorldServer world);

    /**
     * Creates a new {@link IFarPlayerTracker} for the given {@link IFarWorld}.
     *
     * @param world the {@link IFarWorld} to create an {@link IFarPlayerTracker} for
     * @return a new {@link IFarPlayerTracker} for the given {@link IFarWorld}
     */
    public abstract IFarPlayerTracker createPlayerTracker(@NonNull IFarWorld world);

    /**
     * Creates a new {@link IFarRenderer} for the given {@link WorldClient}.
     *
     * @param world the {@link WorldClient} to create an {@link IFarRenderer} for
     * @return a new {@link IFarRenderer} for the given {@link WorldClient}
     */
    @SideOnly(Side.CLIENT)
    public abstract IFarRenderer createRenderer(@NonNull WorldClient world);

    /**
     * Reads a {@link IFarPiece} from its binary format.
     * <p>
     * The format is compatible with {@link IFarPiece#writePiece(ByteBuf)}.
     *
     * @param src the {@link ByteBuf} containing the encoded data
     * @return the decoded {@link IFarPiece}
     */
    public abstract IFarPiece readPiece(@NonNull ByteBuf src);

    /**
     * Reads a {@link IFarPos} from its binary format.
     * <p>
     * The format is compatible with {@link IFarPos#writePos(ByteBuf)}.
     *
     * @param src the {@link ByteBuf} containing the encoded data
     * @return the decoded {@link IFarPos}
     */
    public abstract IFarPos readPos(@NonNull ByteBuf src);

    public abstract IFarPiece piece(@NonNull IFarPos pos);

    public abstract IFarPiece[] pieceArray(int size);
}
