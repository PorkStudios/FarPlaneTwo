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

package net.daporkchop.fp2.util.datastructure.java.ndimensionalintsegtree;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.lib.primitive.lambda.IntIntConsumer;
import net.daporkchop.lib.primitive.lambda.IntIntIntConsumer;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Thread-safe wrapper around a {@link NDimensionalIntSegtreeSet}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class SynchronizedNDimensionalIntSegtreeSet implements NDimensionalIntSegtreeSet {
    @NonNull
    protected final NDimensionalIntSegtreeSet delegate;

    @Override
    public int dimensions() {
        return this.delegate.dimensions();
    }

    @Override
    public synchronized long count() {
        return this.delegate.count();
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public synchronized void clear() {
        this.delegate.clear();
    }

    @Override
    public synchronized boolean add(@NonNull int... point) {
        return this.delegate.add(point);
    }

    @Override
    public synchronized boolean remove(@NonNull int... point) {
        return this.delegate.remove(point);
    }

    @Override
    public synchronized boolean contains(@NonNull int... point) {
        return this.delegate.contains(point);
    }

    @Override
    public synchronized void forEach(@NonNull Consumer<int[]> callback) {
        this.delegate.forEach(callback);
    }

    @Override
    public synchronized boolean containsAny(@NonNull int[] a, @NonNull int[] b) {
        return this.delegate.containsAny(a, b);
    }

    @Override
    public synchronized boolean containsAny(int shift, @NonNull int... point) {
        return this.delegate.containsAny(shift, point);
    }

    @Override
    public synchronized boolean add(int x) {
        return this.delegate.add(x);
    }

    @Override
    public synchronized boolean add(int x, int y) {
        return this.delegate.add(x, y);
    }

    @Override
    public synchronized boolean add(int x, int y, int z) {
        return this.delegate.add(x, y, z);
    }

    @Override
    public synchronized boolean remove(int x) {
        return this.delegate.remove(x);
    }

    @Override
    public synchronized boolean remove(int x, int y) {
        return this.delegate.remove(x, y);
    }

    @Override
    public synchronized boolean remove(int x, int y, int z) {
        return this.delegate.remove(x, y, z);
    }

    @Override
    public synchronized boolean contains(int x) {
        return this.delegate.contains(x);
    }

    @Override
    public synchronized boolean contains(int x, int y) {
        return this.delegate.contains(x, y);
    }

    @Override
    public synchronized boolean contains(int x, int y, int z) {
        return this.delegate.contains(x, y, z);
    }

    @Override
    public synchronized void forEach1D(@NonNull IntConsumer callback) {
        this.delegate.forEach1D(callback);
    }

    @Override
    public synchronized void forEach2D(@NonNull IntIntConsumer callback) {
        this.delegate.forEach2D(callback);
    }

    @Override
    public synchronized void forEach3D(@NonNull IntIntIntConsumer callback) {
        this.delegate.forEach3D(callback);
    }

    @Override
    public synchronized boolean containsAny(int shift, int x) {
        return this.delegate.containsAny(shift, x);
    }

    @Override
    public synchronized boolean containsAny(int shift, int x, int y) {
        return this.delegate.containsAny(shift, x, y);
    }

    @Override
    public synchronized boolean containsAny(int shift, int x, int y, int z) {
        return this.delegate.containsAny(shift, x, y, z);
    }

    @Override
    public int refCnt() {
        return this.delegate.refCnt();
    }

    @Override
    public NDimensionalIntSegtreeSet retain() throws AlreadyReleasedException {
        this.delegate.retain();
        return this;
    }

    @Override
    public boolean release() throws AlreadyReleasedException {
        return this.delegate.release();
    }
}
