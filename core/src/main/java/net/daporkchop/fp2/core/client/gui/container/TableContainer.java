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

package net.daporkchop.fp2.core.client.gui.container;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiElement;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;
import net.daporkchop.fp2.core.client.gui.util.ElementBounds;

import java.lang.reflect.Array;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
public class TableContainer extends AbstractConfigGuiContainer {
    protected final GuiElement[][] grid;
    protected final int rows;
    protected final int cols;

    public TableContainer(@NonNull GuiContext context, @NonNull GuiElement[]... grid) {
        super(context, Stream.of(grid).flatMap(Stream::of).collect(Collectors.toList()));

        this.grid = grid;
        this.rows = grid.length;
        this.cols = Stream.of(this.grid).mapToInt(Array::getLength).max().orElse(-1);
    }

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        return Stream.of(this.preferredMinimumDimensions());
    }

    @Override
    public ComponentDimensions preferredMinimumDimensions() {
        return new ComponentDimensions(
                IntStream.range(0, this.cols).map(this::getColumnWidth).reduce((a, b) -> a + PADDING + b).orElse(0),
                IntStream.range(0, this.rows).map(this::getRowHeight).reduce((a, b) -> a + PADDING + b).orElse(0));
    }

    protected int getColumnWidth(int column) {
        return Stream.of(this.grid)
                .filter(row -> row.length > column)
                .mapToInt(row -> row[column].preferredMinimumDimensions().sizeX())
                .max().getAsInt();
    }

    protected int getRowHeight(int row) {
        return Stream.of(this.grid[row])
                .map(GuiElement::preferredMinimumDimensions)
                .mapToInt(ComponentDimensions::sizeY)
                .max().getAsInt();
    }

    @Override
    public void pack() {
        int[] widths = IntStream.range(0, this.cols).map(this::getColumnWidth).toArray();
        int[] heights = IntStream.range(0, this.rows).map(this::getRowHeight).toArray();

        int[] x = new int[this.cols];
        int[] y = new int[this.rows];
        for (int i = 1; i < this.cols; i++) {
            x[i] += x[i - 1] + PADDING + widths[i - 1];
        }
        for (int i = 1; i < this.rows; i++) {
            y[i] += y[i - 1] + PADDING + heights[i - 1];
        }

        for (int row = 0; row < this.rows; row++) {
            GuiElement[] rowElements = this.grid[row];

            for (int col = 0; col < min(rowElements.length, this.cols); col++) {
                rowElements[col].bounds(new ElementBounds(x[col], y[row], widths[col], heights[row]));
            }
        }
    }
}
