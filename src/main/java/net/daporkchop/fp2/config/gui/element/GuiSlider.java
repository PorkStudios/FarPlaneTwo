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
import net.daporkchop.fp2.config.ConfigHelper;
import net.daporkchop.fp2.config.Setting;
import net.daporkchop.fp2.config.gui.IGuiContext;
import net.daporkchop.fp2.config.gui.util.ComponentDimensions;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.minecraft.client.resources.I18n;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.FP2.*;
import static net.daporkchop.fp2.config.gui.GuiConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class GuiSlider extends AbstractReflectiveConfigGuiElement<Number> {
    protected final net.minecraftforge.fml.client.config.GuiSlider slider;

    public GuiSlider(@NonNull IGuiContext context, Object instance, @NonNull Field field) {
        super(context, instance, field);

        Setting.Range rangeAnnotation = field.getAnnotation(Setting.Range.class);
        Setting.GuiRange guiRangeAnnotation = field.getAnnotation(Setting.GuiRange.class);
        checkState(rangeAnnotation != null || guiRangeAnnotation != null, "cannot create slider for %s which isn't annotated with %s or %s", field, Setting.Range.class, Setting.GuiRange.class);
        Number minValue = ConfigHelper.evaluate(guiRangeAnnotation != null ? guiRangeAnnotation.min() : rangeAnnotation.min());
        Number maxValue = ConfigHelper.evaluate(guiRangeAnnotation != null ? guiRangeAnnotation.max() : rangeAnnotation.max());

        boolean fp;
        Function<Double, Number> boxFunction;

        Class<?> type = field.getType();
        if (type == int.class || type == Integer.class) {
            fp = false;
            boxFunction = Double::intValue;
        } else if (type == long.class || type == Long.class) {
            fp = false;
            boxFunction = Double::longValue;
        } else if (type == float.class || type == Float.class) {
            fp = true;
            boxFunction = Double::floatValue;
        } else if (type == double.class || type == Double.class) {
            fp = true;
            boxFunction = Double::doubleValue;
        } else {
            throw new IllegalArgumentException(PStrings.fastFormat("cannot create slider for %s of unsupported type %s", field, type));
        }

        this.slider = new net.minecraftforge.fml.client.config.GuiSlider(0, 0, 0, 0, 0, "", "", minValue.doubleValue(), maxValue.doubleValue(), this.get().doubleValue(), fp, true,
                slider -> this.set(boxFunction.apply(fp ? slider.getValue() : slider.getValueInt())));
    }

    @Override
    public Stream<ComponentDimensions> possibleDimensions(int totalSizeX, int totalSizeY) {
        return IntStream.rangeClosed(0, totalSizeX).mapToObj(i -> new ComponentDimensions(i, min(totalSizeY, BUTTON_HEIGHT)));
    }

    @Override
    public ComponentDimensions preferredMinimumDimensions() {
        return new ComponentDimensions(200, BUTTON_HEIGHT);
    }

    @Override
    public void init() {
        String text = I18n.format(this.langKey());

        String prefixFormatKey = this.langKey() + ".prefixFormat";
        this.slider.dispString = I18n.format(I18n.hasKey(prefixFormatKey) ? prefixFormatKey : MODID + ".config.slider.prefixFormat", text);

        String suffixFormatKey = this.langKey() + ".suffixFormat";
        this.slider.suffix = I18n.format(I18n.hasKey(suffixFormatKey) ? suffixFormatKey : MODID + ".config.slider.suffixFormat", text);

        this.slider.updateSlider();
    }

    @Override
    public void pack() {
        this.slider.x = this.bounds.x();
        this.slider.y = this.bounds.y();
        this.slider.width = this.bounds.sizeX();
        this.slider.height = this.bounds.sizeY();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.slider.drawButton(MC, mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseDown(int mouseX, int mouseY, int button) {
        if (button == 0) {
            this.slider.mousePressed(MC, mouseX, mouseY);
        }
    }

    @Override
    public void mouseUp(int mouseX, int mouseY, int button) {
        this.slider.mouseReleased(mouseX, mouseY);
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int dWheel) {
        //no-op
    }

    @Override
    public void mouseDragged(int oldMouseX, int oldMouseY, int newMouseX, int newMouseY, int button) {
        //no-op
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        //no-op
    }
}
