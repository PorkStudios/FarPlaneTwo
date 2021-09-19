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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.config.gui.GuiConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class ScrollingContainer implements IConfigGuiElement {
    @NonNull
    protected final List<IConfigGuiElement> elements;

    protected int x;
    protected int y;
    protected int sizeX;
    protected int sizeY;

    protected int deltaY;
    protected int totalHeight;

    protected boolean draggingScrollbar = false;

    @Override
    public Stream<ElementDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        List<ElementDimensions> possibleDimensions = new ArrayList<>();

        { //without scrollbar (height does make a difference here)
            List<ElementDimensions> shortestDimensions = this.elements.stream()
                    .map(elem -> elem.possibleDimensions(totalSizeX, totalSizeY).min(Comparator.comparingInt(ElementDimensions::sizeY)).get())
                    .collect(Collectors.toList());

            int height = shortestDimensions.stream().mapToInt(ElementDimensions::sizeY).reduce(0, (a, b) -> a + PADDING + b);
            if (height <= totalSizeY) { //removing the scrollbar is only possible if the total height is small enough
                possibleDimensions.add(new ElementDimensions(
                        shortestDimensions.stream().mapToInt(ElementDimensions::sizeX).max().getAsInt(),
                        height));
            }
        }

        { //with scrollbar (needs to be a bit narrower because of it, but height is irrelevant)
            List<ElementDimensions> shortestDimensions = this.elements.stream()
                    .map(elem -> elem.possibleDimensions(totalSizeX - PADDING - SCROLLBAR_WIDTH, Integer.MAX_VALUE).min(Comparator.comparingInt(ElementDimensions::sizeY)).get())
                    .collect(Collectors.toList());

            possibleDimensions.add(new ElementDimensions(
                    shortestDimensions.stream().mapToInt(ElementDimensions::sizeX).max().getAsInt(),
                    min(shortestDimensions.stream().mapToInt(ElementDimensions::sizeY).reduce(0, (a, b) -> a + PADDING + b), totalSizeY)));
        }

        return possibleDimensions.stream();
    }

    @Override
    public void setPositionAndDimensions(int x, int y, int sizeX, int sizeY) {
        this.x = x;
        this.y = y;
        this.sizeX = sizeX;
        this.sizeY = sizeY;

        this.repack();
    }

    @Override
    public void setPosition(int x, int y) {
        this.setPositionAndDimensions(x, y, this.sizeX, this.sizeY);
    }

    @Override
    public void setDimensions(int sizeX, int sizeY) {
        this.setPositionAndDimensions(this.x, this.y, sizeX, sizeY);
    }

    protected void repack() {
        this.deltaY = 0; //reset scroll distance
        this.draggingScrollbar = false;

        { //without scrollbar (height does make a difference here)
            List<ElementDimensions> shortestDimensions = this.elements.stream()
                    .map(elem -> elem.possibleDimensions(this.sizeX, this.sizeY).min(Comparator.comparingInt(ElementDimensions::sizeY)).get())
                    .collect(Collectors.toList());

            int height = shortestDimensions.stream().mapToInt(ElementDimensions::sizeY).reduce(0, (a, b) -> a + PADDING + b);
            if (height <= this.sizeY) { //removing the scrollbar is only possible if the total height is small enough
                this.totalHeight = this.sizeY; //no scrollbar is needed

                for (int y = 0, i = 0; i < this.elements.size(); i++) {
                    IConfigGuiElement element = this.elements.get(i);
                    ElementDimensions dimensions = shortestDimensions.get(i);

                    element.setPositionAndDimensions(0, y, dimensions.sizeX(), dimensions.sizeY());
                    y += dimensions.sizeY() + PADDING;
                }

                return;
            }
        }

        { //with scrollbar (needs to be a bit narrower because of it, but height is irrelevant)
            List<ElementDimensions> shortestDimensions = this.elements.stream()
                    .map(elem -> elem.possibleDimensions(this.sizeX - PADDING - SCROLLBAR_WIDTH, Integer.MAX_VALUE).min(Comparator.comparingInt(ElementDimensions::sizeY)).get())
                    .collect(Collectors.toList());

            for (int y = 0, i = 0; i < this.elements.size(); i++) {
                IConfigGuiElement element = this.elements.get(i);
                ElementDimensions dimensions = shortestDimensions.get(i);

                element.setPositionAndDimensions(0, y, dimensions.sizeX(), dimensions.sizeY());
                y += dimensions.sizeY() + PADDING;
            }

            this.totalHeight = shortestDimensions.stream().mapToInt(ElementDimensions::sizeY).reduce(0, (a, b) -> a + PADDING + b);
        }
    }

    protected int offsetX(int x) {
        return x - this.x;
    }

    protected int offsetY(int y) {
        return y - this.y + this.deltaY;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution scaledResolution = new ScaledResolution(MC);
        int scale = scaledResolution.getScaleFactor();
        glScissor(this.x * scale, (scaledResolution.getScaledHeight() - this.y - this.sizeY) * scale, this.sizeX * scale, this.sizeY * scale);
        glEnable(GL_SCISSOR_TEST);

        glPushMatrix();
        glTranslatef(-this.offsetX(0), -this.offsetY(0), 0.0f);

        try {
            this.elements.forEach(element -> element.render(this.offsetX(mouseX), this.offsetY(mouseY), partialTicks));
        } finally {
            glPopMatrix();

            glDisable(GL_SCISSOR_TEST);
        }

        if (this.totalHeight > this.sizeY) { //we need to draw a scrollbar
            double viewPort = (double) this.sizeY / (double) this.totalHeight;
            int barHeight = floorI(this.sizeY * viewPort);
            int barStart = floorI(((double) this.deltaY / (double) (this.totalHeight - this.sizeY)) * (this.sizeY - barHeight));

            Gui.drawRect(this.x + this.sizeX - SCROLLBAR_WIDTH, this.y, this.x + this.sizeX, this.y + this.sizeY, 0xB0000000);
            Gui.drawRect(this.x + this.sizeX - SCROLLBAR_WIDTH, this.y + barStart, this.x + this.sizeX, this.y + barStart + barHeight, 0xFFDDDDDD);
        }
    }

    @Override
    public void renderOverlay(int mouseX, int mouseY, float partialTicks) {
        glPushMatrix();
        glTranslatef(this.x, this.y - this.deltaY, 0.0f);

        try {
            this.elements.forEach(element -> element.renderOverlay(this.offsetX(mouseX), this.offsetY(mouseY), partialTicks));
        } finally {
            glPopMatrix();
        }
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        this.draggingScrollbar = button == 0 //left-click
                                 && this.totalHeight > this.sizeY //scrollbar is visible
                                 && mouseX >= this.x + this.sizeX - SCROLLBAR_WIDTH && mouseX <= this.x + this.sizeX
                                 && mouseY >= this.y && mouseY <= this.y + this.sizeY;

        this.elements.forEach(element -> element.mouseDown(this.offsetX(mouseX), this.offsetY(mouseY), button));
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        this.draggingScrollbar = false;

        this.elements.forEach(element -> element.mouseUp(this.offsetX(mouseX), this.offsetY(mouseY), button));
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        if (this.draggingScrollbar) {
            int dy = newMouseY - oldMouseY;
            double scaleFactor = (double) this.sizeY / (double) this.totalHeight;
            this.deltaY = clamp(this.deltaY + floorI((double) dy / scaleFactor), 0, this.totalHeight - this.sizeY);
        }

        this.elements.forEach(element -> element.mouseDragged(this.offsetX(oldMouseX), this.offsetY(oldMouseY), this.offsetX(newMouseX), this.offsetY(newMouseY), button));
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        if (mouseX >= this.x && mouseX <= this.x + this.sizeX && mouseY >= this.y && mouseY <= this.y + this.sizeY) {
            this.deltaY = clamp(this.deltaY - dWheel, 0, max(this.totalHeight - this.sizeY, 0));
        }

        this.elements.forEach(element -> element.mouseScroll(this.offsetX(mouseX), this.offsetY(mouseY), dWheel));
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        this.elements.forEach(element -> element.keyPressed(typedChar, keyCode));
    }
}
