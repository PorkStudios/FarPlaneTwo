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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg.generator;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.level.query.QuerySamplingMode;
import net.daporkchop.fp2.core.world.level.block.rough.AbstractRoughFBlockLevel;
import net.daporkchop.lib.common.annotation.param.NotNegative;

/**
 * @author DaPorkchop_
 */
public class CWGRoughFBlockLevel1_12 extends AbstractRoughFBlockLevel<CWGRoughFBlockLevelHolder1_12> {
    public CWGRoughFBlockLevel1_12(@NonNull CWGRoughFBlockLevelHolder1_12 holder) {
        super(holder);
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public boolean generationAllowed() {
        return false;
    }

    @Override
    public int getState(int x, int y, int z, @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) throws GenerationNotAllowedException {
        return 0; //TODO
    }

    @Override
    public int getBiome(int x, int y, int z, @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) throws GenerationNotAllowedException {
        return 0; //TODO
    }

    @Override
    public byte getLight(int x, int y, int z, @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) throws GenerationNotAllowedException {
        return 0; //TODO
    }
}
