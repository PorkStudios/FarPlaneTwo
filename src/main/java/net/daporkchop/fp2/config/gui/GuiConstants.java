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

package net.daporkchop.fp2.config.gui;

import lombok.experimental.UtilityClass;

/**
 * Constant values used throughout the GUI code.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class GuiConstants {
    //
    // sizes
    //

    /**
     * The padding around all components (vertical and horizontal).
     */
    public static final int PADDING = 2;

    /**
     * The height of the title in a GUI header.
     */
    public static final int HEADER_TITLE_HEIGHT = 30;

    /**
     * The width of a scrollbar.
     */
    public static final int SCROLLBAR_WIDTH = 5;

    /**
     * The width of a standard button.
     */
    public static final int BUTTON_WIDTH = 50;

    /**
     * The height of a button.
     */
    public static final int BUTTON_HEIGHT = 20;

    public static final int BUTTON_INTERNAL_PADDING_HORIZONTAL = 6;
}
