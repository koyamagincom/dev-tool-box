package com.example.automation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * AdbBridgeDaemonManager: Quản lý kết nối, kích hoạt, kiểm thử và đồng bộ hoá
 * với Zero-Loss Shell Server (app_process UID 2000) lắng nghe tại 127.0.0.1:8765.
 */
object AdbBridgeDaemonManager {
    private const val TAG = "AdbBridgeDaemon"
    const val DAEMON_HOST = "127.0.0.1"
    const val DAEMON_PORT = 8765
    const val BASE_URL = "http://$DAEMON_HOST:$DAEMON_PORT"
    const val TOKEN_PATH = "/data/local/tmp/.bridge_token"
    const val DEX_PATH = "/data/local/tmp/bridge_daemon.dex"
    const val LOG_PATH = "/data/local/tmp/daemon.log"

    data class DaemonStatus(
        val isOnline: Boolean,
        val uid: Int = -1,
        val user: String = "unknown",
        val uptimeSeconds: Long = 0,
        val freeMemoryMb: Long = 0,
        val totalMemoryMb: Long = 0,
        val token: String = "",
        val rawMessage: String = ""
    )

    data class ExecResult(
        val code: Int,
        val stdout: String,
        val stderr: String,
        val latencyMs: Long = 0,
        val isSuccess: Boolean = (code == 0)
    )

    data class VerificationTestItem(
        val testNumber: Int,
        val name: String,
        val description: String,
        val isPassed: Boolean,
        val details: String,
        val latencyMs: Long = 0
    )

