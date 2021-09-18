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

package net.daporkchop.fp2.config.gui.element;

import lombok.NonNull;
import net.daporkchop.fp2.config.gui.AbstractConfigGuiElement;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.lib.math.vector.i.Vec2i;
import net.minecraftforge.fml.client.config.GuiButtonExt;

import java.lang.reflect.Field;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public abstract class GuiButton<V> extends AbstractConfigGuiElement<V> {
    protected GuiButtonExt button;

    public GuiButton(@NonNull IGuiContext context, @NonNull Object instance, @NonNull Field field) {
        super(context, instance, field);

        this.button = new GuiButtonExt(0, 0, 0, this.buttonText());
    }

    @Override
    public Vec2i minDimensions() {
        return new Vec2i(20, 20);
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);

        this.button.x = x;
        this.button.y = y;
    }

    @Override
    public void setDimensions(int sizeX, int sizeY) {
        super.setDimensions(sizeX, sizeY);

        this.button.width = sizeX;
        this.button.height = sizeY;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.button.drawButton(MC, mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (this.button.mousePressed(MC, mouseX, mouseY) && this.handleClick(button)) {
            this.button.displayString = this.buttonText();
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
    public void keyPressed(char typedChar, int keyCode) {
        //no-op
    }

    protected abstract String buttonText();

    protected abstract boolean handleClick(int button);
}
