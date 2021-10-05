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

package net.daporkchop.fp2.client.gui.util;

import lombok.Data;
import lombok.NonNull;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
@Data
@SideOnly(Side.CLIENT)
public final class ElementBounds {
    public static final ElementBounds ZERO = new ElementBounds(0, 0, 0, 0);

    protected final int x;
    protected final int y;
    protected final int sizeX;
    protected final int sizeY;

    public int maxX() {
        return this.x + this.sizeX;
    }

    public int maxY() {
        return this.y + this.sizeY;
    }

    public ElementBounds union(@NonNull ElementBounds other) {
        int x = min(this.x, other.x);
        int y = min(this.y, other.y);
        int maxX = max(this.maxX(), other.maxX());
        int maxY = max(this.maxY(), other.maxY());

        return new ElementBounds(x, y, maxX - x, maxY - y);
    }

    public ComponentDimensions dimensions() {
        return new ComponentDimensions(this.sizeX, this.sizeY);
    }

    public ElementBounds withPos(int x, int y) {
        return x != this.x || y != this.y ? new ElementBounds(x, y, this.sizeX, this.sizeY) : this;
    }

    public ElementBounds withSize(int sizeX, int sizeY) {
        return sizeX != this.sizeX || sizeY != this.sizeY ? new ElementBounds(this.x, this.y, sizeX, sizeY) : this;
    }

    /**
     * Checks whether or not the given point is contained within this element's bounds.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @return whether or not the given point is contained within this element's bounds
     */
    public boolean contains(int x, int y) {
        return x >= this.x && x < this.x + this.sizeX && y >= this.y && y < this.y + this.sizeY;
    }
}
