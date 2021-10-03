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

import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.config.gui.GuiObjectAccess;
import net.daporkchop.fp2.config.gui.IConfigGuiElement;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.fp2.config.gui.container.ColumnsContainer;
import net.daporkchop.fp2.config.gui.container.ScrollingContainer;
import net.daporkchop.fp2.config.gui.container.TableContainer;
import net.daporkchop.fp2.config.gui.container.VerticallyStackedContainer;
import net.daporkchop.fp2.config.gui.screen.DefaultConfigGuiScreen;
import net.daporkchop.fp2.config.gui.util.ComponentDimensions;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.lib.common.util.PArrays;
import net.minecraft.client.resources.I18n;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.config.gui.GuiConstants.*;

/**
 * @author DaPorkchop_
 */
public class GuiRenderModeButton extends GuiSubmenuButton<FP2Config, String[]> {
    public GuiRenderModeButton(@NonNull IGuiContext context, @NonNull GuiObjectAccess<FP2Config> access, @NonNull Field field) {
        super(context, access, field);
    }

    @Override
    protected void handleClick(int button) {
        if (button == 0) { //left-click
            GuiObjectAccess<String[]> access = this.access.child(this.field);

            this.context.pushSubmenu(this.field.getName(), access, context -> new DefaultConfigGuiScreen(context,
                    new ScrollingContainer<>(context, access, Collections.singletonList(
                            new ColumnsContainer<>(context, access, Arrays.asList(
                                    new DisabledContainer(context, access),
                                    new EnabledContainer(context, access)))))));
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected class EnabledContainer extends VerticallyStackedContainer<String[]> {
        public EnabledContainer(@NonNull IGuiContext context, @NonNull GuiObjectAccess<String[]> access) {
            super(context, access, new ArrayList<>());
        }

        @Override
        public void init() {
            String[] modes = GuiRenderModeButton.this.get();

            this.elements.clear();
            this.elements.add(new GuiTitle(this.context, "renderModesEnabled"));
            this.elements.add(modes.length == 0
                    ? new GuiTitle(this.context, "noRenderModesEnabled")
                    : new TableContainer<>(this.context, this.access,
                    Stream.of(modes)
                            .map(mode -> new IConfigGuiElement[]{
                                    new SquareButton(this.context, this.access, GuiRenderModeButton.this.field, "listRemove") {
                                        @Override
                                        protected void handleClick(int button) {
                                            if (button == 0) { //left-click
                                                GuiRenderModeButton.this.set(Stream.of(GuiRenderModeButton.this.get())
                                                        .filter(((Predicate<String>) mode::equals).negate())
                                                        .toArray(String[]::new));

                                                this.context.pack();
                                            }
                                        }
                                    },
                                    new SquareButton(this.context, this.access, GuiRenderModeButton.this.field, "listUp") {
                                        {
                                            if (PArrays.indexOf(modes, mode) == 0) {
                                                this.button.enabled = false;
                                            }
                                        }

                                        @Override
                                        protected void handleClick(int button) {
                                            if (button == 0) { //left-click
                                                int oldIndex = PArrays.indexOf(modes, mode);
                                                PArrays.swap(modes, oldIndex, max(oldIndex - 1, 0));

                                                this.context.pack();
                                            }
                                        }
                                    },
                                    new SquareButton(this.context, this.access, GuiRenderModeButton.this.field, "listDown") {
                                        {
                                            if (PArrays.indexOf(modes, mode) == modes.length - 1) {
                                                this.button.enabled = false;
                                            }
                                        }

                                        @Override
                                        protected void handleClick(int button) {
                                            if (button == 0) { //left-click
                                                int oldIndex = PArrays.indexOf(modes, mode);
                                                PArrays.swap(modes, oldIndex, min(oldIndex + 1, modes.length - 1));

                                                this.context.pack();
                                            }
                                        }
                                    },
                                    new GuiNoopPaddingElement(new ComponentDimensions(0, 0)),
                                    new GuiLabel(this.context, mode, GuiLabel.Alignment.LEFT, GuiLabel.Alignment.CENTER),
                            })
                            .toArray(IConfigGuiElement[][]::new)));

            super.init();
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected class DisabledContainer extends VerticallyStackedContainer<String[]> {
        public DisabledContainer(@NonNull IGuiContext context, @NonNull GuiObjectAccess<String[]> access) {
            super(context, access, new ArrayList<>());
        }

        @Override
        public void init() {
            String[] modes = IFarRenderMode.REGISTRY.stream()
                    .map(Map.Entry::getKey)
                    .filter(((Predicate<String>) ImmutableSet.copyOf(GuiRenderModeButton.this.get())::contains).negate())
                    .toArray(String[]::new);

            this.elements.clear();
            this.elements.add(new GuiTitle(this.context, "renderModesDisabled"));
            this.elements.add(modes.length == 0
                    ? new GuiTitle(this.context, "noRenderModesDisabled")
                    : new TableContainer<>(this.context, this.access,
                    Stream.of(modes)
                            .map(mode -> new IConfigGuiElement[]{
                                    new SquareButton(this.context, this.access, GuiRenderModeButton.this.field, "listAdd") {
                                        @Override
                                        protected void handleClick(int button) {
                                            if (button == 0) { //left-click
                                                String[] oldModes = GuiRenderModeButton.this.get();
                                                String[] newModes = Arrays.copyOf(oldModes, oldModes.length + 1);
                                                newModes[oldModes.length] = mode;
                                                GuiRenderModeButton.this.set(newModes);

                                                this.context.pack();
                                            }
                                        }
                                    },
                                    new GuiNoopPaddingElement(new ComponentDimensions(0, 0)),
                                    new GuiLabel(this.context, mode, GuiLabel.Alignment.LEFT, GuiLabel.Alignment.CENTER),
                            })
                            .toArray(IConfigGuiElement[][]::new)));

            super.init();
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected abstract class SquareButton extends GuiButton<String[], String> {
        protected final String name;

        public SquareButton(@NonNull IGuiContext context, @NonNull GuiObjectAccess<String[]> access, @NonNull Field field, @NonNull String name) {
            super(context, access, field);

            this.name = name;
        }

        @Override
        public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
            return Stream.of(new ComponentDimensions(min(totalSizeX, BUTTON_HEIGHT), min(totalSizeY, BUTTON_HEIGHT)));
        }

        @Override
        public ComponentDimensions preferredMinimumDimensions() {
            return new ComponentDimensions(BUTTON_HEIGHT, BUTTON_HEIGHT);
        }

        @Override
        protected String langKey() {
            return this.context.localeKeyBase() + this.name;
        }

        @Override
        protected String localizeValue(String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected String text() {
            return this.localizedName();
        }

        @Override
        protected void computeTooltipText0(@NonNull StringJoiner joiner) {
            { //tooltip text from locale
                String tooltipKey = this.langKey() + ".tooltip";
                if (I18n.hasKey(tooltipKey)) {
                    joiner.add(I18n.format(tooltipKey));
                }
            }
        }
    }
}
