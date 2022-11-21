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

package net.daporkchop.fp2.api.world.level.query;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;

import java.util.List;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Container object representing a batch type transition query - a query for executing multiple
 * {@link FBlockLevel#getNextTypeTransitions(Direction, int, int, int, List, TypeTransitionSingleOutput) type transition searches} at once.
 * <p>
 * A batch type transition query consists of:
 * <ul>
 *     <li>a {@link PointsQueryShape} containing the positions of the voxels to begin iteration at</li>
 *     <li>a {@link TypeTransitionBatchOutput} defining which values to read, and to which the retrieved values will be written</li>
 * </ul>
 *
 * @author DaPorkchop_
 */
public interface BatchTypeTransitionQuery {
    static BatchTypeTransitionQuery of(@NonNull Direction direction, @NonNull List<@NonNull TypeTransitionFilter> filters,
                                       @NonNull PointsQueryShape shape, @NonNull TypeTransitionBatchOutput output) {
        return new BatchTypeTransitionQuery() {
            @Override
            public void validate() throws RuntimeException {
                shape.validate();
                output.validate();

                int shapeCount = shape.count();
                int outputSlots = output.slots();
                checkState(shapeCount >= outputSlots, "shape contains %d points, but output only has space for %d slots!", shapeCount, outputSlots);
            }

            @Override
            public Direction direction() {
                return direction;
            }

            @Override
            public List<@NonNull TypeTransitionFilter> filters() {
                return filters;
            }

            @Override
            public PointsQueryShape shape() {
                return shape;
            }

            @Override
            public TypeTransitionBatchOutput output() {
                return output;
            }
        };
    }

    /**
     * Ensures that this query's state is valid, throwing an exception if not.
     * <p>
     * If this method is not called and the query's state is invalid, the behavior of all methods is undefined.
     * <p>
     * It is recommended to call this once per method body before using a query instance, as it could allow the JVM to optimize the code more aggressively.
     *
     * @throws RuntimeException if the query's state is invalid
     */
    void validate() throws RuntimeException;

    /**
     * @return the query's {@link Direction search direction}
     */
    Direction direction();

    /**
     * @return the query's {@link TypeTransitionFilter filters}
     */
    List<@NonNull TypeTransitionFilter> filters();

    /**
     * @return the query's {@link PointsQueryShape shape}
     */
    PointsQueryShape shape();

    /**
     * @return the query's {@link TypeTransitionBatchOutput output}
     */
    TypeTransitionBatchOutput output();
}
