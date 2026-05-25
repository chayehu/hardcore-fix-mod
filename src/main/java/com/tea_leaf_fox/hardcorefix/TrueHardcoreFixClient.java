package com.tea_leaf_fox.hardcorefix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

public class TrueHardcoreFixClient implements ClientModInitializer {
    private static final String ROLLBACK_MARKER = "tea_leaf_fox.txt";
    private final Set<Path> decryptedWorlds = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SelectWorldScreen) {
                // 打开世界选择界面 → 解密所有未标记的极限存档，并记录
                decryptedWorlds.clear();
                Path savesDir = client.getLevelStorage().getSavesDirectory();
                try (DirectoryStream<Path> dirs = Files.newDirectoryStream(savesDir, Files::isDirectory)) {
                    for (Path worldDir : dirs) {
                        if (Files.exists(worldDir.resolve(ROLLBACK_MARKER))) continue;
                        if (Files.exists(worldDir.resolve("hardcore.lock"))) {
                            try {
                                HardcoreEncryption.decryptFilesInDir(worldDir);
                                decryptedWorlds.add(worldDir);
                            } catch (Exception e) {
                                System.err.println("[真正的极限模式] 解密失败: " + worldDir.getFileName());
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[真正的极限模式] 扫描存档失败！");
                }
            } else {
                // 离开世界选择界面（即将打开其他屏幕）→ 重新加密刚才解密的世界
                if (!decryptedWorlds.isEmpty()) {
                    for (Path worldDir : decryptedWorlds) {
                        HardcoreEncryption.encryptWorldDir(worldDir);
                    }
                    decryptedWorlds.clear();
                }
            }
        });
    }
}