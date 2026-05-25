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

    // 保存后加密（原子性：先创建锁文件，成功后再加密文件）
    public static void encryptLevelFiles(ServerWorld world) {
        if (!world.getServer().isHardcore()) return;
        Path worldDir = getWorldDirectory(world);
        System.out.println("[真正的极限模式] 世界目录: " + worldDir);
        if (worldDir == null) return;

        Path lockFile = worldDir.resolve(LOCK_FILE);
        System.out.println("[真正的极限模式] 锁文件路径: " + lockFile);
        try {
            Files.createFile(lockFile);
            System.out.println("[真正的极限模式] 锁文件创建成功。");
        } catch (FileAlreadyExistsException e) {
            System.out.println("[真正的极限模式] 存档已加密，跳过。");
            return;
        } catch (IOException e) {
            System.err.println("[真正的极限模式] 无法创建锁文件，加密取消。");
            return;
        }

        try {
            encryptFile(worldDir.resolve("level.dat"), "level.dat");
            encryptFile(worldDir.resolve("level.dat_old"), "level.dat_old");
            System.out.println("[真正的极限模式] 加密完成。");
        } catch (Exception e) {
            System.err.println("[真正的极限模式] 加密失败！");
            e.printStackTrace();
            try { Files.deleteIfExists(lockFile); } catch (IOException ignored) {}
        }
    }

    // 解密整个文件夹（由客户端入口调用）
    public static void decryptFilesInDir(Path worldDir) {
        Path lockFile = worldDir.resolve(LOCK_FILE);
        System.out.println("[真正的极限模式] 检查解密: " + lockFile);
        if (!Files.exists(lockFile)) {
            System.out.println("[真正的极限模式] 未发现锁文件，无需解密。");
            return;
        }
        try {
            System.out.println("[真正的极限模式] 开始解密...");
            decryptFile(worldDir.resolve("level.dat"), "level.dat");
            Path oldDat = worldDir.resolve("level.dat_old");
            if (Files.exists(oldDat)) decryptFile(oldDat, "level.dat_old");
            Files.delete(lockFile);
            System.out.println("[真正的极限模式] 解密成功，锁文件已删除。");
        } catch (Exception e) {
            System.err.println("[真正的极限模式] 解密失败，保留加密状态。原因: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void encryptFile(Path path, String name) throws Exception {
        System.out.println("[真正的极限模式] 加密前: " + path + " 存在=" + Files.exists(path));
        if (!Files.exists(path)) return;
        byte[] raw = Files.readAllBytes(path);
        System.out.println("[真正的极限模式] " + name + " 原始大小: " + raw.length);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, getKeySpec());
        byte[] encrypted = cipher.doFinal(raw);
        System.out.println("[真正的极限模式] " + name + " 加密后大小: " + encrypted.length);
        Files.write(path, encrypted);
        // 验证写入
        byte[] verify = Files.readAllBytes(path);
        System.out.println("[真正的极限模式] 写入后文件大小: " + verify.length + " 与前一致=" + Arrays.equals(encrypted, verify));
    }

    private static void decryptFile(Path path, String name) throws Exception {
        System.out.println("[真正的极限模式] 解密前: " + path + " 存在=" + Files.exists(path));
        if (!Files.exists(path)) return;
        byte[] encrypted = Files.readAllBytes(path);
        System.out.println("[真正的极限模式] " + name + " 密文大小: " + encrypted.length);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, getKeySpec());
        byte[] decrypted = cipher.doFinal(encrypted);
        System.out.println("[真正的极限模式] " + name + " 解密后大小: " + decrypted.length);
        Files.write(path, decrypted);
    }

    private static Path getWorldDirectory(ServerWorld world) {
        try {
            Path dir = ((com.tea_leaf_fox.hardcorefix.mixin.MinecraftServerAccessor) world.getServer())
                    .getSession()
                    .getWorldDirectory(world.getRegistryKey());
            return dir;
        } catch (Exception e) {
            System.err.println("[真正的极限模式] 获取世界路径异常: " + e.getMessage());
            return null;
        }
    }
}