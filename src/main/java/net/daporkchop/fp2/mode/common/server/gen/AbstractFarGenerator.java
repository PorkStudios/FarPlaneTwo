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

package net.daporkchop.fp2.mode.common.server.gen;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.server.gen.IFarGenerator;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.WorldServer;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractFarGenerator implements IFarGenerator {
    protected static final long SEALEVEL_OFFSET = PUnsafe.pork_getOffset(AbstractFarGenerator.class, "seaLevel");

    protected final int seaLevel; //this is final to allow JIT to hoist slow getfield opcodes out of the main loop when referenced in a loop

    public AbstractFarGenerator() {
        this.seaLevel = Integer.MIN_VALUE;
    }

    @Override
    public void init(@NonNull WorldServer world) {
        PUnsafe.putInt(this, SEALEVEL_OFFSET, world.getSeaLevel());
    }
}
