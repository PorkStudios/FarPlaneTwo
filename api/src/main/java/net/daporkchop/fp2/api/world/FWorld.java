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

package net.daporkchop.fp2.api.world;

import net.daporkchop.fp2.api.FP2;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.world.level.FLevel;

/**
 * A collection of {@link FLevel level}s, each of which is identified by its own unique {@link Identifier}.
 * <p>
 * Worlds are loaded and unloaded by the implementation as necessary. Relevant {@link net.daporkchop.fp2.api.event.generic.load load/unload events} will be sent on
 * {@link FP2#eventBus() the global event bus}.
 *
 * @author DaPorkchop_
 */
public interface FWorld extends AutoCloseable {
    /**
     * @return the implementation-specific object corresponding to this world
     */
    Object implWorld();

    /**
     * @return an event bus for events specific to this world
     */
    FEventBus eventBus();

    /**
     * Called when the world is being unloaded.
     */
    @Override
    void close();
}
