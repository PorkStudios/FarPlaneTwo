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

package net.daporkchop.fp2.config.gui.screen;

import lombok.NonNull;
import net.daporkchop.fp2.config.ConfigHelper;
import net.daporkchop.fp2.config.gui.IConfigGuiElement;
import net.daporkchop.fp2.config.gui.IConfigGuiScreen;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.lib.math.vector.i.Vec2i;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.input.Keyboard.*;

/**
 * @author DaPorkchop_
 */
public class DefaultConfigGuiScreen implements IConfigGuiScreen {
    protected final IGuiContext context;
    protected final Object instance;

    protected final List<IConfigGuiElement> elements;

    protected int sizeX;
    protected int sizeY;

    public DefaultConfigGuiScreen(@NonNull IGuiContext context, @NonNull Object instance) {
        this.context = context;
        this.instance = instance;

        this.elements = Stream.of(instance.getClass().getFields())
                .filter(field -> (field.getModifiers() & Modifier.STATIC) == 0)
                .map(field -> ConfigHelper.createConfigGuiElement(context, instance, field))
                .collect(Collectors.toList());
    }

    @Override
    public void setDimensions(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;

        int y = 0;
        for (IConfigGuiElement element : this.elements) {
            Vec2i minDimensions = element.minDimensions();

            element.setPositionAndDimensions(0, y, sizeX, minDimensions.getY());
            y += minDimensions.getY();
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.elements.forEach(element -> element.render(mouseX, mouseY, partialTicks));
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        this.elements.forEach(element -> element.mouseDown(mouseX, mouseY, button));
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        this.elements.forEach(element -> element.mouseUp(mouseX, mouseY, button));
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        this.elements.forEach(element -> element.mouseDragged(oldMouseX, oldMouseY, newMouseX, newMouseY, button));
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        if (keyCode == KEY_ESCAPE) {
            this.context.pop();
        } else {
            this.elements.forEach(element -> element.keyPressed(typedChar, keyCode));
        }
    }
}
