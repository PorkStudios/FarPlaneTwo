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
 */

package net.daporkchop.fp2.impl.mc.forge1_16.server.world.level;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.server.world.TerrainGeneratorInfo;
import net.daporkchop.fp2.core.server.world.level.AbstractLevelServer;
import net.daporkchop.fp2.core.util.threading.workergroup.DefaultWorkerManager;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.impl.mc.forge1_16.FP2Forge1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.server.ATMinecraftServer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.server.world.FWorldServer1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.server.world.TerrainGeneratorInfo1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.threading.futureexecutor.ServerThreadMarkedFutureExecutor1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.world.level.IFLevel1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.world.registry.GameRegistry1_16;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;

import java.nio.file.Path;

/**
 * @author DaPorkchop_
 */
@Getter
public class FLevelServer1_16 extends AbstractLevelServer<FP2Forge1_16, MinecraftServer, FWorldServer1_16, ServerWorld, FLevelServer1_16> implements IFLevel1_16 {
    public FLevelServer1_16(@NonNull FP2Forge1_16 fp2, @NonNull ServerWorld implLevel, @NonNull FWorldServer1_16 world, @NonNull Identifier id) {
        super(fp2, implLevel, world, id, new GameRegistry1_16(implLevel));
    }

    @Override
    protected WorkerManager createWorkerManager() {
        return new DefaultWorkerManager(
                this.implLevel().getServer().getRunningThread(),
                ServerThreadMarkedFutureExecutor1_16.getFor(this.implLevel().getServer()));
    }

    @Override
    public Path levelDirectory() {
        return ((ATMinecraftServer1_16) this.implLevel().getServer()).getStorageSource().getDimensionPath(this.implLevel().dimension()).toPath();
    }

    @Override
    public TerrainGeneratorInfo terrainGeneratorInfo() {
        return new TerrainGeneratorInfo1_16(this, this.implLevel());
    }

    @Override
    public int seaLevel() {
        return this.implLevel().getSeaLevel();
    }
}
