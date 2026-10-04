/*
 * Decompiled with CFR 0.152.
 */
package com.pskin;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class SharedFileIO {
    private static final Object JVM_LOCK = new Object();

    private SharedFileIO() {
    }

    public static Handle acquire(File dataFile) {
        try {
            File lockFile = new File(dataFile.getAbsoluteFile().getParentFile(), dataFile.getName() + ".lock");
            File parent = lockFile.getAbsoluteFile().getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            FileChannel ch = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
            FileLock fl = null;
            try {
                fl = ch.lock();
            }
            catch (Exception lockEx) {
                fl = null;
            }
            return new Handle(ch, fl, lockFile);
        }
        catch (Exception e) {
            return null;
        }
    }

    public static void atomicWrite(File target, String content) throws Exception {
        File parent = target.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tmp = new File(parent, target.getName() + ".tmp");
        try (OutputStreamWriter w = new OutputStreamWriter(Files.newOutputStream(tmp.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
            w.write(content);
            ((Writer)w).flush();
        }
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void lockedWrite(File target, String content) {
        Object object = JVM_LOCK;
        synchronized (object) {
            try (Handle h = SharedFileIO.acquire(target)) {
                SharedFileIO.atomicWrite(target, content);
            } catch (Exception e) {
                throw new RuntimeException("lockedWrite failed: " + target.getName(), e);
            }
        }
    }

    public static long mtime(File f) {
        try {
            if (f == null || !f.exists()) {
                return 0L;
            }
            return f.lastModified();
        }
        catch (Exception e) {
            return 0L;
        }
    }

    public static final class Handle
    implements AutoCloseable {
        private final FileChannel channel;
        private final FileLock lock;
        private final File lockFile;

        private Handle(FileChannel channel, FileLock lock, File lockFile) {
            this.channel = channel;
            this.lock = lock;
            this.lockFile = lockFile;
        }

        public boolean isValid() {
            return this.channel != null && this.channel.isOpen();
        }

        @Override
        public void close() {
            try {
                if (this.lock != null && this.lock.isValid()) {
                    this.lock.release();
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                if (this.channel != null && this.channel.isOpen()) {
                    this.channel.close();
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }

        public File getLockFile() {
            return this.lockFile;
        }
    }
}
