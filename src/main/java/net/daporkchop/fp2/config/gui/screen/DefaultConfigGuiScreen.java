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

package net.daporkchop.fp2.config.gui.screen;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.config.ConfigHelper;
import net.daporkchop.fp2.config.Config;
import net.daporkchop.fp2.config.gui.GuiObjectAccess;
import net.daporkchop.fp2.config.gui.IConfigGuiElement;
import net.daporkchop.fp2.config.gui.IConfigGuiScreen;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.fp2.config.gui.container.ScrollingContainer;
import net.daporkchop.fp2.config.gui.element.GuiTitle;
import net.daporkchop.fp2.config.gui.util.ComponentDimensions;
import net.daporkchop.fp2.config.gui.util.ElementBounds;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiButtonExt;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.config.gui.GuiConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.input.Keyboard.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DefaultConfigGuiScreen implements IConfigGuiScreen {
    @NonNull
    protected final IGuiContext context;
    @NonNull
    protected final IConfigGuiElement element;

    protected ComponentDimensions dimensions = ComponentDimensions.ZERO;
    protected GuiButtonExt doneButton = new GuiButtonExt(0, 0, 0, I18n.format("gui.done"));

    public DefaultConfigGuiScreen(@NonNull IGuiContext context, @NonNull GuiObjectAccess<?> access) {
        this.context = context;

        Config.GuiCategories categories = access.clazz().getAnnotation(Config.GuiCategories.class);
        if (categories == null) { //create default categories
            //dummy class to allow us to access the default value for the {@link Setting.GuiCategories} annotation without needing to implement it manually
            @Config.GuiCategories(@Config.CategoryMeta(name = "default"))
            class DummyClass {
            }

            categories = DummyClass.class.getAnnotation(Config.GuiCategories.class);
        }

        checkArg(categories.value().length != 0, "%s has no GUI categories!", access.clazz());
        Map<String, Config.CategoryMeta> categoriesByName = Stream.of(categories.value())
                .reduce(new LinkedHashMap<>(),
                        (map, meta) -> {
                            checkState(map.putIfAbsent(meta.name(), meta) == null, "duplicate category name: %s", meta.name());
                            return map;
                        },
                        (a, b) -> null);

        Map<Config.CategoryMeta, List<IConfigGuiElement>> elementsByCategory = ConfigHelper.getConfigPropertyFields(access.clazz())
                .collect(Collectors.groupingBy(
                        field -> {
                            String categoryName = Optional.ofNullable(field.getAnnotation(Config.GuiCategory.class)).map(Config.GuiCategory::value).orElse("default");
                            Config.CategoryMeta meta = categoriesByName.get(categoryName);
                            checkArg(meta != null, "no such category: %s", categoryName);
                            return meta;
                        },
                        Collectors.mapping(field -> ConfigHelper.createConfigGuiElement(context, access, field), Collectors.toList())));

        this.element = new ScrollingContainer<>(context, access, categoriesByName.values().stream()
                .map(meta -> new AbstractMap.SimpleEntry<>(meta, elementsByCategory.get(meta)))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .flatMap(entry -> Stream.of(
                        elementsByCategory.size() == 1 || !entry.getKey().title() ? null : new GuiTitle(context, entry.getKey().name() + ".category"),
                        ConfigHelper.createConfigGuiContainer(entry.getKey(), context, access, entry.getValue())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    @Override
    public void init() {
        this.element.init();
    }

    @Override
    public void setDimensions(@NonNull ComponentDimensions dimensions) {
        this.dimensions = dimensions;

        this.pack();
    }

    @Override
    public void pack() {
        ComponentDimensions dimensions = this.element.possibleDimensions(this.dimensions.sizeX() - (PADDING << 1), this.dimensions.sizeY() - (HEADER_TITLE_HEIGHT + FOOTER_HEIGHT + (PADDING << 1)))
                .min(Comparator.comparingInt(ComponentDimensions::sizeY)).get(); //find the shortest possible dimensions
        this.element.bounds(new ElementBounds((this.dimensions.sizeX() - dimensions.sizeX()) >> 1, HEADER_TITLE_HEIGHT + PADDING, dimensions.sizeX(), dimensions.sizeY()));

        this.doneButton.x = (this.dimensions.sizeX() - this.doneButton.width) >> 1;
        this.doneButton.y = this.dimensions.sizeY() - FOOTER_HEIGHT;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        String titleString = I18n.format(this.context.localeKeyBase() + "title");
        MC.fontRenderer.drawStringWithShadow(titleString, (this.dimensions.sizeX() - MC.fontRenderer.getStringWidth(titleString)) >> 1, 15, -1);

        this.renderBackground();
        this.element.render(mouseX, mouseY, partialTicks);

        this.doneButton.drawButton(MC, mouseX, mouseY, partialTicks);
    }

    protected void renderBackground() {
        if (MC.world != null) {
            return;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        MC.getTextureManager().bindTexture(Gui.OPTIONS_BACKGROUND);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        int left = 0;
        int right = this.dimensions.sizeX();
        int top = HEADER_TITLE_HEIGHT;
        int bottom = this.dimensions.sizeY() - FOOTER_HEIGHT - PADDING;

        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(left, bottom, 0.0d).tex(left / 32.0d, bottom / 32.0d).color(32, 32, 32, 255).endVertex();
        buffer.pos(right, bottom, 0.0d).tex(right / 32.0d, bottom / 32.0d).color(32, 32, 32, 255).endVertex();
        buffer.pos(right, top, 0.0d).tex(right / 32.0d, top / 32.0d).color(32, 32, 32, 255).endVertex();
        buffer.pos(left, top, 0.0d).tex(left / 32.0d, top / 32.0d).color(32, 32, 32, 255).endVertex();
        tessellator.draw();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(7425);
        GlStateManager.disableTexture2D();
        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(left, (top + 4), 0.0d).tex(0.0d, 1.0d).color(0, 0, 0, 0).endVertex();
        buffer.pos(right, (top + 4), 0.0d).tex(1.0d, 1.0d).color(0, 0, 0, 0).endVertex();
        buffer.pos(right, top, 0.0d).tex(1.0d, 0.0d).color(0, 0, 0, 255).endVertex();
        buffer.pos(left, top, 0.0d).tex(0.0d, 0.0d).color(0, 0, 0, 255).endVertex();
        buffer.pos(left, bottom, 0.0d).tex(0.0d, 1.0d).color(0, 0, 0, 255).endVertex();
        buffer.pos(right, bottom, 0.0d).tex(1.0d, 1.0d).color(0, 0, 0, 255).endVertex();
        buffer.pos(right, (bottom - 4), 0.0d).tex(1.0d, 0.0d).color(0, 0, 0, 0).endVertex();
        buffer.pos(left, (bottom - 4), 0.0d).tex(0.0d, 0.0d).color(0, 0, 0, 0).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.shadeModel(7424);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
    }

    @Override
    public Optional<String[]> getTooltip(int mouseX, int mouseY) {
        return this.element.getTooltip(mouseX, mouseY);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (this.doneButton.mousePressed(MC, mouseX, mouseY)) {
            this.context.pop();
            return;
        }

        this.element.mouseDown(mouseX, mouseY, button);
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        this.element.mouseUp(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        this.element.mouseDragged(oldMouseX, oldMouseY, newMouseX, newMouseY, button);
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        this.element.mouseScroll(mouseX, mouseY, dWheel);
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        if (keyCode == KEY_ESCAPE) {
            this.context.pop();
        } else {
            this.element.keyPressed(typedChar, keyCode);
        }
    }
}
