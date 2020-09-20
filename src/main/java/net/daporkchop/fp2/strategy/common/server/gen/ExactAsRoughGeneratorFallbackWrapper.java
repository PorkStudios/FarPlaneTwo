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

package net.daporkchop.fp2.strategy.common.server.gen;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.util.compat.vanilla.IBlockHeightAccess;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.minecraft.world.WorldServer;

/**
 * Implementation of {@link IFarGeneratorRough} which wraps a {@link IFarGeneratorExact}. This allows worlds with no available rough generator to
 * fall back to an exact generator.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class ExactAsRoughGeneratorFallbackWrapper<POS extends IFarPos, P extends IFarPiece<POS>> implements IFarGeneratorRough<POS, P> {
    @NonNull
    protected final AsyncBlockAccess blockAccess;
    @NonNull
    protected final IFarGeneratorExact<POS, P> exactGenerator;

    @Override
    public void init(@NonNull WorldServer world) {
        //no-op
    }

    @Override
    public void generate(@NonNull P piece) {
        try {
            IBlockHeightAccess prefetched = this.blockAccess.prefetchAsync(this.exactGenerator.neededColumns(piece.pos()),
                    world -> this.exactGenerator.neededCubes(world, piece.pos()))
                    .sync().getNow();
            this.exactGenerator.generate(prefetched, piece);
        } catch (InterruptedException e)    {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean supportsLowResolution() {
        return false;
    }

    @Override
    public boolean isLowResolutionInaccurate() {
        return false;
    }
}
