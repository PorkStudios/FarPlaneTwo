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

package net.daporkchop.fp2.impl.mc.forge1_16.server.world;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.world.BlockWorldConstants;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.server.ServerWorld;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class ExactFBlockWorld1_16 implements FBlockWorld {
    @NonNull
    protected final ServerWorld world;
    @NonNull
    protected final FGameRegistry registry;

    @Override
    public void close() {
        //no-op
    }

    @Override
    public FGameRegistry registry() {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public int getState(int x, int y, int z) {
        //this.world.registryAccess().registryOrThrow(ForgeRegistries.BLOCKS);
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public int getBiome(int x, int y, int z) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public byte getLight(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return BlockWorldConstants.packLight(this.world.getBrightness(LightType.SKY, pos), this.world.getBrightness(LightType.BLOCK, pos));
    }
}
