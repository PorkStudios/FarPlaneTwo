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

package net.daporkchop.fp2.config.gui.container;

import lombok.NonNull;
import net.daporkchop.fp2.config.gui.GuiObjectAccess;
import net.daporkchop.fp2.config.gui.IConfigGuiElement;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.fp2.config.gui.util.ComponentDimensions;
import net.daporkchop.fp2.config.gui.util.ElementBounds;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
public class ScrollingContainer<T> extends AbstractConfigGuiContainer<T> {
    protected static final Comparator<ComponentDimensions> COMPARATOR_LOWY_HIGHX = Comparator.comparingInt(ComponentDimensions::sizeY)
            .thenComparing(Comparator.comparingInt(ComponentDimensions::sizeX).reversed());

    protected int deltaY;
    protected int totalHeight;

    protected boolean draggingScrollbar = false;

    public ScrollingContainer(@NonNull IGuiContext context, @NonNull GuiObjectAccess<T> access, @NonNull List<IConfigGuiElement> elements) {
        super(context, access, elements);
    }

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        List<ComponentDimensions> possibleDimensions = new ArrayList<>();

        { //without scrollbar (height does make a difference here)
            List<ComponentDimensions> shortestDimensions = this.elements.stream()
                    .map(elem -> elem.possibleDimensions(totalSizeX, totalSizeY).min(COMPARATOR_LOWY_HIGHX))
                    .filter(Optional::isPresent).map(Optional::get)
                    .collect(Collectors.toList());

            int height = shortestDimensions.stream().mapToInt(ComponentDimensions::sizeY).reduce(0, (a, b) -> a + PADDING + b);
            if (!shortestDimensions.isEmpty() && height <= totalSizeY) { //removing the scrollbar is only possible if the total height is small enough
                possibleDimensions.add(new ComponentDimensions(
                        shortestDimensions.stream().mapToInt(ComponentDimensions::sizeX).max().getAsInt(),
                        height));
            }
        }

        { //with scrollbar (needs to be a bit narrower because of it, but height is irrelevant)
            List<ComponentDimensions> shortestDimensions = this.elements.stream()
                    .map(elem -> elem.possibleDimensions(totalSizeX - PADDING - SCROLLBAR_WIDTH, Integer.MAX_VALUE).min(COMPARATOR_LOWY_HIGHX).get())
                    .collect(Collectors.toList());

            possibleDimensions.add(new ComponentDimensions(
                    shortestDimensions.stream().mapToInt(ComponentDimensions::sizeX).max().getAsInt() + PADDING + SCROLLBAR_WIDTH,
                    min(shortestDimensions.stream().mapToInt(ComponentDimensions::sizeY).reduce(0, (a, b) -> a + PADDING + b), totalSizeY)));
        }

