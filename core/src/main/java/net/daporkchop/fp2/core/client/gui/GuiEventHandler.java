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

/**
 * Events which are fired to notify a GUI of user input.
 *
 * @author DaPorkchop_
 */
public interface GuiEventHandler {
    int MOUSE_BUTTON_LEFT = 0;
    int MOUSE_BUTTON_RIGHT = 1;

    int KEY_ESCAPE = 1;

    /**
     * Called when the screen is opened.
     */
    void init();

    /**
     * Called when the screen is closed.
     */
    void close();

    /**
     * Called when the screen's contents should be re-packed.
     */
    void pack();

    /**
     * Called when the screen is being rendered.
     *
     * @param mouseX the mouse's current X coordinate
     * @param mouseY the mouse's current Y coordinate
     */
    void render(int mouseX, int mouseY);

    /**
     * Called when a mouse button is pressed.
     *
     * @param mouseX the mouse's current X coordinate
     * @param mouseY the mouse's current Y coordinate
     * @param button the mouse button which was pressed
     */
    void mouseDown(int mouseX, int mouseY, int button);

    /**
     * Called when a mouse button is released.
     *
     * @param mouseX the mouse's current X coordinate
     * @param mouseY the mouse's current Y coordinate
     * @param button the mouse button which was released
     */
    void mouseUp(int mouseX, int mouseY, int button);

    /**
     * Called when the mouse wheel is scrolled.
     *
     * @param mouseX the mouse's current X coordinate
     * @param mouseY the mouse's current Y coordinate
     * @param dWheel the amount to scroll by
     */
    void mouseScroll(int mouseX, int mouseY, int dWheel);

    /**
     * Called when the mouse is moved while a mouse button is down.
     *
     * @param oldMouseX the mouse's old X coordinate
     * @param oldMouseY the mouse's old Y coordinate
     * @param newMouseX the mouse's new X coordinate
     * @param newMouseY the mouse's new Y coordinate
     * @param button    the mouse button which is pressed
     */
    void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button);

    /**
     * Called when a key is pressed.
     *
     * @param typedChar the character which was typed
     * @param keyCode   the key's code
     */
    void keyPressed(char typedChar, int keyCode);
}
