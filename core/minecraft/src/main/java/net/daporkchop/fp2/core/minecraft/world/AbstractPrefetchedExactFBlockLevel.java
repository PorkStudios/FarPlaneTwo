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
 */

package net.daporkchop.fp2.core.minecraft.world;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.level.query.QuerySamplingMode;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionFilter;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionSingleOutput;
import net.daporkchop.fp2.api.world.level.query.shape.AABBsQueryShape;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.BitSet;
import java.util.List;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractPrefetchedExactFBlockLevel<HOLDER extends AbstractExactFBlockLevelHolder<?>> implements FBlockLevel {
    private final HOLDER holder;

    //values from this.holder, hoisted into local fields to reduce pointer chasing
    private final FGameRegistry registry;
    private final IntAxisAlignedBB dataLimits;

    public AbstractPrefetchedExactFBlockLevel(@NonNull HOLDER holder) {
        this.holder = holder;

        this.registry = holder.registry();
        this.dataLimits = holder.dataLimits();
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.holder.containsAnyData(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean containsAnyData(@NonNull IntAxisAlignedBB bb) {
        return this.holder.containsAnyData(bb);
    }

    @Override
    public BitSet containsAnyData(@NonNull AABBsQueryShape query) {
        return this.holder.containsAnyData(query);
    }

    @Override
    public IntAxisAlignedBB guaranteedDataAvailableVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.holder.guaranteedDataAvailableVolume(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public IntAxisAlignedBB guaranteedDataAvailableVolume(@NonNull IntAxisAlignedBB bb) {
        return this.holder.guaranteedDataAvailableVolume(bb);
    }

    @Override
    public int getNextTypeTransitions(@NonNull Direction direction, int x, int y, int z, long maxDistance,
                                      @NonNull List<@NonNull TypeTransitionFilter> filters,
                                      @NonNull TypeTransitionSingleOutput output,
                                      @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) {
        //delegate to holder
        return this.holder.getNextTypeTransitions(direction, x, y, z, maxDistance, filters, output, sampleResolution, samplingMode, uncheckedCast(this));
    }

    //override the simple getters to remove @NonNull annotation from the sampling mode - it's not used, and since is this an internal class we don't have to worry about
    // strictly following the API rules, so we can save ourselves the effort of including the null checks

    @Override
    @SuppressWarnings("NullableProblems")
    public abstract int getState(int x, int y, int z, @NotNegative int sampleResolution, QuerySamplingMode samplingMode) throws GenerationNotAllowedException;

    @Override
    @SuppressWarnings("NullableProblems")
    public abstract int getBiome(int x, int y, int z, @NotNegative int sampleResolution, QuerySamplingMode samplingMode) throws GenerationNotAllowedException;

    @Override
    @SuppressWarnings("NullableProblems")
    public abstract byte getLight(int x, int y, int z, @NotNegative int sampleResolution, QuerySamplingMode samplingMode) throws GenerationNotAllowedException;
}