        return possibleDimensions.stream();
    }

    @Override
    public ComponentDimensions preferredMinimumDimensions() {
        return this.elements.stream()
                .map(IConfigGuiElement::preferredMinimumDimensions)
                .reduce((a, b) -> new ComponentDimensions(max(a.sizeX(), b.sizeX()), a.sizeY() + PADDING + b.sizeY()))
                .get();
    }

    @Override
    public void pack() {
        this.deltaY = 0; //reset scroll distance
        this.draggingScrollbar = false;

        //first pass: compute all element dimensions, and use that to determine whether we should use a scrollbar

        List<ComponentDimensions> elementDimensions = null;
        int totalHeight = -1;

        if (elementDimensions == null) { //without scrollbar (height does make a difference here)
            List<ComponentDimensions> shortestDimensions = this.elements.stream()
                    .map(elem -> elem.possibleDimensions(this.bounds.sizeX(), this.bounds.sizeY()).min(COMPARATOR_LOWY_HIGHX))
                    .filter(Optional::isPresent).map(Optional::get)
                    .collect(Collectors.toList());

            int height = shortestDimensions.stream().mapToInt(ComponentDimensions::sizeY).reduce(0, (a, b) -> a + PADDING + b);
            if (!shortestDimensions.isEmpty() && height <= this.bounds.sizeY()) { //removing the scrollbar is only possible if the total height is small enough
                elementDimensions = shortestDimensions;
                totalHeight = this.bounds.sizeY(); //no scrollbar is needed
            }
        }

        if (elementDimensions == null) { //with scrollbar (needs to be a bit narrower because of it, but height is irrelevant)
            List<ComponentDimensions> shortestDimensions = this.elements.stream()
                    .map(elem -> elem.possibleDimensions(this.bounds.sizeX() - PADDING - SCROLLBAR_WIDTH, Integer.MAX_VALUE).min(COMPARATOR_LOWY_HIGHX).get())
                    .collect(Collectors.toList());

            elementDimensions = shortestDimensions;
            totalHeight = shortestDimensions.stream().mapToInt(ComponentDimensions::sizeY).reduce(0, (a, b) -> a + PADDING + b);
        }

        //second pass: update fields and bounds and decide how to arrange the elements

        this.totalHeight = totalHeight;

        for (int y = 0, i = 0; i < this.elements.size(); i++) {
            IConfigGuiElement element = this.elements.get(i);
            ComponentDimensions dimensions = elementDimensions.get(i);

            element.bounds(new ElementBounds((this.bounds.sizeX() - dimensions.sizeX()) >> 1, y, dimensions.sizeX(), dimensions.sizeY()));
            y += dimensions.sizeY() + PADDING;
        }
    }

    @Override
    protected int offsetY(int y) {
        return super.offsetY(y) + this.deltaY;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution scaledResolution = new ScaledResolution(MC);
        int scale = scaledResolution.getScaleFactor();
        glScissor(this.bounds.x() * scale, (scaledResolution.getScaledHeight() - this.bounds.y() - this.bounds.sizeY()) * scale, this.bounds.sizeX() * scale, this.bounds.sizeY() * scale);
        glEnable(GL_SCISSOR_TEST);

        try {
            super.render(mouseX, mouseY, partialTicks);
        } finally {
            glDisable(GL_SCISSOR_TEST);
        }

        if (this.totalHeight > this.bounds.sizeY()) { //we need to draw a scrollbar
            double viewPort = (double) this.bounds.sizeY() / (double) this.totalHeight;
            int barHeight = floorI(this.bounds.sizeY() * viewPort);
            int barStart = floorI(((double) this.deltaY / (double) (this.totalHeight - this.bounds.sizeY())) * (this.bounds.sizeY() - barHeight));

            Gui.drawRect(this.bounds.x() + this.bounds.sizeX() - SCROLLBAR_WIDTH, this.bounds.y(), this.bounds.x() + this.bounds.sizeX(), this.bounds.y() + this.bounds.sizeY(), 0xB0000000);
            Gui.drawRect(this.bounds.x() + this.bounds.sizeX() - SCROLLBAR_WIDTH, this.bounds.y() + barStart, this.bounds.x() + this.bounds.sizeX(), this.bounds.y() + barStart + barHeight, 0xFFDDDDDD);
        }
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        this.draggingScrollbar = button == 0 //left-click
                                 && this.totalHeight > this.bounds.sizeY() //scrollbar is visible
                                 && mouseX >= this.bounds.x() + this.bounds.sizeX() - SCROLLBAR_WIDTH && mouseX <= this.bounds.x() + this.bounds.sizeX()
                                 && mouseY >= this.bounds.y() && mouseY <= this.bounds.y() + this.bounds.sizeY();

        super.mouseDown(mouseX, mouseY, button);
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        this.draggingScrollbar = false;

        super.mouseUp(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        if (this.draggingScrollbar) {
            int dy = newMouseY - oldMouseY;
            double scaleFactor = (double) this.bounds.sizeY() / (double) this.totalHeight;
            this.deltaY = clamp(this.deltaY + floorI((double) dy / scaleFactor), 0, this.totalHeight - this.bounds.sizeY());
        }

        super.mouseDragged(oldMouseX, oldMouseY, newMouseX, newMouseY, button);
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        if (this.bounds.contains(mouseX, mouseY)) {
            this.deltaY = clamp(this.deltaY + dWheel, 0, max(this.totalHeight - this.bounds.sizeY(), 0));
        }

        super.mouseScroll(mouseX, mouseY, dWheel);
    }
}
