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
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Container object representing a batch data query - a query for retrieving voxel data from many positions within a {@link FBlockLevel} at once.
 * <p>
 * A batch data query consists of:
 * <ul>
 *     <li>a {@link PointsQueryShape} containing the positions of the voxels to get the values at</li>
 *     <li>a {@link DataQueryBatchOutput} defining which values to read, and to which the retrieved values will be written</li>
 *     <li>a sample resolution, as described in {@link FBlockLevel}</li>
 *     <li>a {@link QuerySamplingMode sampling mode}, as described in {@link FBlockLevel}</li>
 * </ul>
 *
 * @author DaPorkchop_
 */
@Builder
@Data
public final class BatchDataQuery {
    @Deprecated
    public static BatchDataQuery of(@NonNull PointsQueryShape shape, @NonNull DataQueryBatchOutput output) {
        return builder().shape(shape).output(output).build();
    }

    /**
     * The query's {@link PointsQueryShape shape}.
     */
    @NonNull
    private final PointsQueryShape shape;

    /**
     * The query's {@link DataQueryBatchOutput output}.
     */
    @NonNull
    private final DataQueryBatchOutput output;

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

        int shapeCount = this.shape.count();
        int outputCount = this.output.count();
        checkState(shapeCount >= outputCount, "shape contains %d points, but output only has space for %d!", shapeCount, outputCount);

        notNegative(this.sampleResolution, "sampleResolution");
    }
}
