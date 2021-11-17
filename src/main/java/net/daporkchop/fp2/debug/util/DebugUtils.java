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

package net.daporkchop.fp2.debug.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * Debug mode utility methods.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class DebugUtils {
    public static final String CHAT_PREFIX = "§8§l[§9FarPlaneTwo Debug§8§l]§r ";

    @SideOnly(Side.CLIENT)
    public void clientMsg(@NonNull String msg) {
        clientMsg(CHAT_PREFIX, msg);
    }

    @SideOnly(Side.CLIENT)
    public void clientMsg(@NonNull String prefix, @NonNull String msg) {
        for (String line : msg.split("\n")) {
            line = prefix + line;
            if (MC.player != null) {
                MC.player.sendMessage(new TextComponentString(line));
            } else { //not currently ingame...
                FP2_LOG.info(line);
            }
        }
    }
}
