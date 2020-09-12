/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.client;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.FP2Config;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
@UtilityClass
public class KeyBindings {
    public final KeyBinding RELOAD_SHADERS = new KeyBinding("key.fp2.debug.reloadShaders", Keyboard.KEY_0, "key.categories.fp2.debug");
    public final KeyBinding DROP_PIECES = new KeyBinding("key.fp2.debug.dropPieces", Keyboard.KEY_9, "key.categories.fp2.debug");
    public final KeyBinding RENDER_PIECES = new KeyBinding("key.fp2.debug.renderPieces", Keyboard.KEY_8, "key.categories.fp2.debug");

    public void register() {
        if (FP2Config.debug.debug)   {
            ClientRegistry.registerKeyBinding(RELOAD_SHADERS);
            ClientRegistry.registerKeyBinding(DROP_PIECES);
            ClientRegistry.registerKeyBinding(RENDER_PIECES);
        }
    }
}
