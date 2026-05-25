package com.tea_leaf_fox.hardcorefix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import java.io.IOException;
import java.nio.file.*;

public class TrueHardcoreFixClient implements ClientModInitializer {
    private static final String ROLLBACK_MARKER = "tea_leaf_fox.txt";

    @Override
    public void onInitializeClient() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SelectWorldScreen) {
                Path savesDir = client.getLevelStorage().getSavesDirectory();
                try (DirectoryStream<Path> dirs = Files.newDirectoryStream(savesDir, Files::isDirectory)) {
                    for (Path worldDir : dirs) {
                        // 检查回滚标记，如果已标记则跳过解密
                        if (Files.exists(worldDir.resolve(ROLLBACK_MARKER))) {
                            System.out.println("[真正的极限模式] 世界 " + worldDir.getFileName() + " 已被标记为作弊，不解密。");
                            continue;
                        }

                        // 只有存在 lock 文件才处理
                        if (!Files.exists(worldDir.resolve("hardcore.lock"))) continue;

                        // 回滚检测
                        if (HardcoreTimeManager.isRollback(worldDir)) {
                            // 创建标记文件，不解密
                            try {
                                Files.createFile(worldDir.resolve(ROLLBACK_MARKER));
                                System.out.println("[真正的极限模式] 已标记为回滚: " + worldDir.getFileName());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }

                        // 正常解密
                        try {
                            HardcoreEncryption.decryptFilesInDir(worldDir);
                        } catch (Exception e) {
                            System.err.println("[真正的极限模式] 解密失败: " + worldDir.getFileName());
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[真正的极限模式] 扫描存档失败！");
                    e.printStackTrace();
                }
            }
        });
    }
}