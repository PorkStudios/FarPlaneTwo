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
 */

package net.daporkchop.fp2.core.util.datastructure.java.ndimensionalintsegtree;

import lombok.NonNull;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.lib.common.function.exception.ERunnable;
import net.daporkchop.lib.common.function.exception.ESupplier;
import net.daporkchop.lib.primitive.lambda.IntIntConsumer;
import net.daporkchop.lib.primitive.lambda.IntIntIntConsumer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

/**
 * A {@link NDimensionalIntSegtreeSet} which is asynchronously pre-initialized with values from a {@link Stream} given at construction time.
 *
 * @author DaPorkchop_
 */
public class AsyncInitializedNDimensionalIntSegtreeSet implements NDimensionalIntSegtreeSet {
    protected final NDimensionalIntSegtreeSet delegate;
    protected CompletableFuture<Void> initFuture;

    public AsyncInitializedNDimensionalIntSegtreeSet(@NonNull NDimensionalIntSegtreeSet delegate, @NonNull ESupplier<Stream<int[]>> initialPoints) {
        this.delegate = delegate;

        this.initFuture = CompletableFuture.runAsync((ERunnable) () -> {
            try (Stream<int[]> stream = initialPoints.get()) { //add all points to delegate set
                stream.forEach(delegate::add);
            }

            this.initFuture = null; //if everything was successful, set initFuture to null to allow it to be GC'd
        });
    }

    protected void handleWrite() {
        if (this.initFuture != null && this.initFuture.isDone()) {
            this.initFuture.join(); //rethrow exception if one was thrown
        }
    }

    protected void handleRead() {
        if (this.initFuture != null) {
            this.initFuture.join(); //block until initialization is complete, or throw exception
        }
    }

    //
    // NDimensionalIntSet methods
    //

    @Override
    public int dimensions() {
        return this.delegate.dimensions();
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public void clear() {
        this.handleRead();
        this.delegate.clear();
    }

    @Override
    public NDimensionalIntSegtreeSet clone() {
        this.handleRead(); //block until future is complete
        return this.delegate.clone(); //clone delegate, we don't need to wrap it in an AsyncInitializedNDimensionalIntSegtreeSet because we only serve as a proxy once initialization is complete
    }

    @Override
    public boolean add(@NonNull int... point) {
        this.handleWrite();
        return this.delegate.add(point);
    }

    @Override
    public boolean remove(@NonNull int... point) {
        this.handleWrite();
        return this.delegate.remove(point);
    }

    @Override
    public boolean contains(@NonNull int... point) {
        this.handleRead();
        return this.delegate.contains(point);
    }

    @Override
    public void forEach(@NonNull Consumer<int[]> callback) {
        this.handleRead();
        this.delegate.forEach(callback);
    }

    @Override
    public boolean add(int x) {
        this.handleWrite();
        return this.delegate.add(x);
    }

    @Override
    public boolean add(int x, int y) {
        this.handleWrite();
        return this.delegate.add(x, y);
    }

    @Override
    public boolean add(int x, int y, int z) {
        this.handleWrite();
        return this.delegate.add(x, y, z);
    }

    @Override
    public boolean remove(int x) {
        this.handleWrite();
        return this.delegate.remove(x);
    }

    @Override
    public boolean remove(int x, int y) {
        this.handleWrite();
        return this.delegate.remove(x, y);
    }

    @Override
    public boolean remove(int x, int y, int z) {
        this.handleWrite();
        return this.delegate.remove(x, y, z);
    }

    @Override
    public boolean contains(int x) {
        this.handleRead();
        return this.delegate.contains(x);
    }

    @Override
    public boolean contains(int x, int y) {
        this.handleRead();
        return this.delegate.contains(x, y);
    }

    @Override
    public boolean contains(int x, int y, int z) {
        this.handleRead();
        return this.delegate.contains(x, y, z);
    }

    @Override
    public void forEach1D(@NonNull IntConsumer callback) {
        this.handleRead();
        this.delegate.forEach1D(callback);
    }

    @Override
    public void forEach2D(@NonNull IntIntConsumer callback) {
        this.handleRead();
        this.delegate.forEach2D(callback);
    }

    @Override
    public void forEach3D(@NonNull IntIntIntConsumer callback) {
        this.handleRead();
        this.delegate.forEach3D(callback);
    }

    @Override
    public boolean containsAll(@NonNull NDimensionalIntSet set) {
        this.handleRead();
        return this.delegate.containsAll(set);
    }

    @Override
    public boolean addAll(@NonNull NDimensionalIntSet set) {
        this.handleWrite();
        return this.delegate.addAll(set);
    }

    @Override
    public boolean removeAll(@NonNull NDimensionalIntSet set) {
        this.handleRead(); //this will block until initialization is complete, which is good because otherwise we might be removing points that don't exist yet but will soon
        return this.delegate.removeAll(set);
    }

    //
    // NDimensionalIntSegtreeSet methods
    //

    @Override
    public boolean containsAny(@NonNull int[] a, @NonNull int[] b) {
        this.handleRead();
        return this.delegate.containsAny(a, b);
    }

    @Override
    public boolean containsAny(int x0, int x1) {
        this.handleRead();
        return this.delegate.containsAny(x0, x1);
    }

    @Override
    public boolean containsAny(int x0, int y0, int x1, int y1) {
        this.handleRead();
        return this.delegate.containsAny(x0, y0, x1, y1);
    }

    @Override
    public boolean containsAny(int x0, int y0, int z0, int x1, int y1, int z1) {
        this.handleRead();
        return this.delegate.containsAny(x0, y0, z0, x1, y1, z1);
    }
}
