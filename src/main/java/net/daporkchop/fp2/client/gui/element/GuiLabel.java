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
import net.daporkchop.fp2.client.gui.IConfigGuiContext;
import net.daporkchop.fp2.core.client.gui.element.AbstractGuiElement;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;
import net.minecraft.client.resources.I18n;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class GuiLabel extends AbstractGuiElement {
    protected final Supplier<String> langKeyFactory;
    protected final Supplier<String> textFactory;

    protected final Alignment horizontalAlignment;
    protected final Alignment verticalAlignment;

    protected String text;
    protected int textWidth;
    protected int textHeight;

    public GuiLabel(@NonNull IConfigGuiContext context, @NonNull String name, @NonNull Alignment horizontalAlignment, @NonNull Alignment verticalAlignment) {
        super(context);

        this.langKeyFactory = () -> context.localeKeyBase() + name;
        this.textFactory = () -> I18n.format(this.langKey());

        this.horizontalAlignment = horizontalAlignment;
        this.verticalAlignment = verticalAlignment;
    }

    public GuiLabel(@NonNull IConfigGuiContext context, @NonNull Supplier<String> textFactory, @NonNull Alignment horizontalAlignment, @NonNull Alignment verticalAlignment) {
        super(context);

        this.langKeyFactory = () -> context.localeKeyBase().substring(0, max(context.localeKeyBase().length() - 1, 0));
        this.textFactory = textFactory;

        this.horizontalAlignment = horizontalAlignment;
        this.verticalAlignment = verticalAlignment;
    }

    @Override
    protected String langKey() {
        return this.langKeyFactory.get();
    }

    @Override
    public void init() {
        this.text = this.textFactory.get();
        this.textWidth = MC.fontRenderer.getStringWidth(this.text);
        this.textHeight = MC.fontRenderer.FONT_HEIGHT;
    }

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        return Stream.of(new ComponentDimensions(totalSizeX, min(this.textHeight, totalSizeY)));
    }

    @Override
    public ComponentDimensions preferredMinimumDimensions() {
        return new ComponentDimensions(this.textWidth, this.textHeight);
    }

    @Override
    public void pack() {
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        MC.fontRenderer.drawStringWithShadow(this.text,
                this.horizontalAlignment.align(this.bounds.x(), this.bounds.sizeX(), this.textWidth),
                this.verticalAlignment.align(this.bounds.y(), this.bounds.sizeY(), this.textHeight),
                -1);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
    }

    /**
     * @author DaPorkchop_
     */
    public enum Alignment {
        LEFT {
            @Override
            public int align(int boundsBase, int boundsSize, int textSize) {
                return boundsBase;
            }
        },
        TOP {
            @Override
            public int align(int boundsBase, int boundsSize, int textSize) {
                return LEFT.align(boundsBase, boundsSize, textSize);
            }
        },
        CENTER {
            @Override
            public int align(int boundsBase, int boundsSize, int textSize) {
                return boundsBase + ((boundsSize - textSize) >> 1);
            }
        },
        RIGHT {
            @Override
            public int align(int boundsBase, int boundsSize, int textSize) {
                return boundsBase + boundsSize - textSize;
            }
        },
        BOTTOM {
            @Override
            public int align(int boundsBase, int boundsSize, int textSize) {
                return RIGHT.align(boundsBase, boundsSize, textSize);
            }
        };

        public abstract int align(int boundsBase, int boundsSize, int textSize);
    }
}
