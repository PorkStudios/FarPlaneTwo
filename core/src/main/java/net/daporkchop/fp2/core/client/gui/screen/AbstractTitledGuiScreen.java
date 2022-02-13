/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.client.gui.screen;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiElement;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.client.gui.container.AbstractConfigGuiContainer;
import net.daporkchop.fp2.core.client.gui.element.AbstractGuiButton;
import net.daporkchop.fp2.core.client.gui.element.properties.SimpleGuiElementProperties;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;
import net.daporkchop.fp2.core.client.gui.util.ElementBounds;

import java.util.Comparator;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * Base implementation of {@link GuiScreen} which renders headers and footers, and then delegates all other rendering to a child {@link GuiElement}.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractTitledGuiScreen extends AbstractGuiScreen {
    public static final int PADDING = AbstractConfigGuiContainer.PADDING;

    public static final int HEADER_HEIGHT = 30;
    public static final int FOOTER_HEIGHT = 27;

    protected final String localeKey;
    protected final GuiElement child;

    protected final GuiElement doneButton;

    public AbstractTitledGuiScreen(@NonNull GuiContext context, @NonNull String localeKey, @NonNull GuiElement child) {
        this(context, localeKey, "gui.done", child);
    }

    public AbstractTitledGuiScreen(@NonNull GuiContext context, @NonNull String localeKey, @NonNull String doneButtonLocaleKey, @NonNull GuiElement child) {
        super(context);

        this.localeKey = localeKey;
        this.child = child;

        this.doneButton = new AbstractGuiButton(context, new SimpleGuiElementProperties(doneButtonLocaleKey)) {
            @Override
            protected void handleClick(int button) {
                if (button == MOUSE_BUTTON_LEFT) {
                    this.context.close();
                }
            }
        };

        this.handlers.add(child);
        this.handlers.add(this.doneButton);
    }

    @Override
    public void pack() {
        ComponentDimensions dimensions = this.child.possibleDimensions(this.dimensions.sizeX() - (PADDING << 1), this.dimensions.sizeY() - (HEADER_HEIGHT + FOOTER_HEIGHT + (PADDING << 1)))
                .min(Comparator.comparingInt(ComponentDimensions::sizeY)).get(); //find the shortest possible dimensions
        this.child.bounds(new ElementBounds((this.dimensions.sizeX() - dimensions.sizeX()) >> 1, HEADER_HEIGHT + PADDING, dimensions.sizeX(), dimensions.sizeY()));

        this.doneButton.bounds(new ElementBounds(
                (this.dimensions.sizeX() - AbstractGuiButton.BUTTON_WIDTH) >> 1, this.dimensions.sizeY() - FOOTER_HEIGHT,
                AbstractGuiButton.BUTTON_WIDTH, AbstractGuiButton.BUTTON_HEIGHT));
    }

    protected String localizedTitleString() {
        return fp2().i18n().format(this.localeKey);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        this.context.renderer().drawCenteredString(this.localizedTitleString(), this.dimensions.sizeX() >> 1, HEADER_HEIGHT >> 1, -1, true, true, true);
        this.context.renderer().drawDefaultBackground(0, HEADER_HEIGHT, this.dimensions.sizeX(), this.dimensions.sizeY() - HEADER_HEIGHT - FOOTER_HEIGHT - PADDING);

        super.render(mouseX, mouseY);
    }
}
