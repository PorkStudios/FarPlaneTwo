/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.core.world;

import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.server.IMixinMinecraftServer1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.world.IMixinWorldServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.world.level.FLevelServer1_12;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Mixin(WorldServer.class)
public abstract class MixinWorldServer implements IMixinWorldServer {
    @Unique
    protected FLevelServer1_12 fp2_levelServer;

    @Override
    public void fp2_initLevelServer() {
        checkState(this.fp2_levelServer == null, "already initialized!");

        this.fp2_levelServer = (FLevelServer1_12) ((IMixinMinecraftServer1_12) PorkUtil.<WorldServer>uncheckedCast(this).getMinecraftServer()).fp2_worldServer().get()
                .loadLevel(FP2Forge1_12_2.getIdentifierForWorld(PorkUtil.<WorldServer>uncheckedCast(this)), this);
    }

    @Override
    public void fp2_closeLevelServer() {
        checkState(this.fp2_levelServer != null, "not initialized or already closed!");

        //try-with-resources to ensure everything gets closed
        try (FLevelServer1_12 levelServer = this.fp2_levelServer) {
            this.fp2_levelServer = null;
        }
    }

    @Override
    public FLevelServer1_12 fp2_levelServer() {
        checkState(this.fp2_levelServer != null, "not initialized or already closed!");
        return this.fp2_levelServer;
    }
}
