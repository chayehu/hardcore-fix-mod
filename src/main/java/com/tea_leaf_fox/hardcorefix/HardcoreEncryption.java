package com.tea_leaf_fox.hardcorefix;

import net.minecraft.server.world.ServerWorld;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;

public class HardcoreEncryption {
    private static final byte[] KEY = "TeaLeafFoxHardcore@2024!SecureKey".getBytes();
    private static final String LOCK_FILE = "hardcore.lock";

    private static SecretKeySpec getKeySpec() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(KEY);
        return new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
    }

    // 通过 ServerWorld 加密（供服务端事件使用）
    public static void encryptLevelFiles(ServerWorld world) {
        if (!world.getServer().isHardcore()) return;
        Path worldDir = getWorldDirectory(world);
        encryptWorldDir(worldDir);
    }

    // 通过路径加密（供客户端调用）
    public static void encryptWorldDir(Path worldDir) {
        if (worldDir == null) return;
        Path lockFile = worldDir.resolve(LOCK_FILE);
        if (Files.exists(lockFile)) return; // 已加密，跳过

        try {
            Files.createFile(lockFile);
        } catch (IOException e) {
            System.err.println("[真正的极限模式] 无法创建锁文件，加密取消。");
            return;
        }

        try {
            encryptFile(worldDir.resolve("level.dat"));
            encryptFile(worldDir.resolve("level.dat_old"));
        } catch (Exception e) {
            System.err.println("[真正的极限模式] 加密失败！");
            try { Files.deleteIfExists(lockFile); } catch (IOException ignored) {}
        }
    }

    // 解密整个文件夹
    public static void decryptFilesInDir(Path worldDir) {
        Path lockFile = worldDir.resolve(LOCK_FILE);
        if (!Files.exists(lockFile)) return;
        try {
            decryptFile(worldDir.resolve("level.dat"));
            Path oldDat = worldDir.resolve("level.dat_old");
            if (Files.exists(oldDat)) decryptFile(oldDat);
            Files.delete(lockFile);
        } catch (Exception e) {
            System.err.println("[真正的极限模式] 解密失败！");
        }
    }

    private static void encryptFile(Path path) throws Exception {
        if (!Files.exists(path)) return;
        byte[] raw = Files.readAllBytes(path);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, getKeySpec());
        byte[] encrypted = cipher.doFinal(raw);
        Files.write(path, encrypted);
    }

    private static void decryptFile(Path path) throws Exception {
        if (!Files.exists(path)) return;
        byte[] encrypted = Files.readAllBytes(path);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, getKeySpec());
        byte[] decrypted = cipher.doFinal(encrypted);
        Files.write(path, decrypted);
    }

    private static Path getWorldDirectory(ServerWorld world) {
        try {
            return ((com.tea_leaf_fox.hardcorefix.mixin.MinecraftServerAccessor) world.getServer())
                    .getSession()
                    .getWorldDirectory(world.getRegistryKey());
        } catch (Exception e) {
            return null;
        }
    }
}