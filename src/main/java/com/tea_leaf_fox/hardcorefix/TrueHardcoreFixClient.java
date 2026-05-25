package com.tea_leaf_fox.hardcorefix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import java.io.IOException;
import java.nio.file.*;

public class TrueHardcoreFixClient implements ClientModInitializer {
    private static final String ROLLBACK_MARKER = "tea_leaf_fox.txt";

    @Override
    public void onInitializeClient() {
        // ① 打开世界选择界面时解密（不解密已标记为作弊的存档）
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SelectWorldScreen) {
                Path savesDir = client.getLevelStorage().getSavesDirectory();
                try (DirectoryStream<Path> dirs = Files.newDirectoryStream(savesDir, Files::isDirectory)) {
                    for (Path worldDir : dirs) {
                        if (Files.exists(worldDir.resolve(ROLLBACK_MARKER))) continue;
                        if (Files.exists(worldDir.resolve("hardcore.lock"))) {
                            try {
                                HardcoreEncryption.decryptFilesInDir(worldDir);
                            } catch (Exception e) {
                                System.err.println("[真正的极限模式] 解密失败: " + worldDir.getFileName());
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[真正的极限模式] 扫描存档失败！");
                }
            }
        });

        // ② 游戏关闭前，扫描所有存档，把解密过的极限存档重新加密
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            Path savesDir = client.getLevelStorage().getSavesDirectory();
            try (DirectoryStream<Path> dirs = Files.newDirectoryStream(savesDir, Files::isDirectory)) {
                for (Path worldDir : dirs) {
                    // 跳过已有作弊标记的存档
                    if (Files.exists(worldDir.resolve(ROLLBACK_MARKER))) continue;
                    // 只有我们的极限存档才会有 hardcore_time.dat
                    // 如果存在 hardcore_time.dat 但不存在 hardcore.lock，说明被解密过，需要重新加密
                    if (Files.exists(worldDir.resolve("hardcore_time.dat")) &&
                            !Files.exists(worldDir.resolve("hardcore.lock"))) {
                        HardcoreEncryption.encryptWorldDir(worldDir);
                    }
                }
            } catch (IOException e) {
                System.err.println("[真正的极限模式] 退出时重新加密失败！");
            }
        });
    }
}