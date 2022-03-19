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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.exactfblockworld;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.BlockWorldConstants;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry.GameRegistry1_12_2;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class PrefetchedCubesCCFBlockWorld1_12 implements FBlockWorld, IBlockAccess {
    public static PrefetchedCubesCCFBlockWorld1_12 prefetchCubes(@NonNull CCExactFBlockWorldHolder1_12 holder, boolean generationAllowed, @NonNull NDimensionalIntSet cubePositions) throws GenerationNotAllowedException {
        //collect all positions into a list
        List<CubePos> cubePositionsList = new ArrayList<>(toInt(cubePositions.count()));
        cubePositions.forEach3D((x, y, z) -> cubePositionsList.add(new CubePos(x, y, z)));

        return prefetchCubes(holder, generationAllowed, cubePositionsList);
    }

    public static PrefetchedCubesCCFBlockWorld1_12 prefetchCubes(@NonNull CCExactFBlockWorldHolder1_12 holder, boolean generationAllowed, @NonNull List<CubePos> cubePositions) throws GenerationNotAllowedException {
        return new PrefetchedCubesCCFBlockWorld1_12(holder, generationAllowed, holder.multiGetCubes(cubePositions.stream(), generationAllowed));
    }

    protected final CCExactFBlockWorldHolder1_12 holder;
    protected final WorldServer world;
    protected final boolean generationAllowed;

    protected final XYZMap<ICube> cubes = new XYZMap<>(0.75f, 16);

    public PrefetchedCubesCCFBlockWorld1_12(@NonNull CCExactFBlockWorldHolder1_12 holder, boolean generationAllowed, @NonNull Stream<ICube> cubes) {
        this.holder = holder;
        this.world = holder.world;
        this.generationAllowed = generationAllowed;

        cubes.forEach(cube -> {
            checkArg(this.cubes.put(cube) == null, "duplicate cube at (%d,%d,%d)", cube.getX(), cube.getY(), cube.getZ());
        });
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public FGameRegistry registry() {
        return GameRegistry1_12_2.get();
    }

    @Override
    public IntAxisAlignedBB dataLimits() {
        return this.holder.farWorld.fp2_IFarWorld_coordLimits();
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.holder.containsAnyData(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public int getState(int x, int y, int z) throws GenerationNotAllowedException {
        ICube cube = this.cubes.get(Coords.blockToCube(x), Coords.blockToCube(y), Coords.blockToCube(z));
        assert cube != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        return GameRegistry1_12_2.get().state2id(cube.getBlockState(x, y, z).getActualState(this, new BlockPos(x, y, z)));
    }

    @Override
    public int getBiome(int x, int y, int z) throws GenerationNotAllowedException {
        ICube cube = this.cubes.get(Coords.blockToCube(x), Coords.blockToCube(y), Coords.blockToCube(z));
        assert cube != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        return GameRegistry1_12_2.get().biome2id(cube.getBiome(new BlockPos(x, y, z)));
    }

    @Override
    public byte getLight(int x, int y, int z) throws GenerationNotAllowedException {
        ICube cube = this.cubes.get(Coords.blockToCube(x), Coords.blockToCube(y), Coords.blockToCube(z));
        assert cube != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        BlockPos pos = new BlockPos(x, y, z);
        return this.world.isValid(pos)
                ? BlockWorldConstants.packLight(cube.getLightFor(EnumSkyBlock.SKY, pos), cube.getLightFor(EnumSkyBlock.BLOCK, pos))
                : BlockWorldConstants.packLight(15, 0); //World#getLightFor returns the type's default light level if the position isn't valid
    }

    // IBlockAccess

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return null; //there are no tile entities!
    }

    @Override
    @SneakyThrows(GenerationNotAllowedException.class)
    public IBlockState getBlockState(BlockPos pos) {
        if (this.world.isOutsideBuildHeight(pos)) {
            return Blocks.AIR.getDefaultState();
        } else {
            ICube cube = this.cubes.get(Coords.blockToCube(pos.getX()), Coords.blockToCube(pos.getY()), Coords.blockToCube(pos.getZ()));

            //this is gross, i'd rather have it throw an exception. unfortunately, Block#getActualBlockState may have to access the state of a neighboring block, which may
            //  not have been prefetched. however, since we NEED to know the real block state at the position, we're forced to load the cube...
            if (cube == null) {
                //this instance doesn't have the cube prefetched, try to retrieve it from the holder...
                cube = this.holder.getCube(Coords.blockToCube(pos.getX()), Coords.blockToCube(pos.getY()), Coords.blockToCube(pos.getZ()), this.generationAllowed);

                //don't bother saving the loaded chunk into the cache:
                //- we don't want to modify this instance's state, in order to avoid causing future regular block accesses to be succeed when the would otherwise have failed
                //- this is very much an edge case which doesn't necessarily need to be fast
            }

            return cube.getBlockState(pos);
        }
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState state = this.getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return this.getBlockState(pos).getStrongPower(this, pos, direction);
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        if (!this.world.isValid(pos)) {
            return _default;
        }

        return this.getBlockState(pos).isSideSolid(this, pos, side);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        throw new UnsupportedOperationException(); //this is a client-only method, i don't need to implement it
    }

    @SideOnly(Side.CLIENT)
    @Override
    public WorldType getWorldType() {
        throw new UnsupportedOperationException(); //this is a client-only method, i don't need to implement it
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Biome getBiome(BlockPos pos) {
        throw new UnsupportedOperationException(); //this is a client-only method, i don't need to implement it
    }
}
