/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.util;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.util.datastructure.java.ndimensionalintset.Int3HashSet;
import net.daporkchop.fp2.core.util.datastructure.simple.SimpleSet;
import net.daporkchop.lib.common.annotation.NotThreadSafe;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import static net.daporkchop.fp2.core.engine.EngineConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link Set} optimized specifically for {@link TilePos}.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
@NotThreadSafe
@NoArgsConstructor
public final class TilePosHashSet extends SimpleSet<TilePos> {
    private final Int3HashSet[] delegates = new Int3HashSet[MAX_LODS];

    public TilePosHashSet(Collection<? extends TilePos> c) {
        if (c instanceof TilePosHashSet) {
            Int3HashSet[] thisDelegates = this.delegates;
            Int3HashSet[] otherDelegates = ((TilePosHashSet) c).delegates;
            assert MAX_LODS == otherDelegates.length : "thisDelegates (" + MAX_LODS + ") and otherDelegates (" + otherDelegates.length + ") have mismatched lengths!";
            for (int i = 0; i < MAX_LODS; i++) {
                Int3HashSet otherDelegate = otherDelegates[i];
                if (otherDelegate != null && !otherDelegate.isEmpty()) {
                    thisDelegates[i] = new Int3HashSet(otherDelegate);
                }
            }
        } else { //fall back to regular addAll
            this.addAll(c);
        }
    }

    private Int3HashSet getOrCreateDelegate(int level) {
        Int3HashSet[] delegates = this.delegates;
        Int3HashSet delegate = delegates[level];

        if (delegate == null) { //set is unallocated, create a new one
            delegate = delegates[level] = new Int3HashSet();
        }

        return delegate;
    }

    @Override
    public int size() {
        int size = 0;
        for (Int3HashSet set : this.delegates) {
            if (set != null) {
                size += set.size();
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (Int3HashSet set : this.delegates) {
            if (set != null && !set.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clear() {
        for (Int3HashSet set : this.delegates) {
            if (set != null) {
                set.clear();
            }
        }
    }

    @Override
    public boolean add(TilePos pos) {
        return this.getOrCreateDelegate(pos.level()).add(pos.x(), pos.y(), pos.z());
    }

    @Override
    public boolean remove(Object value) {
        if (value instanceof TilePos) {
            TilePos pos = (TilePos) value;

            Int3HashSet delegate = this.delegates[pos.level()];
            return delegate != null && delegate.remove(pos.x(), pos.y(), pos.z());
        } else {
            return false;
        }
    }

    @Override
    public boolean contains(Object value) {
        if (value instanceof TilePos) {
            TilePos pos = (TilePos) value;

            Int3HashSet delegate = this.delegates[pos.level()];
            return delegate != null && delegate.contains(pos.x(), pos.y(), pos.z());
        } else {
            return false;
        }
    }

    @Override
    public void forEach(@NonNull Consumer<? super TilePos> callback) {
        Int3HashSet[] delegates = this.delegates;

        for (int level = 0; level < delegates.length; level++) {
            Int3HashSet delegate = delegates[level];
            if (delegate != null) {
                int levelButFinal = level; //damn you java
                delegate.forEach3D((x, y, z) -> callback.accept(new TilePos(levelButFinal, x, y, z)));
            }
        }
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        if (c instanceof TilePosHashSet) { //the other set is of the same type
            Int3HashSet[] thisDelegates = this.delegates;
            Int3HashSet[] otherDelegates = ((TilePosHashSet) c).delegates;
            assert MAX_LODS == otherDelegates.length : "thisDelegates (" + MAX_LODS + ") and otherDelegates (" + otherDelegates.length + ") have mismatched lengths!";

            for (int level = 0; level < MAX_LODS; level++) {
                Int3HashSet thisDelegate = thisDelegates[level];
                Int3HashSet otherDelegate = otherDelegates[level];

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
    public boolean addAll(@NonNull Collection<? extends TilePos> c) {
        if (c instanceof TilePosHashSet) { //the other set is of the same type
            Int3HashSet[] thisDelegates = this.delegates;
            Int3HashSet[] otherDelegates = ((TilePosHashSet) c).delegates;
            assert MAX_LODS == otherDelegates.length : "thisDelegates (" + MAX_LODS + ") and otherDelegates (" + otherDelegates.length + ") have mismatched lengths!";

            boolean modified = false;
            for (int level = 0; level < MAX_LODS; level++) {
                Int3HashSet thisDelegate = thisDelegates[level];
                Int3HashSet otherDelegate = otherDelegates[level];

                if (otherDelegate == null || otherDelegate.isEmpty()) { //the other set is null or empty, there's nothing to add
                    continue;
                }

                if (thisDelegate == null) { //this delegate hasn't been allocated yet, clone it
                    thisDelegates[level] = new Int3HashSet(otherDelegate);
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
        if (c instanceof TilePosHashSet) { //the other set is of the same type
            Int3HashSet[] thisDelegates = this.delegates;
            Int3HashSet[] otherDelegates = ((TilePosHashSet) c).delegates;
            assert MAX_LODS == otherDelegates.length : "thisDelegates (" + MAX_LODS + ") and otherDelegates (" + otherDelegates.length + ") have mismatched lengths!";

            boolean modified = false;
            for (int level = 0; level < MAX_LODS; level++) {
                Int3HashSet thisDelegate = thisDelegates[level];
                Int3HashSet otherDelegate = otherDelegates[level];

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

    /**
     * Adds all of the positions in the AABB defined by the given corner positions to this set.
     *
     * @param min the minimum position (inclusive)
     * @param max the maximum position (exclusive)
     * @return {@code true} if the set was modified
     * @throws IllegalArgumentException if the given tile positions are not both at the same level
     * @throws IllegalArgumentException if any of the minimum coordinates are greater than the corresponding maximum coordinates
     */
    public boolean addAllInAABB(@NonNull TilePos min, @NonNull TilePos max) {
        checkArg(min.level() == max.level(), "mismatched levels (min=%s, max=%s)", min, max);
        checkArg(min.x() <= max.x() && min.y() <= max.y() && min.z() <= max.z(), "min (%s) may not be greater than max (%s)", min, max);

        return this.getOrCreateDelegate(min.level()).addAllInRange(min.x(), min.y(), min.z(), max.x(), max.y(), max.z()) != 0;
    }
}
