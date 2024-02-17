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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.of;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.daporkchop.fp2.api.FP2.*;
import static net.daporkchop.fp2.impl.mc.forge1_12_2.compat.of.OFHelper1_12.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = "fp2_optifine", useMetadata = true, dependencies = "required-after:fp2", clientSideOnly = true)
public class FP2OptiFine1_12 {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (!OF) {
            event.getModLog().info("OptiFine not detected, not enabling integration");
            return;
        }

        Events events = new Events();
        fp2().eventBus().register(events); //register self to receive fp2 events
    }

    /**
     * Class containing events which will be registered to activate OptiFine integration.
     *
     * @author DaPorkchop_
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Events {
        //texture UVs

        @SideOnly(Side.CLIENT)
        @FEventHandler(name = "optifine_texuvs_renderquads_grass",
                constrain = @Constrain(before = "vanilla_texuvs_renderquads_default"))
        public void texUVsRenderQuadsBetterGrass(TextureUVs.StateFaceQuadRenderEvent event) {
            IBlockState state = (IBlockState) event.registry().id2state(event.state());
            if (state.getBlock() == Blocks.GRASS && event.direction() != Direction.NEGATIVE_Y //side texture of a grass block
                && PUnsafe.getInt(Minecraft.getMinecraft().gameSettings, OF_BETTERGRASS_OFFSET) != OF_OFF) { //better grass is enabled
                event.direction(Direction.POSITIVE_Y); //use the top texture for the sides
            }
        }
    }
}
