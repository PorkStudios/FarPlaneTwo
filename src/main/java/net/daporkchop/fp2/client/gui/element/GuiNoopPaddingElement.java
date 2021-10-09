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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.client.gui.IConfigGuiElement;
import net.daporkchop.fp2.client.gui.util.ComponentDimensions;
import net.daporkchop.fp2.client.gui.util.ElementBounds;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@Setter
@Accessors(chain = false)
@SideOnly(Side.CLIENT)
public class GuiNoopPaddingElement implements IConfigGuiElement {
    @NonNull
    protected final ComponentDimensions preferredMinimumDimensions;

    @NonNull
    protected ElementBounds bounds = null;

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        return Stream.of(new ComponentDimensions(min(this.preferredMinimumDimensions.sizeX(), totalSizeX), min(this.preferredMinimumDimensions.sizeY(), totalSizeY)));
    }

    @Override
    public void init() {
    }

    @Override
    public void pack() {
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public Optional<String[]> getTooltip(int mouseX, int mouseY) {
        return Optional.empty();
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
    }
}
