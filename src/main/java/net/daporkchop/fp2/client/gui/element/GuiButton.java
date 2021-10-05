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

package net.daporkchop.fp2.client.gui.element;

import lombok.NonNull;
import net.daporkchop.fp2.client.gui.IGuiContext;
import net.daporkchop.fp2.client.gui.access.GuiObjectAccess;
import net.daporkchop.fp2.client.gui.util.ComponentDimensions;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gui.GuiConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public abstract class GuiButton<V> extends AbstractReflectiveConfigGuiElement<V> {
    protected GuiButtonExt button = new GuiButtonExt(0, 0, 0, "");

    public GuiButton(@NonNull IGuiContext context, @NonNull GuiObjectAccess<V> access) {
        super(context, access);
    }

    @Override
    public void init() {
        this.button.displayString = this.text();
    }

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        int textWidth = MC.fontRenderer.getStringWidth(this.button.displayString) + BUTTON_INTERNAL_PADDING_HORIZONTAL;
        return textWidth >= totalSizeX
                ? Stream.of(new ComponentDimensions(totalSizeX, min(totalSizeY, BUTTON_HEIGHT))) //not enough space for the full text width
                : IntStream.rangeClosed(textWidth, totalSizeX).mapToObj(i -> new ComponentDimensions(i, min(totalSizeY, BUTTON_HEIGHT)));
    }

    @Override
    public ComponentDimensions preferredMinimumDimensions() {
        return new ComponentDimensions(200, BUTTON_HEIGHT);
    }

    @Override
    public void pack() {
        this.button.x = this.bounds.x();
        this.button.y = this.bounds.y();
        this.button.width = this.bounds.sizeX();
        this.button.height = this.bounds.sizeY();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.button.drawButton(MC, mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        super.mouseDown(mouseX, mouseY, button);

        if (button == 0 && this.button.mousePressed(MC, mouseX, mouseY)) {
            this.handleClick(button);
        }
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        //no-op
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        //no-op
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        //no-op
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        //no-op
    }

    protected abstract void handleClick(int button);
}
