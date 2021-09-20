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

package net.daporkchop.fp2.config.gui.container;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.config.gui.IConfigGuiElement;
import net.daporkchop.fp2.config.gui.util.ComponentDimensions;
import net.daporkchop.fp2.config.gui.util.ElementBounds;
import net.daporkchop.lib.common.util.PArrays;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.config.gui.GuiConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class ColumnsContainer extends AbstractConfigGuiContainer {
    public ColumnsContainer(@NonNull List<IConfigGuiElement> elements) {
        super(elements);
    }

    protected int maxColumnSizeX(int sizeX, int columns) {
        return (sizeX - (PADDING * (columns - 1))) / columns;
    }

    protected OptionalInt columnSizeX(int maxColumnSizeX) {
        BitSet commonPossibleSizeX = this.elements.stream()
                .map(elem -> elem.possibleDimensions(maxColumnSizeX, Integer.MAX_VALUE)
                        .mapToInt(ComponentDimensions::sizeX)
                        .collect(BitSet::new, BitSet::set, BitSet::or))
                .reduce((a, b) -> {
                    a.and(b);
                    return a;
                }).get();

        return commonPossibleSizeX.isEmpty() ? OptionalInt.empty() : OptionalInt.of(commonPossibleSizeX.length() - 1);
    }

    protected List<ElementBounds> fillColumns(int x, int y, int columns, int columnSizeX) {
        List<ElementBounds> out = new ArrayList<>(this.elements.size());
        int[] heights = new int[positive(columns, "columns")];

        for (IConfigGuiElement element : this.elements) {
            ComponentDimensions dimensions = element.possibleDimensions(columnSizeX, Integer.MAX_VALUE)
                    .filter(d -> d.sizeX() == columnSizeX)
                    .min(Comparator.comparingInt(ComponentDimensions::sizeY)).get();

            int column = PArrays.indexOf(heights, IntStream.of(heights).min().getAsInt());
            out.add(new ElementBounds(x + column * (columnSizeX + PADDING), y + heights[column], columnSizeX, dimensions.sizeY()));
            heights[column] += dimensions.sizeY() + PADDING;
        }

        return out;
    }

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        @RequiredArgsConstructor
        @Getter
        class ColumnConfiguration {
            final int columns;
            final int columnSizeX;
        }

        int preferredWidth = this.elements.stream()
                .map(IConfigGuiElement::preferredMinimumDimensions)
                .mapToInt(ComponentDimensions::sizeX)
                .max().getAsInt();

        return Stream.of(IntStream.rangeClosed(1, MAX_COLUMNS)
                .mapToObj(columns -> {
                    OptionalInt columnSizeX = this.columnSizeX(this.maxColumnSizeX(totalSizeX, columns));
                    return columnSizeX.isPresent() ? new ColumnConfiguration(columns, columnSizeX.getAsInt()) : null;
                })
                .filter(Objects::nonNull)
                .min((a, b) -> {
                    if (a.columnSizeX >= preferredWidth && b.columnSizeX >= preferredWidth) {
                        return Integer.compare(a.columnSizeX - preferredWidth, b.columnSizeX - preferredWidth);
                    } else if (a.columnSizeX >= preferredWidth) {
                        return -1;
                    } else if (b.columnSizeX >= preferredWidth) {
                        return 1;
                    } else {
                        return -Integer.compare(a.columnSizeX, b.columnSizeX);
                    }
                })
                .map(configuration -> this.fillColumns(0, 0, configuration.columns, configuration.columnSizeX).stream()
                        .reduce(ElementBounds::union)
                        .map(ElementBounds::dimensions)
                        .get())
                .get());
    }

    @Override
    public ComponentDimensions preferredMinimumDimensions() {
        return this.elements.stream()
                .map(IConfigGuiElement::preferredMinimumDimensions)
                .reduce((a, b) -> new ComponentDimensions(max(a.sizeX(), b.sizeX()), a.sizeY() + PADDING + b.sizeY()))
                .get();
    }

    @Override
    public void pack() {
        for (int columns = 1; columns <= MAX_COLUMNS; columns++) {
            OptionalInt columnSizeX = this.columnSizeX(this.maxColumnSizeX(this.bounds.sizeX(), columns));
            if (!columnSizeX.isPresent()) {
                continue;
            }

            List<ElementBounds> elementBounds = this.fillColumns(this.bounds.x(), this.bounds.y(), columns, columnSizeX.getAsInt());
            int totalHeight = elementBounds.stream().mapToInt(bounds -> bounds.y() + bounds.sizeY()).max().getAsInt();
            if (totalHeight > this.bounds.y() + this.bounds.sizeY()) {
                continue;
            }

            for (int i = 0; i < this.elements.size(); i++) {
                this.elements.get(i).bounds(elementBounds.get(i));
            }
            return;
        }

        throw new IllegalStateException();
    }
}
