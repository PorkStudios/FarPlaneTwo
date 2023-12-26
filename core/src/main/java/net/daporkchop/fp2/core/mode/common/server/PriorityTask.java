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

package net.daporkchop.fp2.core.mode.common.server;

import lombok.NonNull;
import net.daporkchop.fp2.core.engine.TilePos;

import java.util.Comparator;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * A combination of a {@link TaskStage} and a {@link TilePos}, representing a request to do some operation at a given tile position.
 *
 * @author DaPorkchop_
 */
public interface PriorityTask {
    /**
     * @deprecated internal API, do not touch!
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    Comparator<PriorityTask> _APPROX_COMPARATOR = (a, b) -> {
        int d;
        if ((d = a.stage().compareTo(b.stage())) == 0) {
            d = Integer.compare(a.pos().level(), b.pos().level());
        }
        return d;
    };

    /**
     * @return a {@link Comparator} which is able to do approximate comparisons between {@link PriorityTask}s
     */
    @SuppressWarnings("Deprecation")
    static <POS extends TilePos> Comparator<PriorityTask> approxComparator() {
        return uncheckedCast(_APPROX_COMPARATOR);
    }

    /**
     * Gets a {@link PriorityTask} using the given {@link TaskStage} and {@link POS}.
     *
     * @param stage the {@link TaskStage}
     * @param pos   the {@link POS}
     * @return a {@link PriorityTask}
     */
    static <POS extends TilePos> PriorityTask forStageAndPosition(@NonNull TaskStage stage, @NonNull POS pos) {
        return new PriorityTask() {
            @Override
            public TaskStage stage() {
                return stage;
            }

            @Override
            public POS pos() {
                return pos;
            }

            @Override
            public int hashCode() {
                return this.stage().hashCode() * 31 + pos.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                } else if (obj instanceof PriorityTask) {
                    PriorityTask o = uncheckedCast(obj);
                    return stage.equals(o.stage()) && pos.equals(o.pos());
                } else {
                    return false;
                }
            }

            @Override
            public String toString() {
                return stage.toString() + '@' + pos;
            }
        };
    }

    /**
     * @return the {@link TaskStage} which this task is at
     */
    TaskStage stage();

    /**
     * @return the tile position where the task is to be executed
     */
    TilePos pos();
}
