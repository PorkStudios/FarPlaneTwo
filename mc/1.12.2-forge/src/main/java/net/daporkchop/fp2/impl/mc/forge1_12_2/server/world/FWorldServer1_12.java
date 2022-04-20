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

package net.daporkchop.fp2.impl.mc.forge1_12_2.server.world;

import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.world.level.FLevel;
import net.daporkchop.fp2.core.server.world.AbstractWorldServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FP2Forge1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.world.level.FLevelServer1_12;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

/**
 * @author DaPorkchop_
 */
public class FWorldServer1_12 extends AbstractWorldServer<FP2Forge1_12_2> {
    private final MinecraftServer server;

    public FWorldServer1_12(@NonNull FP2Forge1_12_2 fp2, @NonNull MinecraftServer server) {
        super(fp2, server.getActiveAnvilConverter().getSaveLoader(server.getFolderName(), false).getWorldDirectory().toPath());

        this.server = server;
    }

    @Override
    protected FLevel createLevel(@NonNull Identifier id, Object implLevel) {
        return new FLevelServer1_12(this.fp2(), (WorldServer) implLevel, this, id);
    }
}
