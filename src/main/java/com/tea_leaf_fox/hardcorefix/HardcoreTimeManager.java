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
    private static final String EXTERNAL_FILE = "hardcore_time_check.dat";
    private static final String INTERNAL_FILE = "hardcore_time.dat";

    private static SecretKeySpec getKeySpec() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(KEY);
        return new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
    }

    private static Path getExternalPath() {
        return Paths.get(EXTERNAL_FILE);
    }

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
     * 保存时间戳：外部文件和内部文件都只记录世界ID + 游戏时间
     */
    public static void saveTime(ServerWorld world) {
        if (!world.getServer().isHardcore()) return;
        Path worldDir = getWorldDir(world);
        if (worldDir == null) return;

        long gameTime = world.getTime();
        String worldId = worldDir.getFileName().toString();

        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, getKeySpec());

            String data = worldId + "|" + gameTime;
            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));

            // 写入外部文件（游戏根目录）
            Files.write(getExternalPath(), encrypted);
            // 写入内部文件（存档文件夹内）
            Files.write(worldDir.resolve(INTERNAL_FILE), encrypted);

            System.out.println("[真正的极限模式] 时间戳已保存: 世界=" + worldId + ", 游戏时间=" + gameTime);
        } catch (Exception e) {
            System.err.println("[真正的极限模式] 写入时间戳失败！");
            e.printStackTrace();
        }
    }

    /**
     * 回滚检测：外部游戏时间 > 内部游戏时间 → 回滚
     */
    public static boolean isRollback(Path worldDir) {
        Path internalFile = worldDir.resolve(INTERNAL_FILE);
        Path externalFile = getExternalPath();
        if (!Files.exists(internalFile) || !Files.exists(externalFile)) {
            System.out.println("[真正的极限模式] 回滚检测: 缺少文件 (内部=" + Files.exists(internalFile) + ", 外部=" + Files.exists(externalFile) + ")");
            return false;
        }

        try {
            byte[] extData = Files.readAllBytes(externalFile);
            byte[] intData = Files.readAllBytes(internalFile);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, getKeySpec());
            String extStr = new String(cipher.doFinal(extData), "UTF-8");
            String intStr = new String(cipher.doFinal(intData), "UTF-8");

            String[] extParts = extStr.split("\\|");
            String[] intParts = intStr.split("\\|");
            if (extParts.length != 2 || intParts.length != 2) {
                System.out.println("[真正的极限模式] 回滚检测: 数据格式错误");
                return false;
            }

            String extWorldId = extParts[0];
            long extGameTime = Long.parseLong(extParts[1]);

            String intWorldId = intParts[0];
            long intGameTime = Long.parseLong(intParts[1]);

            System.out.println("[真正的极限模式] 回滚检测: 外部时间=" + extGameTime + ", 内部时间=" + intGameTime + ", 世界=" + extWorldId);

            // 世界名必须相同，且外部游戏时间严格大于内部游戏时间
            if (extWorldId.equals(intWorldId) && extGameTime > intGameTime) {
                System.out.println("[真正的极限模式] 检测到回滚！");
                return true;
            } else {
                System.out.println("[真正的极限模式] 未检测到回滚。");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[真正的极限模式] 回滚检测异常，放行。");
            e.printStackTrace();
            return false;
        }
    }
}