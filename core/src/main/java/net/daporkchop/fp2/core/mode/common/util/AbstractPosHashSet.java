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

package net.daporkchop.fp2.core.mode.common.util;

import lombok.NonNull;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.fp2.core.util.datastructure.simple.SimpleSet;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Base class for implementations of {@link Set} optimized specifically for a specific {@link IFarPos} type.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractPosHashSet<POS extends IFarPos> extends SimpleSet<POS> {
    protected final NDimensionalIntSet[] delegates;

    public AbstractPosHashSet(int dimensions, int maxLevels) {
        NDimensionalIntSet.Builder builder = Datastructures.INSTANCE.nDimensionalIntSet()
                .dimensions(dimensions).threadSafe(false);

        this.delegates = new NDimensionalIntSet[maxLevels];
        for (int level = 0; level < maxLevels; level++) {
            this.delegates[level] = builder.build();
        }
    }

    @Override
    public int size() {
        return Stream.of(this.delegates).mapToInt(NDimensionalIntSet::size).sum();
    }

    @Override
    public boolean isEmpty() {
        return Stream.of(this.delegates).allMatch(NDimensionalIntSet::isEmpty);
    }

    @Override
    public void clear() {
        Stream.of(this.delegates).forEach(NDimensionalIntSet::clear);
    }

    @Override
    public abstract boolean add(POS value);

    @Override
    public abstract boolean remove(Object value);

    @Override
    public abstract boolean contains(Object value);

    @Override
    public abstract void forEach(@NonNull Consumer<? super POS> callback);

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        if (this.getClass() == c.getClass()) { //the other set is of the same type
            NDimensionalIntSet[] thisDelegates = this.delegates;
            NDimensionalIntSet[] otherDelegates = ((AbstractPosHashSet) c).delegates;

            for (int level = 0; level < thisDelegates.length; level++) {
                if (!thisDelegates[level].containsAll(otherDelegates[level])) {
                    return false;
                }
            }
            return true;
        } else {
            return super.containsAll(c);
        }
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends POS> c) {
        if (this.getClass() == c.getClass()) { //the other set is of the same type
            NDimensionalIntSet[] thisDelegates = this.delegates;
            NDimensionalIntSet[] otherDelegates = ((AbstractPosHashSet) c).delegates;

            boolean modified = false;
            for (int level = 0; level < thisDelegates.length; level++) {
                if (thisDelegates[level].addAll(otherDelegates[level])) {
                    modified = true;
                }
            }
            return modified;
        } else {
            return super.addAll(c);
        }
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        if (this.getClass() == c.getClass()) { //the other set is of the same type
            NDimensionalIntSet[] thisDelegates = this.delegates;
            NDimensionalIntSet[] otherDelegates = ((AbstractPosHashSet) c).delegates;

            boolean modified = false;
            for (int level = 0; level < thisDelegates.length; level++) {
                if (thisDelegates[level].removeAll(otherDelegates[level])) {
                    modified = true;
                }
            }
            return modified;
        } else {
            return super.removeAll(c);
        }
    }
}
