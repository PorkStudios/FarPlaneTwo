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

package net.daporkchop.fp2.core.mode.common.util;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.fp2.core.util.datastructure.simple.SimpleSet;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Base class for implementations of {@link Set} optimized specifically for a specific tile position type.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractPosHashSet<POS, SET extends NDimensionalIntSet> extends SimpleSet<POS> {
    @NonNull
    protected final SET[] delegates;

    public AbstractPosHashSet(@NonNull SET[] delegates, Collection<? extends POS> c) {
        this.delegates = delegates;

        if (this.getClass() == c.getClass()) { //the other set is of the same type
            SET[] thisDelegates = this.delegates;
            SET[] otherDelegates = PorkUtil.<AbstractPosHashSet<POS, SET>>uncheckedCast(c).delegates;
            assert thisDelegates.length == otherDelegates.length : "thisDelegates (" + thisDelegates.length + ") and otherDelegates (" + otherDelegates.length + ") have mismatched lengths!";

            //clone the source set at each level
            for (int level = 0; level < thisDelegates.length; level++) {
                SET otherDelegate = otherDelegates[level];
                if (otherDelegate != null && !otherDelegate.isEmpty()) { //the source set has been allocated and is non-empty, clone it
                    thisDelegates[level] = this.cloneSet(otherDelegate);
                }
            }
        } else { //fall back to regular addAll
            this.addAll(c);
        }
    }

    protected abstract SET createSet();

    protected abstract SET cloneSet(SET src);

    protected SET getOrCreateDelegate(int level) {
        SET[] delegates = this.delegates;
        SET delegate = delegates[level];

        if (delegate == null) { //set is unallocated, create a new one
            delegate = delegates[level] = this.createSet();
        }

        return delegate;
    }

    @Override
    public int size() {
        int size = 0;
        for (SET set : this.delegates) {
            if (set != null) {
                size += set.size();
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (SET set : this.delegates) {
            if (set != null && !set.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clear() {
        for (SET set : this.delegates) {
            if (set != null) {
                set.clear();
            }
        }
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
            SET[] thisDelegates = this.delegates;
            SET[] otherDelegates = PorkUtil.<AbstractPosHashSet<POS, SET>>uncheckedCast(c).delegates;
            assert thisDelegates.length == otherDelegates.length : "thisDelegates (" + thisDelegates.length + ") and otherDelegates (" + otherDelegates.length + ") have mismatched lengths!";

            for (int level = 0; level < thisDelegates.length; level++) {
                SET thisDelegate = thisDelegates[level];
                SET otherDelegate = otherDelegates[level];

                if (thisDelegate != otherDelegate //the sets can only be identity equal if they are both null, so we can now assume that at least one of them is non-null
                    && otherDelegate != null //if the other set is null, our set is guaranteed to contain every point
                    && (thisDelegate == null
                        ? !otherDelegate.isEmpty() //if our set is null it's implicitly empty, so if the other set is non-empty we're missing all of the points
                        : !thisDelegate.containsAll(otherDelegate))) { //neither set is null, delegate to containsAll on the actual sets
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
            SET[] thisDelegates = this.delegates;
            SET[] otherDelegates = PorkUtil.<AbstractPosHashSet<POS, SET>>uncheckedCast(c).delegates;
            assert thisDelegates.length == otherDelegates.length : "thisDelegates (" + thisDelegates.length + ") and otherDelegates (" + otherDelegates.length + ") have mismatched lengths!";

            boolean modified = false;
            for (int level = 0; level < thisDelegates.length; level++) {
                SET thisDelegate = thisDelegates[level];
                SET otherDelegate = otherDelegates[level];

                if (otherDelegate == null || otherDelegate.isEmpty()) { //the other set is null or empty, there's nothing to add
                    continue;
                }

                if (thisDelegate == null) { //this delegate hasn't been allocated yet, clone it
                    thisDelegates[level] = this.cloneSet(otherDelegate);
                    modified = true;
                } else if (thisDelegate.addAll(otherDelegate)) { //neither set is null, delegate to addAll on the actual sets
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
            SET[] thisDelegates = this.delegates;
            SET[] otherDelegates = PorkUtil.<AbstractPosHashSet<POS, SET>>uncheckedCast(c).delegates;
            assert thisDelegates.length == otherDelegates.length : "thisDelegates (" + thisDelegates.length + ") and otherDelegates (" + otherDelegates.length + ") have mismatched lengths!";

            boolean modified = false;
            for (int level = 0; level < thisDelegates.length; level++) {
                SET thisDelegate = thisDelegates[level];
                SET otherDelegate = otherDelegates[level];

                if (otherDelegate == null || otherDelegate.isEmpty() //the other set is null or empty, there's nothing to remove
                    || thisDelegate == null || thisDelegate.isEmpty()) { //this set is null or empty, there's nothing to be removed
                    continue;
                }

                if (thisDelegate.removeAll(otherDelegate)) { //neither set is null, delegate to removeAll on the actual sets
                    modified = true;
                }
            }
            return modified;
        } else {
            return super.removeAll(c);
        }
    }
}
