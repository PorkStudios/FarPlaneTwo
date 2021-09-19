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
import net.daporkchop.fp2.config.gui.ElementDimensions;
import net.daporkchop.fp2.config.gui.IConfigGuiElement;
import net.daporkchop.fp2.config.gui.IConfigGuiScreen;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.fp2.config.gui.ScrollingContainer;
import net.minecraft.client.resources.I18n;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.config.gui.GuiConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.input.Keyboard.*;

/**
 * @author DaPorkchop_
 */
public class DefaultConfigGuiScreen implements IConfigGuiScreen {
    protected final IGuiContext context;
    protected final Object instance;

    protected final IConfigGuiElement container;

    protected int sizeX;
    protected int sizeY;

    public DefaultConfigGuiScreen(@NonNull IGuiContext context, @NonNull Object instance) {
        this.context = context;
        this.instance = instance;

        this.container = new ScrollingContainer(Stream.of(instance.getClass().getFields())
                .filter(field -> (field.getModifiers() & Modifier.STATIC) == 0)
                .map(field -> ConfigHelper.createConfigGuiElement(context, instance, field))
                .collect(Collectors.toList()));
    }

    @Override
    public void setDimensions(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;

        ElementDimensions dimensions = this.container.possibleDimensions(sizeX - (PADDING << 1), sizeY - HEADER_TITLE_HEIGHT - PADDING)
                .min(Comparator.comparingInt(ElementDimensions::sizeY)).get(); //find the shortest possible dimensions
        this.container.setPositionAndDimensions((sizeX - dimensions.sizeX()) >> 1, HEADER_TITLE_HEIGHT, dimensions.sizeX(), dimensions.sizeY());

        /*int rowWidth = this.elements.stream().map(IConfigGuiElement::possibleDimensions).mapToInt(ElementDimensions::minSizeX).max().getAsInt();
        int rowHeight = this.elements.stream().map(IConfigGuiElement::possibleDimensions).mapToInt(ElementDimensions::minSizeY).max().getAsInt();

        for (int i = 0; i < this.elements.size(); i++) {
            this.elements.get(i).setPositionAndDimensions(
                    ((sizeX + PADDING) >> 1) - (rowWidth + PADDING) * (~i & 1),
                    HEADER_TITLE_HEIGHT + (rowHeight + PADDING) * (i >> 1),
                    rowWidth, rowHeight);
        }*/
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.context.drawDefaultBackground();

        String titleString = I18n.format(this.context.localeKeyBase() + "title");
        MC.fontRenderer.drawStringWithShadow(titleString, (this.sizeX - MC.fontRenderer.getStringWidth(titleString)) >> 1, 15, -1);

        this.container.render(mouseX, mouseY, partialTicks);
        this.container.renderOverlay(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        this.container.mouseDown(mouseX, mouseY, button);
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        this.container.mouseUp(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        this.container.mouseDragged(oldMouseX, oldMouseY, newMouseX, newMouseY, button);
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        this.container.mouseScroll(mouseX, mouseY, dWheel);
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        if (keyCode == KEY_ESCAPE) {
            this.context.pop();
        } else {
            this.container.keyPressed(typedChar, keyCode);
        }
    }
}
