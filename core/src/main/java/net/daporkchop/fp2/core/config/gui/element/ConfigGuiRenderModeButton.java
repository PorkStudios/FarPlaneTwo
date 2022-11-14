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

import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiElement;
import net.daporkchop.fp2.core.client.gui.container.ColumnsContainer;
import net.daporkchop.fp2.core.client.gui.container.ScrollingContainer;
import net.daporkchop.fp2.core.client.gui.container.TableContainer;
import net.daporkchop.fp2.core.client.gui.container.VerticallyStackedContainer;
import net.daporkchop.fp2.core.client.gui.element.AbstractGuiButton;
import net.daporkchop.fp2.core.client.gui.element.GuiLabel;
import net.daporkchop.fp2.core.client.gui.element.GuiNoopPaddingElement;
import net.daporkchop.fp2.core.client.gui.element.GuiTitle;
import net.daporkchop.fp2.core.client.gui.element.properties.GuiElementProperties;
import net.daporkchop.fp2.core.client.gui.element.properties.SimpleGuiElementProperties;
import net.daporkchop.fp2.core.client.gui.screen.AbstractTitledGuiScreen;
import net.daporkchop.fp2.core.client.gui.util.ComponentDimensions;
import net.daporkchop.fp2.core.config.gui.access.ConfigGuiObjectAccess;
import net.daporkchop.lib.common.util.PArrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
public class ConfigGuiRenderModeButton extends AbstractGuiButton {
    protected final ConfigGuiObjectAccess<String[]> access;
    protected final Set<String> serverModes;

    public ConfigGuiRenderModeButton(@NonNull GuiContext context, @NonNull GuiElementProperties properties, @NonNull ConfigGuiObjectAccess<String[]> access) {
        super(context, new ConfigGuiSubmenuButton.NameIsTextElementPropertiesWrapper(properties));

        this.access = access;
        this.serverModes = ImmutableSet.copyOf(Optional.ofNullable(access.getServer()).orElse(fp2().renderModeNames()));
    }

