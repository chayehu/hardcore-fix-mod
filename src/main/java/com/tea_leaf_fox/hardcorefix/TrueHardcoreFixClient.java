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
        // ① 打开世界选择界面：检测回滚、解密正常存档
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SelectWorldScreen) {
                Path savesDir = client.getLevelStorage().getSavesDirectory();
                try (DirectoryStream<Path> dirs = Files.newDirectoryStream(savesDir, Files::isDirectory)) {
                    for (Path worldDir : dirs) {
                        // 跳过已被标记回滚的存档
                        if (Files.exists(worldDir.resolve(ROLLBACK_MARKER))) {
                            System.out.println("[真正的极限模式] 世界 " + worldDir.getFileName() + " 已被标记，跳过。");
                            continue;
                        }

                        // 检测回滚
                        if (HardcoreTimeManager.isRollback(worldDir)) {
                            try {
                                // 1. 创建回滚标记
                                Files.createFile(worldDir.resolve(ROLLBACK_MARKER));
                                // 2. 强制加密世界文件（确保文件是乱码）
                                HardcoreEncryption.encryptWorldDir(worldDir);
                                // 3. 删除 hardcore.lock → 模组永远不再解密
                                Files.deleteIfExists(worldDir.resolve("hardcore.lock"));
                                // 4. 删除 hardcore_time.dat → 退出时不会误操作
                                Files.deleteIfExists(worldDir.resolve("hardcore_time.dat"));
                                System.out.println("[真正的极限模式] 已永久封禁世界: " + worldDir.getFileName());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            continue; // 不再解密
                        }

                        // 正常解密
                        if (Files.exists(worldDir.resolve("hardcore.lock"))) {
                            try {
                                HardcoreEncryption.decryptFilesInDir(worldDir);
                                System.out.println("[真正的极限模式] 解密成功: " + worldDir.getFileName());
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

        // ② 游戏关闭前：把已解密但未进入的极限存档重新加密
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            Path savesDir = client.getLevelStorage().getSavesDirectory();
            try (DirectoryStream<Path> dirs = Files.newDirectoryStream(savesDir, Files::isDirectory)) {
                for (Path worldDir : dirs) {
                    if (Files.exists(worldDir.resolve(ROLLBACK_MARKER))) continue;
                    if (Files.exists(worldDir.resolve("hardcore_time.dat")) &&
                            !Files.exists(worldDir.resolve("hardcore.lock"))) {
                        HardcoreEncryption.encryptWorldDir(worldDir);
                        System.out.println("[真正的极限模式] 退出时重新加密: " + worldDir.getFileName());
                    }
                }
            } catch (IOException e) {
                System.err.println("[真正的极限模式] 退出时重新加密失败！");
            }
        });
    }
}