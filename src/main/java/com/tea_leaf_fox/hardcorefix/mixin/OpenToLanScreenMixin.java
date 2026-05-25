package com.tea_leaf_fox.hardcorefix.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OpenToLanScreen.class)
public class OpenToLanScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void lockButtonsForHardcore(CallbackInfo ci) {
        // 安全检查：获取当前的屏幕实例
        OpenToLanScreen screen = (OpenToLanScreen) (Object) this;

        // 检查当前世界是否为极限模式
        if (MinecraftClient.getInstance().world.getLevelProperties().isHardcore()) {
            // 遍历屏幕上的所有元素
            screen.children().forEach(element -> {
                // 找到“允许作弊”和“游戏模式”按钮
                if (element instanceof CyclingButtonWidget<?> button) {
                    // 将按钮的“激活”状态设为false，使其变灰且无法点击
                    button.active = false;
                } else if (element instanceof ButtonWidget button) {
                    // 同样禁用其他可能的功能按钮
                    button.active = false;
                }
            });
        }
    }
}