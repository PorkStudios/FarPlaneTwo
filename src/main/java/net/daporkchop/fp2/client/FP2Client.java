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

package net.daporkchop.fp2.client;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gl.shader.reload.ShaderMacros;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.config.listener.ConfigListenerManager;
import net.daporkchop.fp2.mode.heightmap.client.HeightmapShaders;
import net.daporkchop.fp2.mode.voxel.client.VoxelShaders;
import net.daporkchop.fp2.net.packet.standard.client.CPacketClientConfig;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.compat.of.OFHelper.*;
import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static net.daporkchop.fp2.net.FP2Network.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Manages initialization of FP2 on the client.
 *
 * @author DaPorkchop_
 */
@UtilityClass
@SideOnly(Side.CLIENT)
public class FP2Client {
    public static final ShaderMacros.Mutable GLOBAL_SHADER_MACROS = new ShaderMacros.Mutable();

    /**
     * Called during {@link FMLPreInitializationEvent}.
     */
    public void preInit() {
        if (!OPENGL_45) { //require at least OpenGL 4.5
            unsupported("Your system does not support OpenGL 4.5!\nRequired by FarPlaneTwo.");
        }

        int size = glGetInteger(GL_MAX_SHADER_STORAGE_BLOCK_SIZE);
        FP2_LOG.info(PStrings.fastFormat("Max SSBO size: %d bytes (%.2f MiB)", size, size / (1024.0d * 1024.0d)));

        if (!MC.getFramebuffer().isStencilEnabled() && !MC.getFramebuffer().enableStencil()) {
            if (OF && (PUnsafe.getBoolean(MC.gameSettings, OF_FASTRENDER_OFFSET) || PUnsafe.getInt(MC.gameSettings, OF_AALEVEL_OFFSET) > 0)) {
                unsupported("FarPlaneTwo was unable to enable the OpenGL stencil buffer!\n"
                            + "Please launch the game without FarPlaneTwo and disable\n"
                            + "  OptiFine's \"Fast Render\" and \"Antialiasing\", then\n"
                            + "  try again.");
            } else {
                unsupported("Unable to enable the OpenGL stencil buffer!\nRequired by FarPlaneTwo.");
            }
        }

        ClientEvents.register();

        ConfigListenerManager.add(() -> PROTOCOL_FP2.sendToServer(new CPacketClientConfig().config(FP2Config.global())));
    }

    /**
     * Called during {@link FMLInitializationEvent}.
     */
    public void init() {
        KeyBindings.register();
    }

    /**
     * Called during {@link FMLPostInitializationEvent}.
     */
    public void postInit() {
        GLOBAL_SHADER_MACROS
                .define("T_SHIFT", T_SHIFT)
                .define("RENDER_PASS_COUNT", RENDER_PASS_COUNT);

        TexUVs.initDefault();

        MC.resourceManager.registerReloadListener(new FP2ResourceReloadListener());

        //load shader classes on client thread
        PUnsafe.ensureClassInitialized(HeightmapShaders.class);
        PUnsafe.ensureClassInitialized(VoxelShaders.class);
    }
}