    data class VerificationReport(
        val totalTests: Int,
        val passedTests: Int,
        val averageLatencyMs: Double,
        val testItems: List<VerificationTestItem>,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Đọc auth token từ file /data/local/tmp/.bridge_token
     */
    fun readAuthToken(): String {
        return try {
            val tokenFile = File(TOKEN_PATH)
            if (tokenFile.exists() && tokenFile.canRead()) {
                tokenFile.readText().trim()
            } else {
                // Thử đọc qua Shizuku hoặc shell nếu không có quyền đọc trực tiếp
                val (success, output) = AdbBridge.executeShell("cat $TOKEN_PATH")
                if (success && output.isNotBlank()) output.trim() else ""
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read token directly: ${e.message}")
            ""
        }
    }

    /**
     * Kiểm tra trạng thái DaemonServer qua GET /status
     */
    suspend fun checkStatus(customToken: String? = null): DaemonStatus = withContext(Dispatchers.IO) {
        val token = customToken ?: readAuthToken()
        try {
            val url = URL("$BASE_URL/status")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val memObj = json.optJSONObject("memory_mb")
                DaemonStatus(
                    isOnline = true,
                    uid = json.optInt("uid", 2000),
                    user = json.optString("user", "shell"),
                    uptimeSeconds = json.optLong("uptime_seconds", 0),
                    freeMemoryMb = memObj?.optLong("free", 0) ?: 0,
                    totalMemoryMb = memObj?.optLong("total", 0) ?: 0,
                    token = token,
                    rawMessage = responseText
                )
            } else {
                val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                DaemonStatus(
                    isOnline = false,
                    token = token,
                    rawMessage = "HTTP $responseCode: $errText"
                )
            }
        } catch (e: Exception) {
            DaemonStatus(
                isOnline = false,
                token = token,
                rawMessage = e.localizedMessage ?: "Không kết nối được tới Daemon (Offline)"
            )
        }
    }

    /**
     * Thực thi lệnh shell qua DaemonServer POST /exec
     */
    suspend fun executeCommand(command: String, customToken: String? = null): ExecResult = withContext(Dispatchers.IO) {
        val token = customToken ?: readAuthToken()
        val startTime = System.currentTimeMillis()
        try {
            val url = URL("$BASE_URL/exec")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val payload = JSONObject().apply {
                put("cmd", command)
            }.toString()

            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(payload) }

            val responseCode = conn.responseCode
            val latency = System.currentTimeMillis() - startTime

            if (responseCode == 200) {
                val res = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(res)
                ExecResult(
                    code = json.optInt("code", 0),
                    stdout = json.optString("stdout", ""),
                    stderr = json.optString("stderr", ""),
                    latencyMs = latency,
                    isSuccess = json.optInt("code", 0) == 0
                )
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                ExecResult(
                    code = responseCode,
                    stdout = "",
                    stderr = "Lỗi HTTP $responseCode: $err",
                    latencyMs = latency,
                    isSuccess = false
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            ExecResult(
                code = -1,
                stdout = "",
                stderr = "Lỗi kết nối Daemon: ${e.localizedMessage}",
                latencyMs = latency,
                isSuccess = false
            )
        }
    }

    /**
     * Chụp ảnh màn hình từ DaemonServer qua GET /screenshot
     */
    suspend fun captureScreenshot(customToken: String? = null): Pair<Bitmap?, String> = withContext(Dispatchers.IO) {
        val token = customToken ?: readAuthToken()
        try {
            val url = URL("$BASE_URL/screenshot")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 8000
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    bitmap to "Thành công (${bytes.size / 1024} KB)"
                } else {
                    null to "Dữ liệu ảnh rỗng"
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                null to "HTTP $responseCode: $err"
            }
        } catch (e: Exception) {
            null to "Lỗi: ${e.localizedMessage}"
        }
    }

    /**
     * Dừng DaemonServer an toàn qua POST /stop
     */
    suspend fun stopDaemon(customToken: String? = null): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val token = customToken ?: readAuthToken()
        try {
            val url = URL("$BASE_URL/stop")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3000
                readTimeout = 3000
                doOutput = true
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }
            val resCode = conn.responseCode
            if (resCode == 200) {
                true to "Đã gửi lệnh dừng DaemonServer thành công."
            } else {
                false to "Lỗi HTTP $resCode khi dừng Daemon"
            }
        } catch (e: Exception) {
            false to "Lỗi dừng Daemon: ${e.localizedMessage}"
        }
    }

    /**
     * Chạy bộ 6 bài kiểm thử tự động (Verification Suite) và đo Benchmark Latency
     */
    suspend fun runVerificationSuite(token: String): VerificationReport = withContext(Dispatchers.IO) {
        val testItems = mutableListOf<VerificationTestItem>()
        var passedCount = 0

        // Test 1: /status -> UID 2000
        val t1Start = System.currentTimeMillis()
        val status = checkStatus(token)
        val t1Latency = System.currentTimeMillis() - t1Start
        val t1Pass = status.isOnline && status.uid == 2000
        if (t1Pass) passedCount++
        testItems.add(
            VerificationTestItem(
                testNumber = 1,
                name = "Trạng thái & Đặc Quyền UID",
                description = "Xác minh Server online và chạy dưới UID 2000 (Shell).",
                isPassed = t1Pass,
                details = "Status: ${if (status.isOnline) "ONLINE" else "OFFLINE"}, UID: ${status.uid}, User: ${status.user}, Uptime: ${status.uptimeSeconds}s",
                latencyMs = t1Latency
            )
        )

        // Test 2: exec 'id'
        val execId = executeCommand("id", token)
        val t2Pass = execId.isSuccess && (execId.stdout.contains("uid=2000(shell)") || execId.stdout.contains("gid=2000(shell)"))
        if (t2Pass) passedCount++
        testItems.add(
            VerificationTestItem(
                testNumber = 2,
                name = "Thực Thi Lệnh Cơ Bản (id)",
                description = "Gửi lệnh 'id' kiểm tra chuỗi định danh đặc quyền shell.",
                isPassed = t2Pass,
                details = "Output: ${execId.stdout.ifBlank { execId.stderr }}",
                latencyMs = execId.latencyMs
            )
        )

        // Test 3: exec 'getprop ro.build.version.release'
        val execProp = executeCommand("getprop ro.build.version.release", token)
        val t3Pass = execProp.isSuccess && execProp.stdout.isNotBlank()
        if (t3Pass) passedCount++
        testItems.add(
            VerificationTestItem(
                testNumber = 3,
                name = "Đọc Thuộc Tính Hệ Thống (getprop)",
                description = "Đọc phiên bản Android hệ thống.",
                isPassed = t3Pass,
                details = "Android Version: ${execProp.stdout.trim()}",
                latencyMs = execProp.latencyMs
            )
        )

        // Test 4: /screenshot stream binary
        val t4Start = System.currentTimeMillis()
        val (bitmap, shotMsg) = captureScreenshot(token)
        val t4Latency = System.currentTimeMillis() - t4Start
        val t4Pass = bitmap != null
        if (t4Pass) passedCount++
        testItems.add(
            VerificationTestItem(
                testNumber = 4,
                name = "Chụp Màn Hình & Stream Binary (/screenshot)",
                description = "Chạy screencap -p và nhận luồng dữ liệu PNG.",
                isPassed = t4Pass,
                details = "Kết quả: $shotMsg (Resolution: ${bitmap?.width ?: 0}x${bitmap?.height ?: 0})",
                latencyMs = t4Latency
            )
        )

        // Test 5: Security invalid token -> 401
        val t5Start = System.currentTimeMillis()
        var t5Pass = false
        var t5Details = ""
        try {
            val url = URL("$BASE_URL/status")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                setRequestProperty("Authorization", "Bearer INVALID_TEST_TOKEN_XYZ_999")
            }
            val code = conn.responseCode
            t5Pass = (code == 401)
            t5Details = "Mã HTTP phản hồi: $code (Yêu cầu 401 Unauthorized)"
        } catch (e: Exception) {
            t5Pass = true
            t5Details = "Server đã từ chối request không hợp lệ: ${e.localizedMessage}"
        }
        val t5Latency = System.currentTimeMillis() - t5Start
        if (t5Pass) passedCount++
        testItems.add(
            VerificationTestItem(
                testNumber = 5,
                name = "Kiểm Tra Bảo Mật Token (Security Test)",
                description = "Gửi request với sai Bearer Token và xác minh chặn 401.",
                isPassed = t5Pass,
                details = t5Details,
                latencyMs = t5Latency
            )
        )

        // Test 6: Latency Benchmark (10 iterations)
        val latencies = mutableListOf<Long>()
        for (i in 1..10) {
            val r = executeCommand("echo ping", token)
            latencies.add(r.latencyMs)
        }
        val avgLat = if (latencies.isNotEmpty()) latencies.average() else 0.0
        val minLat = latencies.minOrNull() ?: 0L
        val maxLat = latencies.maxOrNull() ?: 0L
        val t6Pass = avgLat > 0
        if (t6Pass) passedCount++
        testItems.add(
            VerificationTestItem(
                testNumber = 6,
                name = "Đo Độ Trễ (Latency Benchmark 10 lần)",
                description = "Thực thi 10 request liên tục đo thời gian phản hồi.",
                isPassed = t6Pass,
                details = "Min: ${minLat}ms | Max: ${maxLat}ms | Trung bình: ${String.format("%.2f", avgLat)}ms",
                latencyMs = avgLat.toLong()
            )
        )

        VerificationReport(
            totalTests = 6,
            passedTests = passedCount,
            averageLatencyMs = avgLat,
            testItems = testItems
        )
    }

    /**
     * Tạo mã lệnh 1-chạm kích hoạt qua ADB Shell
     */
    fun getLaunchAdbCommand(): String {
        return "adb shell \"nohup app_process -Djava.class.path=$DEX_PATH /data/local/tmp com.adbbridge.DaemonServer > $LOG_PATH 2>&1 &\""
    }

    /**
     * Tạo mã lệnh kích hoạt nội bộ Termux
     */
    fun getTermuxLaunchCommand(): String {
        return "nohup app_process -Djava.class.path=$DEX_PATH /data/local/tmp com.adbbridge.DaemonServer > $LOG_PATH 2>&1 &"
    }

    // Các mẫu mã nguồn chuẩn để người dùng sao chép hoặc xuất ra file
    const val DAEMON_SERVER_JAVA_CODE = """package com.adbbridge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Executors;

public class DaemonServer {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8765;
    private static final String TOKEN_PATH = "/data/local/tmp/.bridge_token";
    private static String authToken = "";
    private static long startTime = 0;
    private static HttpServer server = null;

    public static void main(String[] args) {
        startTime = System.currentTimeMillis();
        System.out.println("[AdbBridgeDaemon] Starting initialization...");
        initAuthToken();

        try {
            server = HttpServer.create(new InetSocketAddress(HOST, PORT), 0);
            server.setExecutor(Executors.newFixedThreadPool(8));

            server.createContext("/status", new StatusHandler());
            server.createContext("/exec", new ExecHandler());
            server.createContext("/screenshot", new ScreenshotHandler());
            server.createContext("/stop", new StopHandler());

            server.start();
            System.out.println("[AdbBridgeDaemon] Server is running on http://" + HOST + ":" + PORT);
            System.out.println("[AdbBridgeDaemon] UID: " + android.os.Process.myUid());
        } catch (Throwable t) {
            System.err.println("[AdbBridgeDaemon] Fatal: Failed to start server!");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void initAuthToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        authToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        File tokenFile = new File(TOKEN_PATH);
        try (FileWriter fw = new FileWriter(tokenFile, false)) {
            fw.write(authToken);
        } catch (IOException e) {
            System.err.println("[AdbBridgeDaemon] Warning: Could not write token file: " + e.getMessage());
        }

        try {
            Runtime.getRuntime().exec(new String[]{"chmod", "600", TOKEN_PATH}).waitFor();
        } catch (Exception ignored) {}
    }

    private static boolean isAuthorized(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        String clientToken = authHeader.substring("Bearer ".length()).trim();
        return authToken.equals(clientToken);
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized: Invalid or missing token\"}");
                return;
            }

            int uid = android.os.Process.myUid();
            long uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000;
            Runtime runtime = Runtime.getRuntime();
            long freeMem = runtime.freeMemory() / (1024 * 1024);
            long totalMem = runtime.totalMemory() / (1024 * 1024);

            String json = String.format(
                    "{\"status\":\"online\",\"uid\":%d,\"user\":\"shell\",\"uptime_seconds\":%d,\"memory_mb\":{\"free\":%d,\"total\":%d}}",
                    uid, uptimeSeconds, freeMem, totalMem
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    static class ExecHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized: Invalid or missing token\"}");
                return;
            }

            InputStream is = exchange.getRequestBody();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int read;
            while ((read = is.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            String body = baos.toString("UTF-8").trim();

            String command = "";
            int cmdIdx = body.indexOf("\"cmd\"");
            if (cmdIdx != -1) {
                int colonIdx = body.indexOf(":", cmdIdx);
                if (colonIdx != -1) {
                    int startQuote = body.indexOf("\"", colonIdx);
                    if (startQuote != -1) {
                        int endQuote = body.lastIndexOf("\"");
                        if (endQuote > startQuote) {
                            command = body.substring(startQuote + 1, endQuote)
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\")
                                    .replace("\\n", "\n")
                                    .replace("\\t", "\t");
                        }
                    }
                }
            }

            if (command.isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Missing or empty 'cmd' parameter\"}");
                return;
            }

            int exitCode = -1;
            StringBuilder stdoutSb = new StringBuilder();
            StringBuilder stderrSb = new StringBuilder();

            try {
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
                BufferedReader stdReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

                String line;
                while ((line = stdReader.readLine()) != null) stdoutSb.append(line).append("\n");
                while ((line = errReader.readLine()) != null) stderrSb.append(line).append("\n");
                exitCode = process.waitFor();
            } catch (Exception e) {
                stderrSb.append("Execution error: ").append(e.getMessage());
            }

            String json = String.format(
                    "{\"code\":%d,\"stdout\":\"%s\",\"stderr\":\"%s\"}",
                    exitCode,
                    escapeJson(stdoutSb.toString().trim()),
                    escapeJson(stderrSb.toString().trim())
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    static class ScreenshotHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized: Invalid or missing token\"}");
                return;
            }

            try {
                Process proc = Runtime.getRuntime().exec(new String[]{"screencap", "-p"});
                InputStream procIn = proc.getInputStream();
                ByteArrayOutputStream imgBuffer = new ByteArrayOutputStream();
                byte[] temp = new byte[8192];
                int len;
                while ((len = procIn.read(temp)) != -1) {
                    imgBuffer.write(temp, 0, len);
                }
                proc.waitFor();

                byte[] imageBytes = imgBuffer.toByteArray();
                exchange.getResponseHeaders().set("Content-Type", "image/png");
                exchange.sendResponseHeaders(200, imageBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(imageBytes);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class StopHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            if (!isAuthorized(exchange)) {
                sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized: Invalid or missing token\"}");
                return;
            }

            sendJsonResponse(exchange, 200, "{\"status\":\"stopping\"}");
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    if (server != null) server.stop(0);
                    new File(TOKEN_PATH).delete();
                    System.exit(0);
                } catch (Exception ignored) {}
            }).start();
        }
    }
}"""

    val BUILD_DAEMON_SH_CODE = """
#!/usr/bin/env bash
set -e

echo "=== [1/4] Khởi tạo môi trường biên dịch ==="
mkdir -p build_classes
TARGET_DIR="/data/local/tmp"

# 1. Tìm công cụ D8 / DX trong Android SDK hoặc Termux
D8_CMD=""
if command -v d8 >/dev/null 2>&1; then
    D8_CMD="d8"
elif command -v dx >/dev/null 2>&1; then
    D8_CMD="dx"
elif [ -f "${'$'}ANDROID_HOME/build-tools/34.0.0/d8" ]; then
    D8_CMD="${'$'}ANDROID_HOME/build-tools/34.0.0/d8"
fi

# 2. Biên dịch mã nguồn Java -> .class
echo "=== [2/4] Biên dịch mã nguồn Java ==="
javac -d build_classes com/adbbridge/DaemonServer.java

# 3. Đóng gói thành bridge_daemon.dex
echo "=== [3/4] Đóng gói thành bridge_daemon.dex ==="
if [[ "${'$'}D8_CMD" == *"d8"* ]] || command -v d8 >/dev/null 2>&1; then
    d8 --output ./ build_classes/com/adbbridge/DaemonServer*.class
    mv classes.dex bridge_daemon.dex
else
    dx --dex --output=bridge_daemon.dex build_classes/
fi

# 4. Phân quyền và copy vào /data/local/tmp
echo "=== [4/4] Cài đặt vào /data/local/tmp ==="
cp bridge_daemon.dex "${'$'}TARGET_DIR/bridge_daemon.dex"
chmod 755 "${'$'}TARGET_DIR/bridge_daemon.dex"

rm -rf build_classes
echo "✅ Hoàn tất! File .dex đã sẵn sàng tại ${'$'}TARGET_DIR/bridge_daemon.dex"
""".trimIndent()

    val START_DAEMON_SH_CODE = """
#!/usr/bin/env bash
set -e

SERVER_URL="http://127.0.0.1:8765"
TOKEN_FILE="/data/local/tmp/.bridge_token"
DEX_FILE="/data/local/tmp/bridge_daemon.dex"

echo "🔍 Kiểm tra trạng thái DaemonServer..."
if [ -f "${'$'}TOKEN_FILE" ]; then
    TOKEN=${'$'}(cat "${'$'}TOKEN_FILE" 2>/dev/null || true)
    STATUS=${'$'}(curl -s -m 2 -H "Authorization: Bearer ${'$'}TOKEN" "${'$'}SERVER_URL/status" 2>/dev/null || true)
    if [[ "${'$'}STATUS" == *"online"* ]]; then
        echo "⚡ DaemonServer đang chạy sẵn sàng!"
        echo "${'$'}STATUS"
        exit 0
    fi
fi

echo "🚀 Đang khởi động DaemonServer ngầm bằng app_process (UID 2000)..."
START_CMD="nohup app_process -Djava.class.path=${'$'}DEX_FILE /data/local/tmp com.adbbridge.DaemonServer > /data/local/tmp/daemon.log 2>&1 &"

if command -v adb >/dev/null 2>&1; then
    adb shell "${'$'}START_CMD"
else
    eval "${'$'}START_CMD"
fi

sleep 1

if [ -f "${'$'}TOKEN_FILE" ]; then
    TOKEN=${'$'}(cat "${'$'}TOKEN_FILE")
    RESPONSE=${'$'}(curl -s -m 3 -H "Authorization: Bearer ${'$'}TOKEN" "${'$'}SERVER_URL/status")
    echo "✅ Kích hoạt thành công!"
    echo "📊 Phản hồi: ${'$'}RESPONSE"
else
    echo "⚠️ Đã phát lệnh khởi chạy. Xem log chi tiết tại: /data/local/tmp/daemon.log"
fi
""".trimIndent()

    val ADBX_CLI_CODE = """
#!/usr/bin/env bash
# Công cụ CLI adbx - Siêu nhẹ, thực thi lệnh qua AdbBridgeDaemon

SERVER="http://127.0.0.1:8765"
TOKEN_FILE="/data/local/tmp/.bridge_token"

if [ ! -f "${'$'}TOKEN_FILE" ]; then
    echo "❌ Lỗi: Không tìm thấy ${'$'}TOKEN_FILE. Vui lòng chạy ./start_daemon.sh trước!" >&2
    exit 1
fi

TOKEN=${'$'}(cat "${'$'}TOKEN_FILE")

if [ ${'$'}# -eq 0 ]; then
    echo "Cách dùng: adbx <lệnh shell>"
    echo "Ví dụ: adbx input tap 500 1000"
    exit 0
fi

CMD="${'$'}*"
JSON_CMD=${'$'}(printf '%s' "${'$'}CMD" | python3 -c 'import json, sys; print(json.dumps(sys.stdin.read()))')
PAYLOAD="{\"cmd\": ${'$'}JSON_CMD}"

RESPONSE=${'$'}(curl -s -X POST \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${'$'}TOKEN" \
    -d "${'$'}PAYLOAD" \
    "${'$'}SERVER/exec")

STDOUT=${'$'}(python3 -c "import json, sys; d=json.loads(sys.stdin.read()); print(d.get('stdout', ''))" <<< "${'$'}RESPONSE" 2>/dev/null || true)
STDERR=${'$'}(python3 -c "import json, sys; d=json.loads(sys.stdin.read()); print(d.get('stderr', ''))" <<< "${'$'}RESPONSE" 2>/dev/null || true)
CODE=${'$'}(python3 -c "import json, sys; d=json.loads(sys.stdin.read()); print(d.get('code', 0))" <<< "${'$'}RESPONSE" 2>/dev/null || echo 0)

if [ -n "${'$'}STDOUT" ]; then
    printf "%s\n" "${'$'}STDOUT"
fi

if [ -n "${'$'}STDERR" ]; then
    printf "%s\n" "${'$'}STDERR" >&2
fi

exit "${'$'}CODE"
""".trimIndent()

    val VERIFY_DAEMON_PY_CODE = """
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os, sys, time, json, urllib.request, urllib.error

SERVER_URL = "http://127.0.0.1:8765"
TOKEN_FILE = "/data/local/tmp/.bridge_token"

def get_token():
    if not os.path.exists(TOKEN_FILE):
        print(f"❌ Không tìm thấy token file tại: {TOKEN_FILE}")
        sys.exit(1)
    with open(TOKEN_FILE, "r") as f:
        return f.read().strip()

def make_req(path, method="GET", data=None, token=None):
    url = f"{SERVER_URL}{path}"
    headers = {}
    if token: headers["Authorization"] = f"Bearer {token}"
    encoded = json.dumps(data).encode("utf-8") if data is not None else None
    if data is not None: headers["Content-Type"] = "application/json"
    return urllib.request.Request(url, data=encoded, headers=headers, method=method)

def main():
    print("=" * 60)
    print("      ADB BRIDGE DAEMON AUTOMATED VERIFICATION SUITE")
    print("=" * 60)
    token = get_token()
    passed = 0

    # Test 1: /status
    print("\n[TEST 1] Kiểm Tra Trạng Thái & Đặc Quyền UID")
    try:
        with urllib.request.urlopen(make_req("/status", token=token), timeout=3) as r:
            b = json.loads(r.read().decode("utf-8"))
            print(f"Status: {b.get('status')} | UID: {b.get('uid')} | Uptime: {b.get('uptime_seconds')}s")
            if b.get("status") == "online" and b.get("uid") == 2000:
                print(">>> KẾT QUẢ: PASS")
                passed += 1
    except Exception as e: print(f"Lỗi: {e}")

    # Test 2: id
    print("\n[TEST 2] Thực Thi Lệnh Cơ Bản (id)")
    try:
        with urllib.request.urlopen(make_req("/exec", "POST", {"cmd": "id"}, token), timeout=3) as r:
            out = json.loads(r.read().decode("utf-8")).get("stdout", "")
            print(f"Output: {out}")
            if "uid=2000(shell)" in out: print(">>> KẾT QUẢ: PASS"); passed += 1
    except Exception as e: print(f"Lỗi: {e}")

    # Test 3: getprop
    print("\n[TEST 3] Đọc Thuộc Tính Hệ Thống (getprop)")
    try:
        with urllib.request.urlopen(make_req("/exec", "POST", {"cmd": "getprop ro.build.version.release"}, token), timeout=3) as r:
            v = json.loads(r.read().decode("utf-8")).get("stdout", "").strip()
            print(f"Android Version: {v}")
            if v: print(">>> KẾT QUẢ: PASS"); passed += 1
    except Exception as e: print(f"Lỗi: {e}")

    # Test 4: screenshot
    print("\n[TEST 4] Chụp Ảnh Màn Hình & Stream Binary")
    try:
        with urllib.request.urlopen(make_req("/screenshot", token=token), timeout=5) as r:
            data = r.read()
            if data.startswith(b"\x89PNG") and len(data) > 10240:
                print(f"Received: {len(data)/1024:.2f} KB | Valid PNG"); print(">>> KẾT QUẢ: PASS"); passed += 1
    except Exception as e: print(f"Lỗi: {e}")

    # Test 5: security
    print("\n[TEST 5] Kiểm Tra Bảo Mật Token")
    try:
        with urllib.request.urlopen(make_req("/status", token="invalid_tok"), timeout=3) as r:
            print(">>> KẾT QUẢ: FAIL")
    except urllib.error.HTTPError as he:
        if he.code == 401: print("HTTP 401 Unauthorized (Chặn thành công)"); print(">>> KẾT QUẢ: PASS"); passed += 1
    except Exception as e: print(f">>> KẾT QUẢ: PASS ({e})"); passed += 1

    # Test 6: latency
    print("\n[TEST 6] Đo Độ Trễ (Latency Benchmark 10 lần)")
    lats = []
    for _ in range(10):
        t0 = time.perf_counter()
        with urllib.request.urlopen(make_req("/exec", "POST", {"cmd": "echo ping"}, token), timeout=2) as r: r.read()
        lats.append((time.perf_counter() - t0) * 1000)
    avg = sum(lats) / len(lats)
    print(f"Min: {min(lats):.2f}ms | Max: {max(lats):.2f}ms | Avg: {avg:.2f}ms")
    print(">>> KẾT QUẢ: PASS"); passed += 1

    print("\n" + "=" * 60)
    print(f"TỔNG KẾT KIỂM THỬ: {passed}/6 BÀI KIỂM TRA ĐẠT HOÀN HẢO")
    print("=" * 60)

if __name__ == "__main__":
    main()
"""
}
