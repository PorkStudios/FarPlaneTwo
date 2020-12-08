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

package net.daporkchop.fp2.mode;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.api.server.IFarPlayerTracker;
import net.daporkchop.fp2.mode.api.server.IFarWorld;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.api.server.gen.IFarScaler;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.client.HeightmapRenderer;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapPiece;
import net.daporkchop.fp2.mode.heightmap.server.HeightmapPlayerTracker;
import net.daporkchop.fp2.mode.heightmap.server.HeightmapWorld;
import net.daporkchop.fp2.mode.heightmap.server.gen.HeightmapPieceScalerMax;
import net.daporkchop.fp2.mode.heightmap.server.gen.exact.CCHeightmapGenerator;
import net.daporkchop.fp2.mode.heightmap.server.gen.exact.VanillaHeightmapGenerator;
import net.daporkchop.fp2.mode.heightmap.server.gen.rough.CWGHeightmapGenerator;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.client.VoxelRenderer;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;
import net.daporkchop.fp2.mode.voxel.server.VoxelPlayerTracker;
import net.daporkchop.fp2.mode.voxel.server.VoxelWorld;
import net.daporkchop.fp2.mode.voxel.server.gen.exact.CCVoxelGenerator;
import net.daporkchop.fp2.mode.voxel.server.gen.exact.VanillaVoxelGenerator;
import net.daporkchop.fp2.mode.voxel.server.scale.VoxelScalerIntersection;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.PriorityCollection;
import net.daporkchop.fp2.util.SimpleRecycler;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.string.PUnsafeStrings;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
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
    HEIGHTMAP(7) {
        @Override
        protected void registerDefaultGenerators() {
            //rough
            PriorityCollection<Function<WorldServer, IFarGeneratorRough<HeightmapPos, HeightmapPiece>>> generatorsRough = this.generatorsRough();
            generatorsRough.add(-100, world -> Constants.isCwgWorld(world) ? new CWGHeightmapGenerator() : null);

            //exact
            PriorityCollection<Function<WorldServer, IFarGeneratorExact<HeightmapPos, HeightmapPiece>>> generatorsExact = this.generatorsExact();
            generatorsExact.add(-100, world -> Constants.isCubicWorld(world) ? new CCHeightmapGenerator() : null);
            generatorsExact.add(100, world -> new VanillaHeightmapGenerator());
        }

        @Override
        protected SimpleRecycler<IFarPiece> pieceRecycler0() {
            return new SimpleRecycler.OfReusablePersistent<>(HeightmapPiece::new);
        }

        @Override
        public IFarScaler createScaler0() {
            return new HeightmapPieceScalerMax();
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
        public IFarPos readPos(@NonNull ByteBuf src) {
            return new HeightmapPos(src);
        }

        @Override
        public IFarPiece[] pieceArray(int size) {
            return new HeightmapPiece[size];
        }
    },
    VOXEL(4) {
        @Override
        protected void registerDefaultGenerators() {
            //rough
            PriorityCollection<Function<WorldServer, IFarGeneratorRough<VoxelPos, VoxelPiece>>> generatorsRough = this.generatorsRough();
            //generatorsRough.add(0, world -> false && Constants.isCwgWorld(world) ? new CWGVoxelGenerator() : null);

            //exact
            PriorityCollection<Function<WorldServer, IFarGeneratorExact<VoxelPos, VoxelPiece>>> generatorsExact = this.generatorsExact();
            generatorsExact.add(-100, world -> Constants.isCubicWorld(world) ? new CCVoxelGenerator() : null);
            generatorsExact.add(100, world -> new VanillaVoxelGenerator());
        }

        @Override
        protected SimpleRecycler<IFarPiece> pieceRecycler0() {
            return new SimpleRecycler.OfReusablePersistent<>(VoxelPiece::new);
        }

        @Override
        public IFarScaler createScaler0() {
            return new VoxelScalerIntersection();
        }

        @Override
        public IFarWorld createWorld(@NonNull WorldServer world) {
            return new VoxelWorld(world);
        }

        @Override
        public IFarPlayerTracker createPlayerTracker(@NonNull IFarWorld world) {
            return new VoxelPlayerTracker(uncheckedCast(world));
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IFarRenderer createRenderer(@NonNull WorldClient world) {
            return new VoxelRenderer(world);
        }

        @Override
        public IFarPos readPos(@NonNull ByteBuf src) {
            return new VoxelPos(src);
        }

        @Override
        public IFarPiece[] pieceArray(int size) {
            return new VoxelPiece[size];
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

    @Getter(AccessLevel.NONE)
    private final PriorityCollection<Function<WorldServer, IFarGeneratorRough>> generatorsRough = new PriorityCollection<>();
    @Getter(AccessLevel.NONE)
    private final PriorityCollection<Function<WorldServer, IFarGeneratorExact>> generatorsExact = new PriorityCollection<>();

    @Getter(AccessLevel.NONE)
    private final Ref<SimpleRecycler<IFarPiece>> pieceRecycler = ThreadRef.late(this::pieceRecycler0);

    private final int pieceVersion;

    RenderMode(int pieceVersion) {
        String name = PStrings.clone(this.name());
        PUnsafeStrings.titleFormat(name);
        PUnsafeStrings.setEnumName(this, name.intern());
        this.pieceVersion = pieceVersion;

        this.registerDefaultGenerators();
    }

    protected abstract void registerDefaultGenerators();

    protected abstract SimpleRecycler<IFarPiece> pieceRecycler0();

    public <P extends IFarPiece> SimpleRecycler<P> pieceRecycler() {
        return uncheckedCast(this.pieceRecycler.get());
    }

    /**
     * {@link #generatorsRough}, but with an unchecked generic cast
     */
    public <POS extends IFarPos, P extends IFarPiece> PriorityCollection<Function<WorldServer, IFarGeneratorRough<POS, P>>> generatorsRough() {
        return uncheckedCast(this.generatorsRough);
    }

    /**
     * {@link #generatorsExact}, but with an unchecked generic cast
     */
    public <POS extends IFarPos, P extends IFarPiece> PriorityCollection<Function<WorldServer, IFarGeneratorExact<POS, P>>> generatorsExact() {
        return uncheckedCast(this.generatorsExact);
    }

    /**
     * @see #createScaler()
     */
    protected abstract IFarScaler createScaler0();

    /**
     * @return a new {@link IFarScaler}
     */
    public <POS extends IFarPos, P extends IFarPiece> IFarScaler<POS, P> createScaler() {
        return uncheckedCast(this.createScaler0());
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
     * Reads a {@link IFarPos} from its binary format.
     * <p>
     * The format is compatible with {@link IFarPos#writePos(ByteBuf)}.
     *
     * @param src the {@link ByteBuf} containing the encoded data
     * @return the decoded {@link IFarPos}
     */
    public abstract IFarPos readPos(@NonNull ByteBuf src);

    public abstract IFarPiece[] pieceArray(int size);
}
