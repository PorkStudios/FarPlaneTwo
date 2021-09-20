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

package net.daporkchop.fp2.config.gui.element;

import lombok.NonNull;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.fp2.config.gui.container.ScrollingContainer;
import net.daporkchop.fp2.config.gui.screen.DefaultConfigGuiScreen;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.client.resources.I18n;

import java.lang.reflect.Field;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.FP2.*;

/**
 * @author DaPorkchop_
 */
public class GuiEnumButton extends GuiButton<Enum> {
    protected final Enum[] values;

    public GuiEnumButton(@NonNull IGuiContext context, Object instance, @NonNull Field field) {
        super(context, instance, field);

        this.values = PorkUtil.<Class<Enum>>uncheckedCast(this.get().getDeclaringClass()).getEnumConstants();
    }

    @Override
    protected String buttonText() {
        Enum value = this.get();
        return I18n.format(MODID + ".config.enum.format", super.buttonText(), I18n.format(PorkUtil.className(value) + '.' + value));
    }

    @Override
    protected void handleClick(int button) {
        if (button == 0) { //left-click
            Enum currentValue = this.get();

            this.context.pushSubmenu(this.field.getName(), context -> new DefaultConfigGuiScreen(context, new ScrollingContainer(Stream.of(this.values)
                    .map(value -> new ValueSelectionButton(context, value, value == currentValue))
                    .collect(Collectors.toList()))));
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected class ValueSelectionButton extends GuiButton<Enum> {
        protected final boolean current;

        public ValueSelectionButton(@NonNull IGuiContext context, @NonNull Enum value, boolean current) {
            super(context, null, Stream.of(value.getDeclaringClass().getDeclaredFields()).filter(field -> field.getName().equals(value.name())).findFirst().get());

            this.current = current;
        }

        @Override
        protected String langKey() {
            Enum value = this.get();
            return PorkUtil.className(value) + '.' + value;
        }

        @Override
        protected String buttonText() {
            return I18n.format(MODID + ".config.enum.selected." + this.current, super.buttonText());
        }

        @Override
        protected void handleClick(int button) {
            GuiEnumButton.this.set(this.get());
            this.context.pop();
        }
    }
}
