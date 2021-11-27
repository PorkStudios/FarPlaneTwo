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

package net.daporkchop.fp2.core.client.gui.element;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.element.properties.GuiElementProperties;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;

import java.util.stream.Stream;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
public class GuiLabel extends AbstractGuiElement {
    protected final Alignment horizontalAlignment;
    protected final Alignment verticalAlignment;

    public GuiLabel(@NonNull GuiContext context, @NonNull GuiElementProperties properties, @NonNull Alignment horizontalAlignment, @NonNull Alignment verticalAlignment) {
        super(context, properties);

        this.horizontalAlignment = horizontalAlignment;
        this.verticalAlignment = verticalAlignment;
    }

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        return Stream.of(new ComponentDimensions(totalSizeX, min(this.context.renderer().getStringWidth(this.properties.text()), totalSizeY)));
    }

    @Override
    public ComponentDimensions preferredMinimumDimensions() {
        return new ComponentDimensions(this.context.renderer().getStringWidth(this.properties.text()), this.context.renderer().getStringHeight());
    }

    @Override
    public void render(int mouseX, int mouseY) {
        super.render(mouseX, mouseY);

        String text = this.properties.text();
        this.context.renderer().drawString(text,
                this.horizontalAlignment.align(this.bounds.x(), this.bounds.sizeX(), this.context.renderer().getStringWidth(text)),
                this.verticalAlignment.align(this.bounds.y(), this.bounds.sizeY(), this.context.renderer().getStringHeight()),
                -1, true);
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
