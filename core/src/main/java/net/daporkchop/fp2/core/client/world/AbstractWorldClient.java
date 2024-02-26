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

package net.daporkchop.fp2.core.client.world;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.FWorldClient;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.world.level.AbstractLevelClient;
import net.daporkchop.fp2.core.world.AbstractWorld;
import net.daporkchop.lib.common.misc.threadlocal.TL;

/**
 * Base implementation of {@link FWorldClient}.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractWorldClient<F extends FP2Core,
        IMPL_WORLD, WORLD extends AbstractWorldClient<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL>,
        IMPL_LEVEL, LEVEL extends AbstractLevelClient<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL>> extends AbstractWorld<F, IMPL_WORLD, WORLD, IMPL_LEVEL, LEVEL> implements FWorldClient {
    public static final TL<IntAxisAlignedBB> COORD_LIMITS_HACK = TL.create(); //TODO: this is disgusting

    public AbstractWorldClient(@NonNull F fp2, IMPL_WORLD implWorld) {
        super(fp2, implWorld);
    }
}