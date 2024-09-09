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
 * Implementation of {@link Set} optimized specifically for {@link TilePos} at a single detail level.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
@NotThreadSafe
public final class TilePosSingleLevelHashSet extends SimpleSet<TilePos> {
    private final Int3HashSet delegate = new Int3HashSet();
    private final int level;

    public TilePosSingleLevelHashSet(int level) {
        checkArg(level >= 0 && level < MAX_LODS, "level must be in range [0,%s): %s", MAX_LODS, level);
        this.level = level;
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
        this.delegate.clear();
    }

    @Override
    public boolean add(TilePos pos) {
        checkArg(pos.level() == this.level, "cannot add tile position at level %s (only level %s is permitted)", pos.level(), this.level);
        return this.delegate.add(pos.x(), pos.y(), pos.z());
    }

    @Override
    public boolean remove(Object value) {
        if (value instanceof TilePos) {
            TilePos pos = (TilePos) value;

            return pos.level() == this.level && this.delegate.remove(pos.x(), pos.y(), pos.z());
        } else {
            return false;
        }
    }

    @Override
    public boolean contains(Object value) {
        if (value instanceof TilePos) {
            TilePos pos = (TilePos) value;

            return pos.level() == this.level && this.delegate.contains(pos.x(), pos.y(), pos.z());
        } else {
            return false;
        }
    }

    @Override
    public void forEach(@NonNull Consumer<? super TilePos> callback) {
        int level = this.level;
        this.delegate.forEach3D((x, y, z) -> callback.accept(new TilePos(level, x, y, z)));
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        if (c instanceof TilePosSingleLevelHashSet) { //the other set is of the same type
            Int3HashSet thisDelegate = this.delegate;
            Int3HashSet otherDelegate = ((TilePosSingleLevelHashSet) c).delegate;

            return thisDelegate == otherDelegate || thisDelegate.containsAll(otherDelegate);
        } else {
            return super.containsAll(c);
        }
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends TilePos> c) {
        if (c instanceof TilePosSingleLevelHashSet) { //the other set is of the same type
            Int3HashSet otherDelegate = ((TilePosSingleLevelHashSet) c).delegate;
            int otherLevel = ((TilePosSingleLevelHashSet) c).level;

            if (otherDelegate.isEmpty()) { //the other set is empty, there's nothing to add
                return false;
            }

            checkArg(otherLevel == this.level, "cannot add tile position at level %s (only level %s is permitted)", otherLevel, this.level);
            return this.delegate.addAll(otherDelegate);
        } else {
            return super.addAll(c);
        }
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        if (c instanceof TilePosSingleLevelHashSet) { //the other set is of the same type
            Int3HashSet otherDelegate = ((TilePosSingleLevelHashSet) c).delegate;
            int otherLevel = ((TilePosSingleLevelHashSet) c).level;

            return otherLevel == this.level && this.delegate.removeAll(otherDelegate);
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
     * @throws IllegalArgumentException if the given tile positions are not both at this set's level
     * @throws IllegalArgumentException if any of the minimum coordinates are greater than the corresponding maximum coordinates
     */
    public boolean addAllInAABB(@NonNull TilePos min, @NonNull TilePos max) {
        checkArg(min.level() == max.level(), "mismatched levels (min=%s, max=%s)", min, max);
        checkArg(min.level() == this.level, "cannot add tile position at level %s (only level %s is permitted)", min.level(), this.level);
        checkArg(min.x() <= max.x() && min.y() <= max.y() && min.z() <= max.z(), "min (%s) may not be greater than max (%s)", min, max);

        return this.delegate.addAllInRange(min.x(), min.y(), min.z(), max.x(), max.y(), max.z()) != 0;
    }
}
