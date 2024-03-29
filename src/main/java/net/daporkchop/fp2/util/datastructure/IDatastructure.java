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

package net.daporkchop.fp2.util.datastructure;

import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.common.util.exception.AlreadyReleasedException;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base for all datastructure interface.
 *
 * @author DaPorkchop_
 */
interface IDatastructure<I extends IDatastructure<? extends I>> extends RefCounted {
    @Override
    int refCnt();

    @Override
    I retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;

    abstract class Builder<B extends Builder<B, I>, I> {
        protected boolean threadSafe;

        /**
         * Configures whether or not a thread-safe implementation is required.
         */
        public B threadSafe(boolean threadSafe) {
            this.threadSafe = threadSafe;
            return uncheckedCast(this);
        }

        protected void validate() {
        }

        protected abstract I buildThreadSafe();

        protected abstract I buildNotThreadSafe();

        public I build() {
            this.validate();
            return this.threadSafe ? this.buildThreadSafe() : this.buildNotThreadSafe();
        }
    }
}