    @Override
    protected void handleClick(int button) {
        if (button == MOUSE_BUTTON_LEFT) {
            fp2().openScreen(context -> new AbstractTitledGuiScreen(context, this.properties.localeKey(),
                    new ScrollingContainer(context, Collections.singletonList(
                            new ColumnsContainer(context, Arrays.asList(
                                    new DisabledContainer(context),
                                    new EnabledContainer(context)))))) {
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
    protected class DisabledContainer extends VerticallyStackedContainer {
        public DisabledContainer(@NonNull GuiContext context) {
            super(context, new ArrayList<>());
        }

        @Override
        public void init() {
            String[] modes = Stream.of(fp2().renderModeNames())
                    .filter(((Predicate<String>) ImmutableSet.copyOf(ConfigGuiRenderModeButton.this.access.getCurrent())::contains).negate())
                    .toArray(String[]::new);

            this.elements.clear();
            this.elements.add(new GuiTitle(this.context, new SimpleGuiElementProperties(ConfigGuiRenderModeButton.this.properties.localeKey() + ".renderModesDisabled")));
            this.elements.add(new TableContainer(this.context, modes.length == 0 ?
                    new GuiElement[][]{
                            {
                                    new GuiNoopPaddingElement(this.context, new ComponentDimensions(0, BUTTON_HEIGHT)),
                                    new GuiLabel(this.context, new SimpleGuiElementProperties(ConfigGuiRenderModeButton.this.properties.localeKey() + ".noRenderModesDisabled"), GuiLabel.Alignment.CENTER, GuiLabel.Alignment.CENTER),
                                    new GuiNoopPaddingElement(this.context, new ComponentDimensions(0, BUTTON_HEIGHT)),
                            }
                    }
                    : Stream.of(modes)
                    .map(mode -> new GuiElement[]{
                            new SquareButton(this.context, "listAdd") {
                                @Override
                                protected void handleClick(int button) {
                                    if (button == 0) { //left-click
                                        String[] oldModes = ConfigGuiRenderModeButton.this.access.getCurrent();
                                        String[] newModes = Arrays.copyOf(oldModes, oldModes.length + 1);
                                        newModes[oldModes.length] = mode;
                                        ConfigGuiRenderModeButton.this.access.setCurrent(newModes);

                                        this.context.reinit();
                                    }
                                }
                            },
                            new GuiNoopPaddingElement(this.context, new ComponentDimensions(0, 0)),
                            new ModeLabel(this.context, mode),
                    })
                    .toArray(GuiElement[][]::new)));

            super.init();
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected class EnabledContainer extends VerticallyStackedContainer {
        public EnabledContainer(@NonNull GuiContext context) {
            super(context, new ArrayList<>());
        }

        @Override
        public void init() {
            String[] modes = ConfigGuiRenderModeButton.this.access.getCurrent();

            this.elements.clear();
            this.elements.add(new GuiTitle(this.context, new SimpleGuiElementProperties(ConfigGuiRenderModeButton.this.properties.localeKey() + ".renderModesEnabled")));
            this.elements.add(new TableContainer(this.context, modes.length == 0 ?
                    new GuiElement[][]{
                            {
                                    new GuiNoopPaddingElement(this.context, new ComponentDimensions(0, BUTTON_HEIGHT)),
                                    new GuiLabel(this.context, new SimpleGuiElementProperties(ConfigGuiRenderModeButton.this.properties.localeKey() + ".noRenderModesEnabled"), GuiLabel.Alignment.CENTER, GuiLabel.Alignment.CENTER),
                                    new GuiNoopPaddingElement(this.context, new ComponentDimensions(0, BUTTON_HEIGHT)),
                            }
                    }
                    : Stream.of(modes)
                    .map(mode -> new GuiElement[]{
                            new SquareButton(this.context, "listRemove") {
                                @Override
                                protected void handleClick(int button) {
                                    if (button == 0) { //left-click
                                        ConfigGuiRenderModeButton.this.access.setCurrent(Stream.of(ConfigGuiRenderModeButton.this.access.getCurrent())
                                                .filter(((Predicate<String>) mode::equals).negate())
                                                .toArray(String[]::new));

                                        this.context.reinit();
                                    }
                                }
                            },
                            new SquareButton(this.context, "listUp") {
                                {
                                    if (PArrays.linearSearch(modes, mode) == 0) {
                                        this.enabled = false;
                                    }
                                }

                                @Override
                                protected void handleClick(int button) {
                                    if (button == 0) { //left-click
                                        int oldIndex = PArrays.linearSearch(modes, mode);
                                        PArrays.swap(modes, oldIndex, max(oldIndex - 1, 0));

                                        this.context.reinit();
                                    }
                                }
                            },
                            new SquareButton(this.context, "listDown") {
                                {
                                    if (PArrays.linearSearch(modes, mode) == modes.length - 1) {
                                        this.enabled = false;
                                    }
                                }

                                @Override
                                protected void handleClick(int button) {
                                    if (button == 0) { //left-click
                                        int oldIndex = PArrays.linearSearch(modes, mode);
                                        PArrays.swap(modes, oldIndex, min(oldIndex + 1, modes.length - 1));

                                        this.context.reinit();
                                    }
                                }
                            },
                            new GuiNoopPaddingElement(this.context, new ComponentDimensions(0, 0)),
                            new ModeLabel(this.context, mode),
                    })
                    .toArray(GuiElement[][]::new)));

            super.init();
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected abstract class SquareButton extends AbstractGuiButton {
        public SquareButton(@NonNull GuiContext context, @NonNull String name) {
            super(context, new SimpleGuiElementProperties(ConfigGuiRenderModeButton.this.properties.localeKey() + '.' + name));
        }

        @Override
        public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
            return Stream.of(new ComponentDimensions(min(totalSizeX, BUTTON_HEIGHT), min(totalSizeY, BUTTON_HEIGHT)));
        }

        @Override
        public ComponentDimensions preferredMinimumDimensions() {
            return new ComponentDimensions(BUTTON_HEIGHT, BUTTON_HEIGHT);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected class ModeLabel extends GuiLabel {
        protected final String name;

        public ModeLabel(@NonNull GuiContext context, @NonNull String name) {
            super(context, new SimpleGuiElementProperties(ConfigGuiRenderModeButton.this.properties.localeKey() + '.' + name) {
                @Override
                public String name() {
                    String localizedName = super.name();
                    return ConfigGuiRenderModeButton.this.serverModes.contains(name) ? localizedName : "§8" + localizedName;
                }

                @Override
                protected void computeTooltipText0(@NonNull StringJoiner joiner) {
                    super.computeTooltipText0(joiner);

                    if (!ConfigGuiRenderModeButton.this.serverModes.contains(name)) {
                        joiner.add(fp2().i18n().format(ConfigGuiRenderModeButton.this.properties.localeKey() + ".renderModeDisabledOnServer"));
                    }
                }
            }, GuiLabel.Alignment.LEFT, GuiLabel.Alignment.CENTER);

            this.name = name;
        }
    }
}
