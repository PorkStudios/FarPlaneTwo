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

/**
 * Version-independent interface for rendering GUI components.
 *
 * @author DaPorkchop_
 */
public interface GuiRenderer {
    /**
     * Translates the view region by the given offset for all rendering commands executed within the given {@link Runnable}.
     *
     * @param dx     the offset along the X axis
     * @param dy     the offset along the Y axis
     * @param action a {@link Runnable} which will execute more render code
     */
    void translate(int dx, int dy, @NonNull Runnable action);

    /**
     * Sets the scissor region for all rendering commands executed within the given {@link Runnable}.
     *
     * @param x      the scissor region's X coordinate
     * @param y      the scissor region's Y coordinate
     * @param width  the scissor region's width
     * @param height the scissor region's height
     * @param action a {@link Runnable} which will execute more render code
     */
    void scissor(int x, int y, int width, int height, @NonNull Runnable action);

    /**
     * Draws the default background texture.
     *
     * @param x      the background's X coordinate
     * @param y      the background's Y coordinate
     * @param width  the background's width
     * @param height the background's height
     */
    void drawDefaultBackground(int x, int y, int width, int height);

    /**
     * Draws a quad with a fixed color.
     *
     * @param x      the quad's X coordinate
     * @param y      the quad's Y coordinate
     * @param width  the quad's width
     * @param height the quad's height
     * @param argb   the quad's color
     */
    void drawQuadColored(int x, int y, int width, int height, int argb);

    /**
     * Draws a quad textured for use as a button.
     *
     * @param x       the quad's X coordinate
     * @param y       the quad's Y coordinate
     * @param width   the quad's width
     * @param height  the quad's height
     * @param hovered whether or not the mouse is hovered over the button
     * @param enabled whether or not the button is enabled
     */
    void drawButtonBackground(int x, int y, int width, int height, boolean hovered, boolean enabled);

    /**
     * Computes the width of the given {@link CharSequence}.
     *
     * @param text the text
     * @return the text's width
     */
    int getStringWidth(@NonNull CharSequence text);

    /**
     * @return the height of a line of text
     */
    int getStringHeight();

    /**
     * Truncates the given {@link CharSequence} so that it is at most the given width.
     *
     * @param text    the text
     * @param width   the text's maximum width
     * @param reverse if {@code true}, the text will be truncated from the beginning rather than from the end
     * @return the trimmed text
     */
    CharSequence trimStringToWidth(@NonNull CharSequence text, int width, boolean reverse);

    /**
     * Draws the given {@link CharSequence}.
     *
     * @param text   the text
     * @param x      the text's X coordinate
     * @param y      the text's Y coordinate
     * @param argb   the text's color
     * @param shadow whether or not the text should drop a shadow
     */
    void drawString(@NonNull CharSequence text, int x, int y, int argb, boolean shadow);

    /**
     * Draws the given {@link CharSequence}.
     *
     * @param text               the text
     * @param x                  the text's X coordinate
     * @param y                  the text's Y coordinate
     * @param argb               the text's color
     * @param shadow             whether or not the text should drop a shadow
     * @param centeredHorizontal whether or not the text should be centered horizontally
     * @param centeredVertical   whether or not the text should be centered vertically
     */
    default void drawCenteredString(@NonNull CharSequence text, int x, int y, int argb, boolean shadow, boolean centeredHorizontal, boolean centeredVertical) {
        if (centeredHorizontal) {
            x -= this.getStringWidth(text) >> 1;
        }
        if (centeredVertical) {
            y -= this.getStringHeight() >> 1;
        }

        this.drawString(text, x, y, argb, shadow);
    }

    /**
     * Draws the given {@link CharSequence}s as a tooltip.
     *
     * @param mouseX   the mouse's X coordinate
     * @param mouseY   the mouse's Y coordinate
     * @param maxWidth the maximum tooltip width, or {@code -1} if no limit should be applied
     * @param lines    the lines of text to draw. Any newlines will be ignored
     */
    void drawTooltip(int mouseX, int mouseY, int maxWidth, @NonNull CharSequence... lines);
}
