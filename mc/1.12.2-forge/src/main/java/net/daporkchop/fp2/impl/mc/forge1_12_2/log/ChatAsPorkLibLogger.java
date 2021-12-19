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

package net.daporkchop.fp2.impl.mc.forge1_12_2.log;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.logging.LogLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class ChatAsPorkLibLogger extends BaseProxyLogger {
    protected static final String PRE = "§r§8§l[§r";
    protected static final String POST = "§r§8§l]§r ";

    protected static final String[] LEVEL_PREFIXES = {
            PRE + "§9Info" + POST,
            PRE + "§aSuccess" + POST,
            PRE + "§4Error" + POST,
            PRE + "§4§lFatal" + POST,
            PRE + " §4§l§kI§r §4§lAlert§r §4§l§kI§r " + POST,
            PRE + "§eWarn" + POST,
            PRE + "§7§oTrace" + POST,
            PRE + "§7§oDebug" + POST,
    };

    @NonNull
    protected final Minecraft mc;

    @Override
    protected void log(@NonNull LogLevel level, String channel, @NonNull String message) {
        message = PRE + "§9FarPlaneTwo" + POST + (channel != null ? PRE + channel + POST : "") + LEVEL_PREFIXES[level.ordinal()] + message;

        if (this.mc.player != null) {
            this.mc.player.sendMessage(new TextComponentString(message));
        } else {
            fp2().log().log(level, "[CHAT] " + message);
        }
    }
}
