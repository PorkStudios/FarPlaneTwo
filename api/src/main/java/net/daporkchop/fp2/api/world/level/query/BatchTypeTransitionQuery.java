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

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.List;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Container object representing a batch type transition query - a query for executing multiple
 * {@link FBlockLevel#getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput, int, QuerySamplingMode) type transition searches} at once.
 * <p>
 * A batch type transition query consists of:
 * <ul>
 *     <li>a {@link Direction} to iterate in</li>
 *     <li>a maximum iteration distance, as described in {@link FBlockLevel#getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput, int, QuerySamplingMode)}</li>
 *     <li>a {@link List} of {@link TypeTransitionFilter}s describing the type transitions to search for</li>
 *     <li>a {@link PointsQueryShape} containing the positions of the voxels to begin iteration at</li>
 *     <li>a {@link TypeTransitionBatchOutput} defining which values to read, and to which the retrieved values will be written</li>
 *     <li>a sample resolution, as described in {@link FBlockLevel}</li>
 *     <li>a {@link QuerySamplingMode sampling mode}, as described in {@link FBlockLevel}</li>
 * </ul>
 *
 * @author DaPorkchop_
 */
@Builder
@Data
public final class BatchTypeTransitionQuery {
    @Deprecated
    public static BatchTypeTransitionQuery of(@NonNull Direction direction, @NotNegative long maxDistance,
                                       @NonNull List<@NonNull TypeTransitionFilter> filters,
                                       @NonNull PointsQueryShape shape, @NonNull TypeTransitionBatchOutput output) {
        return builder().direction(direction).maxDistance(maxDistance).filters(filters).shape(shape).output(output).build();
    }

    /**
     * The query's {@link Direction search direction}.
     */
    @NonNull
    private final Direction direction;

    /**
     * The query's maximum search distance.
     */
    @Builder.Default
    private final @NotNegative long maxDistance = Long.MAX_VALUE;

    /**
     * The query's {@link TypeTransitionFilter filters}.
     */
    @NonNull
    private final List<@NonNull TypeTransitionFilter> filters;

    /**
     * The query's {@link PointsQueryShape shape}.
     */
    @NonNull
    private final PointsQueryShape shape;

    /**
     * The query's {@link TypeTransitionBatchOutput output}.
     */
    @NonNull
    private final TypeTransitionBatchOutput output;

    /**
     * The query's sample resolution.
     *
     * @see FBlockLevel
     */
    @Builder.Default
    private final @NotNegative int sampleResolution = 0;

    /**
     * The query's {@link QuerySamplingMode sampling mode}.
     *
     * @see FBlockLevel
     */
    @Builder.Default
    @NonNull
    private final QuerySamplingMode samplingMode = QuerySamplingMode.DONT_CARE;

    /**
     * Ensures that this query's state is valid, throwing an exception if not.
     * <p>
     * If this method is not called and the query's state is invalid, the behavior of all methods is undefined.
     * <p>
     * It is recommended to call this once per method body before using a query instance, as it could allow the JVM to optimize the code more aggressively.
     *
     * @throws RuntimeException if the query's state is invalid
     */
    public void validate() throws RuntimeException {
        this.shape.validate();
        this.output.validate();
        notNegative(this.maxDistance, "maxDistance");

        int shapeCount = this.shape.count();
        int outputSlots = this.output.slots();
        checkState(shapeCount >= outputSlots, "shape contains %d points, but output only has space for %d slots!", shapeCount, outputSlots);

        notNegative(this.sampleResolution, "sampleResolution");
    }
}
