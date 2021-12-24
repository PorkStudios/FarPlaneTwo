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

package net.daporkchop.fp2.compat.of;

import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.render.TextureUVs1_12_2;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.daporkchop.fp2.api.FP2.*;
import static net.daporkchop.fp2.compat.of.OFHelper.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = "fp2_optifine", useMetadata = true, dependencies = "required-after:fp2")
public class FP2OptiFine {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (!OF) {
            event.getModLog().info("OptiFine not detected, not enabling integration");
            return;
        }

        fp2().eventBus().register(this); //register self to receive fp2 events
    }

    //texture UVs

    @SideOnly(Side.CLIENT)
    @FEventHandler(name = "optifine_texuvs_renderquads_grass",
            constrain = @Constrain(before = "vanilla_texuvs_renderquads_default"))
    public void texUVsRenderQuadsBetterGrass(TextureUVs1_12_2.StateFaceQuadRenderEvent event) {
        if (event.state().getBlock() == Blocks.GRASS && event.facing() != EnumFacing.DOWN //side texture of a grass block
            && PUnsafe.getInt(Minecraft.getMinecraft().gameSettings, OF_BETTERGRASS_OFFSET) != OF_OFF) { //better grass is enabled
            event.facing(EnumFacing.UP); //use the top texture for the sides
        }
    }
}
