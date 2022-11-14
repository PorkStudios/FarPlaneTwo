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

package net.daporkchop.fp2.core.config.gui.element;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.element.AbstractGuiButton;
import net.daporkchop.fp2.core.client.gui.element.AbstractGuiSlider;
import net.daporkchop.fp2.core.client.gui.element.properties.GuiElementProperties;
import net.daporkchop.fp2.core.config.gui.access.ConfigGuiObjectAccess;

import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class ConfigGuiSlider extends AbstractGuiSlider {
    protected final ConfigGuiObjectAccess<Number> access;

    public ConfigGuiSlider(@NonNull GuiContext context, @NonNull GuiElementProperties properties, @NonNull ConfigGuiObjectAccess<Number> access) {
        super(context, properties, access.getCurrent());

        this.access = access;
    }

    @Override
    protected void drawBackground(int mouseX, int mouseY) {
        super.drawBackground(mouseX, mouseY);

        Number serverValue = this.access.getServer();
        if (serverValue != null) {
            int color = 0x44FF0000;
            this.context.renderer().drawQuadColored(
                    this.bounds.x() + 1 + floorI((serverValue.doubleValue() - this.min) / (this.max - this.min) * (this.bounds.sizeX() - 2)),
                    this.bounds.y() + 1, this.bounds.sizeX() - 2, this.bounds.sizeY() - 2, color);
        }
    }

    @Override
    protected void valueChanged(@NonNull Number value) {
        Class<?> clazz = this.access.type();
        if (clazz == int.class || clazz == Integer.class) {
            this.access.setCurrent(value.intValue());
        } else if (clazz == long.class || clazz == Long.class) {
            this.access.setCurrent(value.longValue());
        } else if (clazz == float.class || clazz == Float.class) {
            this.access.setCurrent(value.floatValue());
        } else if (clazz == double.class || clazz == Double.class) {
            this.access.setCurrent(value.doubleValue());
        } else {
            throw new IllegalArgumentException(clazz.getTypeName());
        }
    }
}
