/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Arrays;

/**
 * Base implementation of {@link IFastLayer}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class AbstractFastLayer implements IFastLayer {
    public static final long CHILD_OFFSET = PUnsafe.pork_getOffset(AbstractFastLayer.class, "child");

    protected final long seed;
    protected final IFastLayer child = null;

    @Getter(AccessLevel.NONE)
    protected Boolean shouldResetIntCacheAfterGet; //using Boolean so that it throws an exception if accessed while not yet initialized

    @Override
    public void init(@NonNull IFastLayer[] children) {
        if (children.length != 0) {
            PUnsafe.putObject(this, CHILD_OFFSET, children[0]);
        }
        this.shouldResetIntCacheAfterGet = Arrays.stream(children).anyMatch(IFastLayer::shouldResetIntCacheAfterGet);
    }

    @Override
    public boolean shouldResetIntCacheAfterGet() {
        return this.shouldResetIntCacheAfterGet;
    }
}
