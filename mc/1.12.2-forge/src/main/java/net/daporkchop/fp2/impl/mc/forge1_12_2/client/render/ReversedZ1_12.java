/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.impl.mc.forge1_12_2.client.render;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.render.ReversedZ;
import net.minecraft.client.renderer.GlStateManager;

/**
 * @author DaPorkchop_
 */
public class ReversedZ1_12 extends ReversedZ {
    public ReversedZ1_12(@NonNull FP2Client client) {
        super(client);
    }

    @Override
    protected void onToggle(boolean active, int oldDepthFunc, double clearDepth) {
        // Thanks to our mixins, this will automatically reverse the provided depth value when reversed-Z is active.
        // Since we want to reverse the depth value, we should therefore only reverse the old depth function here if
        // reversed-Z is disabled.
        GlStateManager.depthFunc(active ? oldDepthFunc : reverseDepthFunc(oldDepthFunc));

        GlStateManager.clearDepth(clearDepth);
    }
}
