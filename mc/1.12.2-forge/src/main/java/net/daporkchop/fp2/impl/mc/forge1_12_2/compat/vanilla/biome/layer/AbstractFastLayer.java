/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation of {@link IFastLayer}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class AbstractFastLayer implements IFastLayer {
    protected final long seed;

    /**
     * @author DaPorkchop_
     */
    public static abstract class NoParents extends AbstractFastLayer {
        public NoParents(long seed) {
            super(seed);
        }

        @Override
        public final List<IFastLayer> parents() {
            return Collections.emptyList();
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Getter
    public static abstract class SingleParent extends AbstractFastLayer {
        protected final IFastLayer parent;

        public SingleParent(long seed, @NonNull IFastLayer parent) {
            super(seed);
            this.parent = parent;
        }

        @Override
        public final List<IFastLayer> parents() {
            return Collections.singletonList(this.parent);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Getter
    public static abstract class WithRiverParent extends AbstractFastLayer {
        protected final IFastLayer parent;
        protected final IFastLayer riverParent;

        public WithRiverParent(long seed, @NonNull IFastLayer parent, @NonNull IFastLayer riverParent) {
            super(seed);
            this.parent = parent;
            this.riverParent = riverParent;
        }

        @Override
        public final List<IFastLayer> parents() {
            return Arrays.asList(this.parent, this.riverParent);
        }
    }
}
