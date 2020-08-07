/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.strategy.base.server;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.util.threading.executor.LazyKey;

/**
 * @author DaPorkchop_
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public final class TaskKey implements LazyKey<TaskKey> {
    @NonNull
    protected final TaskStage stage;
    protected final int level;
    protected final int tieBreak;

    public TaskKey(@NonNull TaskStage stage, int level) {
        this(stage, level, 0);
    }

    @Override
    public int compareTo(TaskKey o) {
        int d;
        if (this.stage.first() || o.stage.first()) {
            d = Integer.compare(this.stage.ordinal(), o.stage.ordinal());
            if (d == 0) {
                d = Integer.compare(this.level, o.level);
            }
        } else {
            d = Integer.compare(this.level, o.level);
            if (d == 0) {
                d = Integer.compare(this.stage.ordinal(), o.stage.ordinal());
            }
        }
        if (d == 0) {
            d = Integer.compare(this.tieBreak, o.tieBreak);
        }
        return d;
    }

    public TaskKey withStage(@NonNull TaskStage stage)  {
        return new TaskKey(stage, this.level, this.tieBreak);
    }

    public TaskKey withStageLevel(@NonNull TaskStage stage, int level)  {
        return new TaskKey(stage, level, this.tieBreak);
    }

    public TaskKey withLevel(int level)  {
        return new TaskKey(this.stage, level, this.tieBreak);
    }

    @Override
    public TaskKey raiseTie() {
        return new TaskKey(this.stage, this.level, this.tieBreak - 1);
    }

    @Override
    public TaskKey lowerTie() {
        return new TaskKey(this.stage, this.level, this.tieBreak + 1);
    }
}
