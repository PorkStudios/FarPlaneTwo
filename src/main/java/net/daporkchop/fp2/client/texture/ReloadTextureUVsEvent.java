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

package net.daporkchop.fp2.client.texture;

import lombok.NonNull;
import net.daporkchop.fp2.debug.util.DebugUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.ArrayList;
import java.util.List;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * Fired on {@link MinecraftForge#EVENT_BUS} when FarPlaneTwo's texture UV cache should be reloaded.
 *
 * @author DaPorkchop_
 */
public class ReloadTextureUVsEvent extends Event {
    /**
     * Fires a texture UV reload event. The player will be notified with the result.
     */
    public static void fire() {
        ReloadTextureUVsEvent event = new ReloadTextureUVsEvent();
        MinecraftForge.EVENT_BUS.post(event);

        if (event.failureCauses.isEmpty()) {
            DebugUtils.clientMsg("§a" + event.total + " texture UV caches successfully reloaded.");
        } else {
            Throwable throwable = new RuntimeException();
            event.failureCauses.forEach(throwable::addSuppressed);
            FP2_LOG.error("texture UV cache reload failed", throwable);

            DebugUtils.clientMsg("§c" + event.failureCauses.size() + '/' + event.total + " texture UV cache failed to reload (check log for info)");
        }
    }

    protected List<Throwable> failureCauses = new ArrayList<>();
    protected int total;

    /**
     * Called by handlers when a shader was successfully reloaded.
     */
    public void handleSuccess() {
        this.total++;
    }

    /**
     * Called by handlers when a shader fails to be reloaded.
     *
     * @param cause the cause of the failure
     */
    public void handleFailure(@NonNull Throwable cause) {
        this.failureCauses.add(cause);
        this.total++;
    }
}
