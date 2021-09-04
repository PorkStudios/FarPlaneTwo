/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.mode.common.server;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.IFarTileCache;
import net.daporkchop.fp2.mode.api.tile.ITileHandle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link IFarTileCache} which makes use of the synchronization guarantees of {@link ConcurrentHashMap}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class HashTileCache<POS extends IFarPos, T extends IFarTile> implements IFarTileCache<POS, T> {
    protected final Map<POS, Entry<POS, T>> map = new ConcurrentHashMap<>();

    @NonNull
    protected final Function<POS, ITileHandle<POS, T>> handleFactory;

    @Override
    public ITileHandle<POS, T> retain(@NonNull POS pos) {
        class State implements BiFunction<POS, Entry<POS, T>, Entry<POS, T>> {
            ITileHandle<POS, T> handle;

            @Override
            public Entry<POS, T> apply(@NonNull POS pos, Entry<POS, T> entry) {
                if (entry == null) { //entry doesn't exist, make a new one
                    entry = new Entry<>(HashTileCache.this.handleFactory.apply(pos));
                } else { //retain reference count on existing handle
                    entry.refCnt++;
                }

                this.handle = entry.handle;
                return entry;
            }
        }

        State state = new State();
        this.map.compute(pos, state);
        return state.handle;
    }

    @Override
    public void release(@NonNull POS _pos) {
        this.map.compute(_pos, (pos, entry) -> {
            checkState(entry != null, "handle at %s doesn't exist!", pos);

            return --entry.refCnt == 0 ? null : entry;
        });
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class Entry<POS extends IFarPos, T extends IFarTile> {
        @NonNull
        protected final ITileHandle<POS, T> handle;
        protected int refCnt = 1;
    }
}
