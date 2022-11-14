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

package net.daporkchop.fp2.core.util.datastructure.java.ndimensionalintset;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.lib.primitive.lambda.IntIntConsumer;
import net.daporkchop.lib.primitive.lambda.IntIntIntConsumer;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Thread-safe wrapper around a {@link NDimensionalIntSet}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class SynchronizedNDimensionalIntSet implements NDimensionalIntSet {
    @NonNull
    protected final NDimensionalIntSet delegate;

    @Override
    public int dimensions() {
        return this.delegate.dimensions();
    }

    @Override
    public synchronized int size() {
        return this.delegate.size();
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
    public synchronized SynchronizedNDimensionalIntSet clone() {
        return new SynchronizedNDimensionalIntSet(this.delegate.clone());
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
    public synchronized boolean containsAll(@NonNull NDimensionalIntSet set) {
        return this.delegate.containsAll(set);
    }

    @Override
    public synchronized boolean addAll(@NonNull NDimensionalIntSet set) {
        return this.delegate.addAll(set);
    }

    @Override
    public synchronized boolean removeAll(@NonNull NDimensionalIntSet set) {
        return this.delegate.removeAll(set);
    }
}
