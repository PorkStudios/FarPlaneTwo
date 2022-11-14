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

package net.daporkchop.fp2.core.client.gui;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;
import net.daporkchop.fp2.core.client.gui.util.ElementBounds;

import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public interface GuiElement extends GuiEventHandler {
    Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY);

    ComponentDimensions preferredMinimumDimensions();

    /**
     * @return this element's bounds
     */
    ElementBounds bounds();

    /**
     * Sets this element's bounds.
     * <p>
     * Implicitly calls {@link #pack()} after the bounds are updated.
     *
     * @param bounds the new bounds
     */
    void bounds(@NonNull ElementBounds bounds);
}
