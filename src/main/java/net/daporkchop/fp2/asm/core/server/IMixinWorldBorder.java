package net.daporkchop.fp2.asm.core.server;

import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldBorder.class)
public interface IMixinWorldBorder {
    @Accessor("startDiameter")
    double getWorldBorderStartDiameter();

    @Accessor("worldSize")
    int getWorldSize();
}
