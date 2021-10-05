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
import net.daporkchop.fp2.client.gui.IGuiContext;
import net.daporkchop.fp2.client.gui.access.GuiObjectAccess;
import net.daporkchop.fp2.client.gui.container.ColumnsContainer;
import net.daporkchop.fp2.client.gui.container.ScrollingContainer;
import net.daporkchop.fp2.client.gui.screen.DefaultConfigGuiScreen;
import net.daporkchop.fp2.config.Config;
import net.daporkchop.fp2.config.ConfigHelper;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public abstract class AbstractReflectiveConfigGuiElement<V> extends AbstractConfigGuiElement {
    protected final GuiObjectAccess<V> access;

    public AbstractReflectiveConfigGuiElement(@NonNull IGuiContext context, @NonNull GuiObjectAccess<V> access) {
        super(context);

        this.access = access;
    }

    protected boolean allowMoreOptions() {
        return true;
    }

    @Override
    protected String langKey() {
        return this.context.localeKeyBase() + this.access.name();
    }

    protected String text() {
        return I18n.format(MODID + ".config.property.format", this.localizedName(), this.localizeValue(this.access.getCurrent()));
    }

    protected abstract String localizeValue(@NonNull V value);

    @Override
    protected void computeTooltipText0(@NonNull StringJoiner joiner) {
        super.computeTooltipText0(joiner);

        { //indicator for this option's old/default values
            String text = Stream.of(
                    Optional.ofNullable(this.localizeValue(this.access.getOld())).map(val -> I18n.format(MODID + ".config.old.tooltip", val)).orElse(null),
                    Optional.ofNullable(this.localizeValue(this.access.getDefault())).map(val -> I18n.format(MODID + ".config.default.tooltip", val)).orElse(null),
                    this.access.getAnnotation(Config.GuiShowServerValue.class) != null ? Optional.ofNullable(this.access.getServer()).map(this::localizeValue).map(val -> I18n.format(MODID + ".config.server.tooltip", val)).orElse(null) : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));

            if (!text.isEmpty()) {
                joiner.add(text);
            }
        }

        { //indicator for this option's value range
            Config.Range range = this.access.getAnnotation(Config.Range.class);
            Config.GuiRange guiRange = this.access.getAnnotation(Config.GuiRange.class);
            if (range != null || guiRange != null) {
                V min = uncheckedCast(ConfigHelper.evaluate(guiRange != null ? guiRange.min() : range.min()));
                V max = uncheckedCast(ConfigHelper.evaluate(guiRange != null ? guiRange.max() : range.max()));

                joiner.add(I18n.format(MODID + ".config.range.tooltip", this.localizeValue(min), this.localizeValue(max)));
            }
        }

        { //indicator for whether this option requires a restart
            Config.RestartRequired restartRequired = this.access.getAnnotation(Config.RestartRequired.class);
            if (restartRequired != null) {
                joiner.add(I18n.format(MODID + ".config.restartRequired.tooltip." + restartRequired.value()));
            }
        }

        if (this.allowMoreOptions()) { //indicator to open "more options" menu
            joiner.add(I18n.format(MODID + ".config.moreOptions.tooltip"));
        }
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (button == 1 && this.bounds.contains(mouseX, mouseY) && this.allowMoreOptions()) {
            class ResetButton extends GuiButton<V> {
                final String name;
                final V value;

                public ResetButton(@NonNull IGuiContext context, @NonNull GuiObjectAccess<V> access, @NonNull String name, @NonNull V value) {
                    super(context, access);

                    this.name = name;
                    this.value = value;
                }

                @Override
                protected boolean allowMoreOptions() {
                    return false;
                }

                @Override
                protected String langKey() {
                    return MODID + ".config.moreOptions." + this.name;
                }

                @Override
                protected String localizeValue(@NonNull V value) {
                    return AbstractReflectiveConfigGuiElement.this.localizeValue(value);
                }

                @Override
                protected Optional<String[]> computeTooltipText() {
                    return Optional.empty();
                }

                @Override
                protected void handleClick(int button) {
                    if (button == 0) { //left-click
                        AbstractReflectiveConfigGuiElement.this.access.setCurrent(this.value);
                        this.context.pop();
                    }
                }
            }

            this.context.pushSubmenu(this.context.localeKeyBase(), this.access, context -> new DefaultConfigGuiScreen(context, new ScrollingContainer<>(context, this.access, Collections.singletonList(
                    new ColumnsContainer<>(context, this.access, Arrays.asList(
                            new ResetButton(context, this.access, "resetOld", this.access.getOld()),
                            new ResetButton(context, this.access, "resetDefault", this.access.getDefault())))))) {
                @Override
                protected String getTitleString() {
                    return AbstractReflectiveConfigGuiElement.this.text();
                }
            });
        }
    }
}
