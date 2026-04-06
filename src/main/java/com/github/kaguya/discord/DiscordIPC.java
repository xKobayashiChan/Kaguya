package com.github.kaguya.discord;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Discord IPC client (pure Java, no native libraries required).
 * All blocking I/O runs on a dedicated daemon thread — never on the MC main thread.
 */
public class DiscordIPC {

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME     = 1;
    private static final int OP_CLOSE     = 2;

    // Sentinel that signals the worker to stop
    private static final String POISON = "\0STOP";

    private final String clientId;
    private final AtomicBoolean running   = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /** Pending SET_ACTIVITY payloads queued from the MC thread. */
    private final LinkedBlockingQueue<String> sendQueue = new LinkedBlockingQueue<>(16);

    private Thread workerThread;
    private RandomAccessFile pipe;

    public DiscordIPC(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Start the background worker. Returns immediately (non-blocking).
     * The actual pipe connection happens on the worker thread.
     */
    public void start() {
        if (running.getAndSet(true)) return;
        sendQueue.clear();

        workerThread = new Thread(this::workerLoop, "DiscordIPC-Worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Queue a SET_ACTIVITY frame. Non-blocking; safe to call from the MC thread.
     * Dropped silently if not yet connected or the queue is full.
     */
    public void setActivity(String details, String state, long startTimestampSeconds,
                            String largeImageKey, String largeImageText) {
        if (!connected.get()) return;
        String payload = buildSetActivityPayload(details, state, startTimestampSeconds, largeImageKey, largeImageText);
        sendQueue.offer(payload); // non-blocking; drops if full
    }

    /**
     * Stop the worker and close the pipe. Non-blocking.
     */
    public void stop() {
        if (!running.getAndSet(false)) return;
        // Wake up the worker if it is waiting for queue items
        sendQueue.offer(POISON);
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    // -----------------------------------------------------------------------
    // Worker loop – runs entirely on the background thread
    // -----------------------------------------------------------------------

    private void workerLoop() {
        // 1. Connect
        if (!tryConnect()) {
            running.set(false);
            return;
        }

        // 2. Start a reader sub-thread so reads don't block the send path
        Thread reader = new Thread(this::readerLoop, "DiscordIPC-Reader");
        reader.setDaemon(true);
        reader.start();

        // 3. Process outgoing queue
        try {
            while (running.get() && connected.get()) {
                String payload = sendQueue.poll(1, TimeUnit.SECONDS);
                if (payload == null) continue;
                if (payload == POISON) break;
                try {
                    sendFrame(OP_FRAME, payload);
                } catch (IOException e) {
                    connected.set(false);
                    break;
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        // 4. Close
        closePipe();
        reader.interrupt();
        connected.set(false);
        running.set(false);
    }

    private boolean tryConnect() {
        for (int i = 0; i < 10; i++) {
            if (!running.get()) return false;
            try {
                String path = getPipePath(i);
                RandomAccessFile raf = new RandomAccessFile(path, "rw");
                synchronized (this) { pipe = raf; }
                sendFrame(OP_HANDSHAKE, String.format("{\"v\":1,\"client_id\":\"%s\"}", clientId));
                connected.set(true);
                return true;
            } catch (Exception ignored) {
                // Pipe not available at index i – try next
            }
        }
        return false;
    }

    /** Reads and discards Discord responses to keep the pipe alive. */
    private void readerLoop() {
        byte[] header = new byte[8];
        while (running.get() && connected.get() && !Thread.currentThread().isInterrupted()) {
            try {
                RandomAccessFile raf;
                synchronized (this) { raf = pipe; }
                if (raf == null) break;

                int read = 0;
                while (read < 8) {
                    int r = raf.read(header, read, 8 - read);
                    if (r < 0) { connected.set(false); return; }
                    read += r;
                }
                ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
                int opcode = buf.getInt();
                int length = buf.getInt();
                if (length > 0 && length < 65536) {
                    byte[] body = new byte[length];
                    int bodyRead = 0;
                    while (bodyRead < length) {
                        int r = raf.read(body, bodyRead, length - bodyRead);
                        if (r < 0) { connected.set(false); return; }
                        bodyRead += r;
                    }
                }
                if (opcode == OP_CLOSE) {
                    connected.set(false);
                    return;
                }
            } catch (Exception e) {
                connected.set(false);
                return;
            }
        }
    }

    private void closePipe() {
        try {
            RandomAccessFile raf;
            synchronized (this) { raf = pipe; pipe = null; }
            if (raf != null) {
                try { sendFrame(OP_CLOSE, "{}"); } catch (Exception ignored) {}
                raf.close();
            }
        } catch (Exception ignored) {}
    }

    // -----------------------------------------------------------------------
    // Frame / payload helpers
    // -----------------------------------------------------------------------

    private synchronized void sendFrame(int opcode, String json) throws IOException {
        if (pipe == null) throw new IOException("pipe closed");
        byte[] data = json.getBytes("UTF-8");
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode);
        header.putInt(data.length);
        pipe.write(header.array());
        pipe.write(data);
    }

    private static String buildSetActivityPayload(String details, String state, long startTimestamp,
                                                  String largeImageKey, String largeImageText) {
        StringBuilder act = new StringBuilder("{");
        if (details != null && !details.isEmpty())
            act.append("\"details\":\"").append(escape(details)).append("\",");
        if (state != null && !state.isEmpty())
            act.append("\"state\":\"").append(escape(state)).append("\",");
        if (startTimestamp > 0)
            act.append("\"timestamps\":{\"start\":").append(startTimestamp).append("},");
        if (largeImageKey != null && !largeImageKey.isEmpty()) {
            act.append("\"assets\":{\"large_image\":\"").append(escape(largeImageKey)).append("\"");
            if (largeImageText != null && !largeImageText.isEmpty())
                act.append(",\"large_text\":\"").append(escape(largeImageText)).append("\"");
            act.append("},");
        }
        if (act.charAt(act.length() - 1) == ',') act.deleteCharAt(act.length() - 1);
        act.append("}");

        long pid = getPid();
        String nonce = UUID.randomUUID().toString();
        return String.format("{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":%d,\"activity\":%s},\"nonce\":\"%s\"}",
                pid, act, nonce);
    }

    private static String getPipePath(int index) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "\\\\.\\pipe\\discord-ipc-" + index;
        }
        String[] dirs = {
            System.getenv("XDG_RUNTIME_DIR"),
            System.getenv("TMPDIR"),
            System.getenv("TMP"),
            System.getenv("TEMP"),
            "/tmp"
        };
        for (String dir : dirs) {
            if (dir != null && !dir.isEmpty()) return dir + "/discord-ipc-" + index;
        }
        return "/tmp/discord-ipc-" + index;
    }

    private static long getPid() {
        try {
            return Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
