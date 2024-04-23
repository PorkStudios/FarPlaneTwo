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

package net.daporkchop.fp2.impl.mc.forge1_16.client.render;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.render.common.AbstractTextureUVs;
import net.daporkchop.fp2.impl.mc.forge1_16.world.registry.GameRegistry1_16;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.inventory.container.PlayerContainer;

import java.util.Collections;
import java.util.List;

/**
 * @author DaPorkchop_
 */
@Getter
public class TextureUVs1_16 extends AbstractTextureUVs {
    protected final Minecraft mc;

    public TextureUVs1_16(@NonNull FP2Core fp2, @NonNull GameRegistry1_16 registry, @NonNull Minecraft mc) {
        super(fp2, registry);
        this.mc = mc;

        this.reloadUVs();
    }

    @Override
    protected List<PackedBakedQuad> missingTextureQuads() {
        TextureAtlasSprite sprite = this.mc.getModelManager().getAtlas(PlayerContainer.BLOCK_ATLAS).getSprite(MissingTextureSprite.getLocation());
        return Collections.singletonList(new PackedBakedQuad(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), 1.0f));
    }
}
