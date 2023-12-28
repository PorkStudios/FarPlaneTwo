/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomTerrainGenerator;
import io.github.opencubicchunks.cubicchunks.cubicgen.flat.FlatTerrainProcessor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.engine.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg.generator.voxel.CWGVoxelGenerator;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Optional;

import static net.daporkchop.fp2.api.FP2.*;

/**
 * @author DaPorkchop_
 */
@Mod(modid = "fp2_cubicworldgen", useMetadata = true, dependencies = "required-after:fp2;required-after:fp2_cubicchunks;after:cubicgen")
public class FP2CubicWorldGen {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (!Loader.isModLoaded("cubicgen")) {
            event.getModLog().info("CubicWorldGen not detected, not enabling integration");
            return;
        }

        Events events = new Events();
        fp2().eventBus().register(events); //register self to receive fp2 events
    }

    /**
     * Class containing events which will be registered to activate CubicWorldGen integration.
     *
     * @author DaPorkchop_
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Events {
        //CustomCubic rough generators

        protected boolean isCustomCubicWorld(IFarLevelServer world) {
            ICubicWorldServer cubicWorld = (ICubicWorldServer) world.implLevel();
            return cubicWorld.isCubicWorld() && cubicWorld.getCubeGenerator() instanceof CustomTerrainGenerator;
        }

        @FEventHandler(name = "cubicworldgen_generator_rough_customcubic")
        public Optional<IFarGeneratorRough> createGeneratorRoughCustomCubic(IFarGeneratorRough.CreationEvent event) {
            return this.isCustomCubicWorld(event.world())
                    ? Optional.of(new CWGVoxelGenerator(event.world(), event.provider()))
                    : Optional.empty();
        }

        //FlatCubic rough generators

        private boolean isFlatCubicWorld(IFarLevelServer world) {
            ICubicWorldServer cubicWorld = (ICubicWorldServer) world.implLevel();
            return cubicWorld.isCubicWorld() && cubicWorld.getCubeGenerator() instanceof FlatTerrainProcessor;
        }
    }
}
