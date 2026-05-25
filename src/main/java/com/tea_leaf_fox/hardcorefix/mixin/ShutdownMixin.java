package com.tea_leaf_fox.hardcorefix.mixin;

import com.tea_leaf_fox.hardcorefix.HardcoreEncryption;
import com.tea_leaf_fox.hardcorefix.HardcoreTimeManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class ShutdownMixin {

    @Inject(method = "shutdown", at = @At("TAIL"))
    private void onShutdownComplete(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (!server.isHardcore()) return;

        ServerWorld overworld = server.getOverworld();
        if (overworld != null) {
            HardcoreEncryption.encryptLevelFiles(overworld);
            HardcoreTimeManager.saveTime(overworld);   // 记录时间戳
        }
    }
}