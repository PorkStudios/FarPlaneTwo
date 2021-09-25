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
import net.daporkchop.fp2.config.gui.GuiObjectAccess;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.fp2.config.gui.container.ScrollingContainer;
import net.daporkchop.fp2.config.gui.screen.DefaultConfigGuiScreen;
import net.minecraft.client.resources.I18n;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.FP2.*;

/**
 * @author DaPorkchop_
 */
public class GuiEnumButton<T, E extends Enum<E>> extends GuiButton<T, E> {
    protected final E[] values;
    protected final Field[] fields;

    public GuiEnumButton(@NonNull IGuiContext context, @NonNull GuiObjectAccess<T> access, @NonNull Field field) {
        super(context, access, field);

        Class<E> enumClazz = this.get().getDeclaringClass();

        this.values = enumClazz.getEnumConstants();
        this.fields = Stream.of(this.values)
                .map(E::name)
                .map(Stream.of(enumClazz.getDeclaredFields()).collect(Collectors.toMap(Field::getName, Function.identity()))::get)
                .peek(Objects::requireNonNull)
                .toArray(Field[]::new);
    }

    @Override
    protected String localizeValue(E value) {
        return I18n.format(value.getDeclaringClass().getTypeName() + '#' + value);
    }

    @Override
    protected void handleClick(int button) {
        if (button == 0) { //left-click
            GuiObjectAccess<E> access = GuiObjectAccess.forStatic();

            this.context.pushSubmenu(this.field.getName(), access, context -> new DefaultConfigGuiScreen(context, new ScrollingContainer<>(access, Stream.of(this.values)
                    .map(value -> new ValueSelectionButton(context, value))
                    .collect(Collectors.toList()))));
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected class ValueSelectionButton extends GuiButton<E, E> {
        protected final E value;

        public ValueSelectionButton(@NonNull IGuiContext context, @NonNull E value) {
            super(context, GuiObjectAccess.forStatic(), GuiEnumButton.this.fields[value.ordinal()]);

            this.value = value;
        }

        @Override
        protected String langKey() {
            return this.value.getDeclaringClass().getTypeName() + '#' + this.value;
        }

        @Override
        protected String text() {
            return I18n.format(MODID + ".config.enum.selected." + (this.value == GuiEnumButton.this.get()), this.localizeValue(this.value));
        }

        @Override
        protected String localizeValue(E value) {
            return I18n.format(this.value.getDeclaringClass().getTypeName() + '#' + value);
        }

        @Override
        protected void handleClick(int button) {
            GuiEnumButton.this.set(this.value);
            this.context.pop();
        }
    }
}
