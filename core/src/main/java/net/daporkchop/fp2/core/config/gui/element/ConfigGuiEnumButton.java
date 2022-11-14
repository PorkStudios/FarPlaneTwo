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

package net.daporkchop.fp2.core.config.gui.element;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.container.ScrollingContainer;
import net.daporkchop.fp2.core.client.gui.element.AbstractGuiButton;
import net.daporkchop.fp2.core.client.gui.element.properties.AbstractGuiElementProperties;
import net.daporkchop.fp2.core.client.gui.element.properties.GuiElementProperties;
import net.daporkchop.fp2.core.client.gui.screen.AbstractTitledGuiScreen;
import net.daporkchop.fp2.core.config.gui.access.ConfigGuiObjectAccess;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
public class ConfigGuiEnumButton<E extends Enum<E>> extends AbstractGuiButton {
    protected final ConfigGuiObjectAccess<E> access;

    public ConfigGuiEnumButton(@NonNull GuiContext context, @NonNull GuiElementProperties properties, @NonNull ConfigGuiObjectAccess<E> access) {
        super(context, properties);

        this.access = access;
    }

    @Override
    protected void handleClick(int button) {
        if (button == MOUSE_BUTTON_LEFT) {
            fp2().openScreen(context -> new AbstractTitledGuiScreen(context, this.properties.localeKey(),
                    new ScrollingContainer(context, Stream.of(this.access.getCurrent().getDeclaringClass().getEnumConstants())
                            .map(value -> new ValueSelectionButton(context, new EnumFieldProperties(value), value))
                            .collect(Collectors.toList()))) {
                @Override
                public void close() {
                    //no-op
                }
            });
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected class EnumFieldProperties extends AbstractGuiElementProperties {
        @NonNull
        protected final E value;

        @Override
        public String localeKey() {
            return this.value.getDeclaringClass().getTypeName() + '#' + this.value.name();
        }

        @Override
        public String text() {
            return fp2().i18n().format(MODID + ".config.enum.selected." + (this.value == ConfigGuiEnumButton.this.access.getCurrent()), this.name());
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected class ValueSelectionButton extends AbstractGuiButton {
        protected final E value;

        public ValueSelectionButton(@NonNull GuiContext context, @NonNull GuiElementProperties properties, @NonNull E value) {
            super(context, properties);

            this.value = value;
        }

        @Override
        protected void handleClick(int button) {
            if (button == MOUSE_BUTTON_LEFT) {
                ConfigGuiEnumButton.this.access.setCurrent(this.value);
                this.context.close();
            }
        }
    }
}
