package com.tea_leaf_fox.hardcorefix;

import net.minecraft.server.world.ServerWorld;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;

public class HardcoreTimeManager {
    private static final byte[] KEY = "TimeFoxTeaLeaf2024!Secure".getBytes();
    private static final String EXTERNAL_FILE = "hardcore_time_check.dat";   // 外部（游戏目录下）
    private static final String INTERNAL_FILE = "hardcore_time.dat";         // 存档文件夹内

    private static SecretKeySpec getKeySpec() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(KEY);
        return new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
    }

    // 获取外部文件路径（游戏当前工作目录，通常为 .minecraft 或 run/）
    private static Path getExternalPath() {
        return Paths.get(EXTERNAL_FILE);
    }

    // 获取世界目录
    private static Path getWorldDir(ServerWorld world) {
        try {
            return ((com.tea_leaf_fox.hardcorefix.mixin.MinecraftServerAccessor) world.getServer())
                    .getSession()
                    .getWorldDirectory(world.getRegistryKey());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 退出时调用：同时写入内部和外部时间戳
     */
    public static void saveTime(ServerWorld world) {
        if (!world.getServer().isHardcore()) return;
        Path worldDir = getWorldDir(world);
        if (worldDir == null) return;
        long time = world.getTime();

        try {
            String worldId = worldDir.getFileName().toString();
            String data = worldId + "|" + time;
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, getKeySpec());
            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));

            // 写入外部文件
            Files.write(getExternalPath(), encrypted);
            // 写入存档内部文件
            Files.write(worldDir.resolve(INTERNAL_FILE), encrypted);
        } catch (Exception e) {
            System.err.println("[真正的极限模式] 写入时间戳失败！");
            e.printStackTrace();
        }
    }

    /**
     * 客户端解密前调用：检查是否存在回滚
     * @return true 表示正常，false 表示回滚
     */
    public static boolean isRollback(Path worldDir) {
        Path internalFile = worldDir.resolve(INTERNAL_FILE);
        Path externalFile = getExternalPath();
        if (!Files.exists(internalFile) || !Files.exists(externalFile)) {
            // 缺少任一文件，无法判断，放行
            return false;
        }

        try {
            byte[] internalData = Files.readAllBytes(internalFile);
            byte[] externalData = Files.readAllBytes(externalFile);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, getKeySpec());
            String internalStr = new String(cipher.doFinal(internalData), "UTF-8");
            String externalStr = new String(cipher.doFinal(externalData), "UTF-8");

            String[] intParts = internalStr.split("\\|");
            String[] extParts = externalStr.split("\\|");
            if (intParts.length != 2 || extParts.length != 2) return false;

            String intWorldId = intParts[0];
            long intTime = Long.parseLong(intParts[1]);
            String extWorldId = extParts[0];
            long extTime = Long.parseLong(extParts[1]);

            // 世界名必须一致，且外部时间严格大于内部时间，说明外部记录更新，内部是旧备份
            if (intWorldId.equals(extWorldId) && extTime > intTime) {
                System.err.println("[真正的极限模式] 检测到回滚！内部时间:" + intTime + " 外部时间:" + extTime);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[真正的极限模式] 回滚检测异常，放行。");
            e.printStackTrace();
            return false;
        }
    }
}