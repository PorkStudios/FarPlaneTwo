/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.util.math;

import lombok.Data;
import net.minecraft.util.math.AxisAlignedBB;

import static net.daporkchop.lib.common.math.PMath.*;

/**
 * Alternative to {@link AxisAlignedBB} using {@code int} coordinates.
 * <p>
 * All coordinates are inclusive.
 *
 * @author DaPorkchop_
 * @see AxisAlignedBB
 */
@Data
public class IntAxisAlignedBB {
    protected final int minX;
    protected final int minY;
    protected final int minZ;
    protected final int maxY;
    protected final int maxX;
    protected final int maxZ;

    public int clampX(int x) {
        return clamp(x, this.minX, this.maxX);
    }

    public int clampY(int y) {
        return clamp(y, this.minY, this.maxY);
    }

    public int clampZ(int z) {
        return clamp(z, this.minZ, this.maxZ);
    }

    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX
               && y >= this.minY && y <= this.maxY
               && z >= this.minZ && z <= this.maxZ;
    }

    public boolean contains2d(int x, int z) {
        return x >= this.minX && x <= this.maxX
               && z >= this.minZ && z <= this.maxZ;
    }
}
