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
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.element.AbstractGuiButton;
import net.daporkchop.fp2.core.client.gui.element.properties.GuiElementProperties;
import net.daporkchop.fp2.core.config.gui.ConfigGuiScreen;
import net.daporkchop.fp2.core.config.gui.access.ConfigGuiObjectAccess;

import java.util.Optional;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
public class ConfigGuiSubmenuButton extends AbstractGuiButton {
    protected final ConfigGuiObjectAccess<?> access;

    public ConfigGuiSubmenuButton(@NonNull GuiContext context, @NonNull GuiElementProperties properties, @NonNull ConfigGuiObjectAccess<?> access) {
        super(context, new NameIsTextElementPropertiesWrapper(properties));

        this.access = access;
    }

    @Override
    protected void handleClick(int button) {
        if (button == MOUSE_BUTTON_LEFT) {
            fp2().openScreen(context -> new ConfigGuiScreen(context, this.properties.localeKey(), this.access));
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class NameIsTextElementPropertiesWrapper implements GuiElementProperties {
        @NonNull
        protected final GuiElementProperties delegate;

        @Override
        public String localeKey() {
            return this.delegate.localeKey();
        }

        @Override
        public String name() {
            return this.delegate.name();
        }

        @Override
        public String text() {
            return this.delegate.name();
        }

        @Override
        public Optional<String[]> tooltip() {
            return this.delegate.tooltip();
        }

        @Override
        public Optional<Number> min() {
            return this.delegate.min();
        }

        @Override
        public Optional<Number> max() {
            return this.delegate.max();
        }

        @Override
        public Optional<Number> step() {
            return this.delegate.step();
        }
    }
}
