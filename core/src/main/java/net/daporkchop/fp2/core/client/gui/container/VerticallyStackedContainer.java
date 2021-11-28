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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
public class VerticallyStackedContainer extends AbstractConfigGuiContainer {
    protected static final Comparator<ComponentDimensions> COMPARATOR_LOWY_HIGHX = Comparator.comparingInt(ComponentDimensions::sizeY)
            .thenComparing(Comparator.comparingInt(ComponentDimensions::sizeX).reversed());

    public VerticallyStackedContainer(@NonNull GuiContext context, @NonNull List<GuiElement> elements) {
        super(context, elements);
    }

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        List<ComponentDimensions> shortestDimensions = this.elements.stream()
                .map(elem -> elem.possibleDimensions(totalSizeX, Integer.MAX_VALUE).min(COMPARATOR_LOWY_HIGHX))
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());

        return Stream.of(new ComponentDimensions(
                shortestDimensions.stream().mapToInt(ComponentDimensions::sizeX).max().getAsInt(),
                shortestDimensions.stream().mapToInt(ComponentDimensions::sizeY).reduce(0, (a, b) -> a + PADDING + b)));
    }

    @Override
    public ComponentDimensions preferredMinimumDimensions() {
        return this.elements.stream()
                .map(GuiElement::preferredMinimumDimensions)
                .reduce((a, b) -> new ComponentDimensions(max(a.sizeX(), b.sizeX()), a.sizeY() + PADDING + b.sizeY()))
                .get();
    }

    @Override
    public void pack() {
        for (int y = 0, i = 0; i < this.elements.size(); i++) {
            GuiElement element = this.elements.get(i);
            ComponentDimensions dimensions = element.possibleDimensions(this.bounds.sizeX(), Integer.MAX_VALUE).min(COMPARATOR_LOWY_HIGHX).get();

            element.bounds(new ElementBounds((this.bounds.sizeX() - dimensions.sizeX()) >> 1, y, dimensions.sizeX(), dimensions.sizeY()));
            y += dimensions.sizeY() + PADDING;
        }
    }
}
