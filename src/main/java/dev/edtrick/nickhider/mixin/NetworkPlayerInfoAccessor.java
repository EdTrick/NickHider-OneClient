package dev.edtrick.nickhider.mixin;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NetworkPlayerInfo.class)
public interface NetworkPlayerInfoAccessor {

    @Accessor("playerTexturesLoaded")
    void setPlayerTexturesLoaded(boolean loaded);

    @Accessor("locationSkin")
    void setLocationSkin(ResourceLocation location);

    @Invoker("loadPlayerTextures")
    void invokeLoadPlayerTextures();
}
