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

package net.daporkchop.fp2.impl.mc.forge1_16.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.client.gui.GuiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;
import net.minecraftforge.fml.client.gui.GuiUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class GuiRenderer1_16 implements GuiRenderer {
    @NonNull
    protected final Minecraft mc;
    @NonNull
    protected final GuiContext1_16 context;

    protected MatrixStack matrixStack;

    protected int totalDx;
    protected int totalDy;

    protected boolean scissor;

    public void render(@NonNull MatrixStack matrixStack, @NonNull Runnable action) {
        this.matrixStack = matrixStack;

        RenderSystem.stencilMask(1);
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL_STENCIL_BUFFER_BIT, false);

        glEnable(GL_STENCIL_TEST);
        RenderSystem.stencilMask(0);
        RenderSystem.stencilFunc(GL_EQUAL, 0, 1);
        RenderSystem.stencilOp(GL_KEEP, GL_REPLACE, GL_REPLACE);

        try {
            action.run();
        } finally {
            glDisable(GL_STENCIL_TEST);

            this.matrixStack = null;
        }
    }

    @Override
    public void translate(int dx, int dy, @NonNull Runnable action) {
        this.matrixStack.pushPose();
        this.matrixStack.translate(dx, dy, 0.0d);

        try {
            this.totalDx += dx;
            this.totalDy += dy;
            action.run();
        } finally {
            this.totalDx -= dx;
            this.totalDy -= dy;
            this.matrixStack.popPose();
        }
    }

    @Override
    public void scissor(int x, int y, int width, int height, @NonNull Runnable action) {
        checkState(!this.scissor, "recursive scissor");

        int scale = (int) this.mc.getWindow().getGuiScale();
        RenderSystem.enableScissor(x * scale, (this.context.height - y - height) * scale, width * scale, height * scale);

        try {
            this.scissor = true;
            action.run();
        } finally {
            this.scissor = false;
            RenderSystem.disableScissor();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void drawDefaultBackground(int x, int y, int width, int height) {
        if (this.mc.level != null) {
            return;
        }

        //adapted from AbstractList

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        this.mc.getTextureManager().bind(AbstractGui.BACKGROUND_LOCATION);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        int left = x;
        int right = x + width;
        int top = y;
        int bottom = y + height;

        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.vertex(left, bottom, 0.0d).uv(left / 32.0f, bottom / 32.0f).color(32, 32, 32, 255).endVertex();
        buffer.vertex(right, bottom, 0.0d).uv(right / 32.0f, bottom / 32.0f).color(32, 32, 32, 255).endVertex();
        buffer.vertex(right, top, 0.0d).uv(right / 32.0f, top / 32.0f).color(32, 32, 32, 255).endVertex();
        buffer.vertex(left, top, 0.0d).uv(left / 32.0f, top / 32.0f).color(32, 32, 32, 255).endVertex();
        tessellator.end();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.disableAlphaTest();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableTexture();
        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.vertex(left, (top + 4), 0.0d).uv(0.0f, 1.0f).color(0, 0, 0, 0).endVertex();
        buffer.vertex(right, (top + 4), 0.0d).uv(1.0f, 1.0f).color(0, 0, 0, 0).endVertex();
        buffer.vertex(right, top, 0.0d).uv(1.0f, 0.0f).color(0, 0, 0, 255).endVertex();
        buffer.vertex(left, top, 0.0d).uv(0.0f, 0.0f).color(0, 0, 0, 255).endVertex();
        buffer.vertex(left, bottom, 0.0d).uv(0.0f, 1.0f).color(0, 0, 0, 255).endVertex();
        buffer.vertex(right, bottom, 0.0d).uv(1.0f, 1.0f).color(0, 0, 0, 255).endVertex();
        buffer.vertex(right, (bottom - 4), 0.0d).uv(1.0f, 0.0f).color(0, 0, 0, 0).endVertex();
        buffer.vertex(left, (bottom - 4), 0.0d).uv(0.0f, 0.0f).color(0, 0, 0, 0).endVertex();
        tessellator.end();

        RenderSystem.enableTexture();
        RenderSystem.shadeModel(7424);
        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
    }

    @Override
    public void drawQuadColored(int x, int y, int width, int height, int argb) {
        AbstractGui.fill(this.matrixStack, x, y, x + width, y + height, argb);
    }

    @Override
    public void drawButtonBackground(int x, int y, int width, int height, boolean hovered, boolean enabled) {
        int buttonIndex = enabled ? hovered ? 2 : 1 : 0;
        GuiUtils.drawContinuousTexturedBox(this.matrixStack, Widget.WIDGETS_LOCATION, x, y, 0, buttonIndex * 20 + 46, width, height, 200, 20, 2, 3, 2, 2, 0);
    }

    @Override
    public int getStringWidth(@NonNull CharSequence text) {
        return this.mc.font.width(text.toString());
    }

    @Override
    public int getStringHeight() {
        return this.mc.font.lineHeight;
    }

    @Override
    public CharSequence trimStringToWidth(@NonNull CharSequence text, int width, boolean reverse) {
        return this.mc.font.plainSubstrByWidth(text.toString(), width, reverse);
    }

    @Override
    public List<CharSequence> wrapStringToWidth(@NonNull CharSequence text, int width) {
        return uncheckedCast(this.mc.font.getSplitter().splitLines(text.toString(), width, Style.EMPTY).stream()
                .map(ITextProperties::getString)
                .collect(Collectors.toList()));
    }

    @Override
    public void drawString(@NonNull CharSequence text, int x, int y, int argb, boolean shadow) {
        if (shadow) {
            this.mc.font.drawShadow(this.matrixStack, text.toString(), x, y, argb);
        } else {
            this.mc.font.draw(this.matrixStack, text.toString(), x, y, argb);
        }
    }

    @Override
    public void drawTooltip(int mouseX, int mouseY, int maxWidth, @NonNull CharSequence... lines) {
        this.matrixStack.pushPose();
        this.matrixStack.translate(-this.totalDx, -this.totalDy, 0.0d);

        if (this.scissor) {
            GlStateManager._disableScissorTest();
        }

        RenderSystem.stencilMask(1);
        RenderSystem.stencilFunc(GL_ALWAYS, 1, 1);
        RenderSystem.stencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);

        try {
            GuiUtils.drawHoveringText(this.matrixStack,
                    Stream.of(lines).map(line -> ITextProperties.of(line.toString())).collect(Collectors.toList()),
                    mouseX + this.totalDx, mouseY + this.totalDy, this.context.width, this.context.height, maxWidth, this.mc.font);
            //RenderSystem.disableLighting();
        } finally {
            RenderSystem.stencilMask(0);
            RenderSystem.stencilFunc(GL_EQUAL, 0, 1);
            RenderSystem.stencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

            if (this.scissor) {
                GlStateManager._enableScissorTest();
            }

            this.matrixStack.popPose();
        }
    }
}
