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

import net.daporkchop.fp2.api.world.level.FBlockLevel;

/**
 * A mode used for sampling voxel data from an {@link FBlockLevel}.
 *
 * @author DaPorkchop_
 * @see FBlockLevel
 */
public enum QuerySamplingMode {
    /**
     * Indicates that the user does not care how the implementation determines the representative voxel data for a sample.
     */
    DONT_CARE,
    /**
     * Indicates that the user is attempting to determine the shape of the terrain surface, and is not interested in interior samples (voxels whose contents are entirely
     * obscured beneath the surface).
     */
    SURFACE,
    /**
     * Indicates that the user is interested in all blocks in the queried volume, and that the representative selection should not be biased towards surface blocks.
     */
    VOLUME,
}
