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

package net.daporkchop.fp2.core.util.math.qef;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * @author DaPorkchop_
 */
@AllArgsConstructor
@NoArgsConstructor
public class QefData {
    protected double ata_00, ata_01, ata_02, ata_11, ata_12, ata_22;
    protected double atb_x, atb_y, atb_z;
    protected double btb;
    protected double massPoint_x, massPoint_y, massPoint_z;
    protected int numPoints;

    public QefData(QefData data) {
        this(data.ata_00, data.ata_01, data.ata_02, data.ata_11, data.ata_12, data.ata_22, data.atb_x, data.atb_y, data.atb_z, data.btb, data.massPoint_x, data.massPoint_y, data.massPoint_z, data.numPoints);
    }

    public void add(QefData data) {
        this.ata_00 += data.ata_00;
        this.ata_01 += data.ata_01;
        this.ata_02 += data.ata_02;
        this.ata_11 += data.ata_11;
        this.ata_12 += data.ata_12;
        this.ata_22 += data.ata_22;
        this.atb_x += data.atb_x;
        this.atb_y += data.atb_y;
        this.atb_z += data.atb_z;
        this.btb += data.btb;
        this.massPoint_x += data.massPoint_x;
        this.massPoint_y += data.massPoint_y;
        this.massPoint_z += data.massPoint_z;
        this.numPoints += data.numPoints;
    }

    public void clear() {
        this.ata_00 = 0.0f;
        this.ata_01 = 0.0f;
        this.ata_02 = 0.0f;
        this.ata_11 = 0.0f;
        this.ata_12 = 0.0f;
        this.ata_22 = 0.0f;
        this.atb_x = 0.0f;
        this.atb_y = 0.0f;
        this.atb_z = 0.0f;
        this.btb = 0.0f;
        this.massPoint_x = 0.0f;
        this.massPoint_y = 0.0f;
        this.massPoint_z = 0.0f;
        this.numPoints = 0;
    }

    public void set(double ata_00, double ata_01, double ata_02, double ata_11, double ata_12, double ata_22, double atb_x, double atb_y, double atb_z, double btb, double massPoint_x, double massPoint_y, double massPoint_z, int numPoints) {
        this.ata_00 = ata_00;
        this.ata_01 = ata_01;
        this.ata_02 = ata_02;
        this.ata_11 = ata_11;
        this.ata_12 = ata_12;
        this.ata_22 = ata_22;
        this.atb_x = atb_x;
        this.atb_y = atb_y;
        this.atb_z = atb_z;
        this.btb = btb;
        this.massPoint_x = massPoint_x;
        this.massPoint_y = massPoint_y;
        this.massPoint_z = massPoint_z;
        this.numPoints = numPoints;
    }

    public void set(QefData data) {
        this.ata_00 = data.ata_00;
        this.ata_01 = data.ata_01;
        this.ata_02 = data.ata_02;
        this.ata_11 = data.ata_11;
        this.ata_12 = data.ata_12;
        this.ata_22 = data.ata_22;
        this.atb_x = data.atb_x;
        this.atb_y = data.atb_y;
        this.atb_z = data.atb_z;
        this.btb = data.btb;
        this.massPoint_x = data.massPoint_x;
        this.massPoint_y = data.massPoint_y;
        this.massPoint_z = data.massPoint_z;
        this.numPoints = data.numPoints;
    }
}
