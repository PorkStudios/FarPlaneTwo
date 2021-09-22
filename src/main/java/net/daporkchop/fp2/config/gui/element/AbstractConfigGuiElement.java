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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.config.gui.IConfigGuiElement;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.fp2.config.gui.util.ElementBounds;
import net.minecraft.client.resources.I18n;

import java.util.Optional;
import java.util.StringJoiner;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractConfigGuiElement implements IConfigGuiElement {
    @NonNull
    protected final IGuiContext context;

    @Getter
    protected ElementBounds bounds = ElementBounds.ZERO;

    @Getter(lazy = true)
    private final String localizedName = I18n.format(this.langKey());

    @Getter(lazy = true)
    private final Optional<String[]> tooltipText = this.computeTooltipText();

    protected abstract String langKey();

    protected Optional<String[]> computeTooltipText() {
        StringJoiner joiner = new StringJoiner("\n\n");
        this.computeTooltipText0(joiner);

        return joiner.length() == 0 ? Optional.empty() : Optional.of(joiner.toString().split("\n"));
    }

    protected void computeTooltipText0(@NonNull StringJoiner joiner) {
        { //tooltip text from locale
            String tooltipKey = this.langKey() + ".tooltip";
            if (I18n.hasKey(tooltipKey)) {
                joiner.add(I18n.format(tooltipKey));
            }
        }
    }

    @Override
    public void bounds(@NonNull ElementBounds bounds) {
        this.bounds = bounds;

        this.pack();
    }

    @Override
    public Optional<String[]> getTooltip(int mouseX, int mouseY) {
        return this.bounds.contains(mouseX, mouseY) ? this.tooltipText() : Optional.empty();
    }
}
