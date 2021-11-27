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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client.gui;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.client.gui.GuiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.client.config.GuiUtils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class GuiRenderer1_12_2 implements GuiRenderer {
    @NonNull
    protected final Minecraft mc;
    @NonNull
    protected final GuiContext1_12_2 context;

    @Override
    public void translate(int dx, int dy, @NonNull Runnable action) {
        glPushMatrix();
        glTranslatef(dx, dy, 0.0f);

        try {
            action.run();
        } finally {
            glPopMatrix();
        }
    }

    @Override
    public void scissor(int x, int y, int width, int height, @NonNull Runnable action) {
        int scale = this.context.scaleFactor;
        glScissor(x * scale, (this.context.height - y - height) * scale, width * scale, height * scale);
        glEnable(GL_SCISSOR_BIT);

        try {
            action.run();
        } finally {
            glDisable(GL_SCISSOR_BIT);
        }
    }

    @Override
    public void drawDefaultBackground(int x, int y, int width, int height) {
        if (this.mc.world != null) {
            return;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        this.mc.getTextureManager().bindTexture(Gui.OPTIONS_BACKGROUND);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int left = x;
        int right = x + width;
        int top = y;
        int bottom = y + height;

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
    public void drawQuadColored(int x, int y, int width, int height, int argb) {
        Gui.drawRect(x, y, x + width, y + height, argb);
    }

    @Override
    public void drawButtonBackground(int x, int y, int width, int height, boolean hovered, boolean enabled) {
        int buttonIndex = enabled ? hovered ? 2 : 1 : 0;
        GuiUtils.drawContinuousTexturedBox(GuiButton.BUTTON_TEXTURES, x, y, 0, buttonIndex * 20 + 46, width, height, 200, 20, 2, 3, 2, 2, 0);
    }

    @Override
    public int getStringWidth(@NonNull CharSequence text) {
        return this.mc.fontRenderer.getStringWidth(text.toString());
    }

    @Override
    public int getStringHeight() {
        return this.mc.fontRenderer.FONT_HEIGHT;
    }

    @Override
    public CharSequence trimStringToWidth(@NonNull CharSequence text, int width, boolean reverse) {
        return this.mc.fontRenderer.trimStringToWidth(text.toString(), width, reverse);
    }

    @Override
    public void drawString(@NonNull CharSequence text, int x, int y, int argb, boolean shadow) {
        this.mc.fontRenderer.drawString(text.toString(), x, y, argb, shadow);
    }

    @Override
    public void drawTooltip(int mouseX, int mouseY, int maxWidth, @NonNull CharSequence... lines) {
        GuiUtils.drawHoveringText(Stream.of(lines).map(CharSequence::toString).collect(Collectors.toList()), mouseX, mouseY, this.context.width, this.context.height, maxWidth, this.mc.fontRenderer);
    }
}
