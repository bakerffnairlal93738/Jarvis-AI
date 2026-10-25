package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TermuxPythonAppState(
    val isRunning: Boolean = false,
    val appName: String = "",
    val appType: String = "",
    val port: Int = 5000,
    val serverUrl: String = "http://localhost:5000",
    val pythonScriptCode: String = "",
    val termuxCommand: String = "",
    val startedAt: Long = 0L,
    val statusMessage: String = "Idle"
)

class TermuxPythonManager(private val context: Context) {
    private val tag = "TermuxPythonManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(TermuxPythonAppState())
    val state: StateFlow<TermuxPythonAppState> = _state.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var currentHtmlContent: String = ""

    /**
     * Creates and runs a Python app/website in the background:
     * 1. Generates the Python script and responsive HTML/CSS/JS web app.
     * 2. Starts a local background HTTP socket server on port 5000.
     * 3. Sends the background run intent to Termux via com.termux.RUN_COMMAND.
     * 4. Updates state so the user can open it or stop it via voice commands.
     */
    suspend fun startPythonApp(
        prompt: String,
        specifiedPort: Int = 5000
    ): TermuxPythonAppState = withContext(Dispatchers.IO) {
        // Stop any previous instance first
        stopPythonAppInternal()

        val detectedType = detectAppType(prompt)
        val appName = deriveAppName(prompt, detectedType)
        val htmlContent = generateWebPageHtml(appName, detectedType, prompt)
        currentHtmlContent = htmlContent

        val pythonCode = generatePythonScript(appName, specifiedPort, htmlContent)
        val termuxCommand = generateTermuxCommand(pythonCode, specifiedPort)

        // Save Python script locally in app storage
        saveScriptLocally(pythonCode)

        // Launch background HTTP server
        startBackgroundHttpServer(specifiedPort, htmlContent)

        // Broadcast intent to Termux (runs in background)
        dispatchTermuxRunCommand(termuxCommand)

        val updatedState = TermuxPythonAppState(
            isRunning = true,
            appName = appName,
            appType = detectedType,
            port = specifiedPort,
            serverUrl = "http://localhost:$specifiedPort",
            pythonScriptCode = pythonCode,
            termuxCommand = termuxCommand,
            startedAt = System.currentTimeMillis(),
            statusMessage = "Running in background on port $specifiedPort"
        )
        _state.value = updatedState
        updatedState
    }

    /**
     * Stops the background Python server:
     * 1. Closes the local background socket server.
     * 2. Sends Termux stop intent (`pkill -f python`).
     * 3. Updates UI state to stopped.
     */
    suspend fun stopPythonApp(): Boolean = withContext(Dispatchers.IO) {
        val wasRunning = _state.value.isRunning
        stopPythonAppInternal()
        dispatchTermuxStopCommand()

        _state.update {
            it.copy(
                isRunning = false,
                statusMessage = "Stopped at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}"
            )
        }
        wasRunning
    }

    private fun stopPythonAppInternal() {
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w(tag, "Error closing server socket: ${e.message}")
        }
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
    }

    /**
     * Lightweight background HTTP Server using standard Java ServerSocket.
     * Guaranteed to work reliably on all Android versions and emulators.
     */
    private fun startBackgroundHttpServer(port: Int, html: String) {
        serverJob = scope.launch {
            try {
                val socket = ServerSocket(port)
                serverSocket = socket
                Log.i(tag, "JARVIS Background HTTP Server started on port $port")

                while (socket.isBound && !socket.isClosed) {
                    val client: Socket = try {
                        socket.accept()
                    } catch (e: Exception) {
                        break
                    }

                    launch {
                        handleClientRequest(client, html)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Background Server exception: ${e.message}", e)
            }
        }
    }

    private fun handleClientRequest(client: Socket, html: String) {
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val line = reader.readLine() // e.g. "GET / HTTP/1.1"

            val responseBody = html.toByteArray(Charsets.UTF_8)
            val output: OutputStream = client.getOutputStream()

            val header = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: text/html; charset=utf-8\r\n")
                append("Content-Length: ${responseBody.size}\r\n")
                append("Access-Control-Allow-Origin: *\r\n")
                append("Connection: close\r\n\r\n")
            }

            output.write(header.toByteArray(Charsets.UTF_8))
            output.write(responseBody)
            output.flush()
        } catch (e: Exception) {
            Log.w(tag, "Client request error: ${e.message}")
        } finally {
            try {
                client.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Sends background intent to Termux via com.termux.RUN_COMMAND service.
     */
    private fun dispatchTermuxRunCommand(command: String) {
        try {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            context.startService(intent)
            Log.i(tag, "Termux RUN_COMMAND dispatched successfully")
        } catch (e: Exception) {
            Log.w(tag, "Termux service not directly reachable (app will serve from background HTTP): ${e.message}")
        }
    }

    /**
     * Sends stop command to Termux to kill running background Python server.
     */
    private fun dispatchTermuxStopCommand() {
        try {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", "pkill -f 'jarvis_app.py' || pkill -f 'python3 -m http.server'"))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            context.startService(intent)
        } catch (e: Exception) {
            Log.w(tag, "Termux stop dispatch error: ${e.message}")
        }
    }

    fun openInBrowser() {
        val url = _state.value.serverUrl
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error opening browser for $url: ${e.message}")
        }
    }

    private fun saveScriptLocally(pythonCode: String) {
        try {
            val dir = File(context.filesDir, "termux_apps")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "jarvis_app.py")
            file.writeText(pythonCode)
        } catch (e: Exception) {
            Log.w(tag, "Failed to write script locally: ${e.message}")
        }
    }

    private fun detectAppType(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("portfolio") || lower.contains("پورٹ فولیو") || lower.contains("سی وی") || lower.contains("resume") -> "Portfolio"
            lower.contains("todo") || lower.contains("ٹوڈو") || lower.contains("task") || lower.contains("کاموں کی فہرست") -> "Task Manager"
            lower.contains("weather") || lower.contains("موسم") -> "Weather Station"
            lower.contains("calculator") || lower.contains("کیلکولیٹر") || lower.contains("حساب") -> "Scientific Calculator"
            lower.contains("chat") || lower.contains("چیٹ") || lower.contains("میسنجر") -> "Live Chat App"
            lower.contains("store") || lower.contains("shop") || lower.contains("دکان") || lower.contains("ای کامرس") || lower.contains("e-commerce") -> "E-Commerce Showcase"
            lower.contains("fitness") || lower.contains("gym") || lower.contains("ورزش") || lower.contains("جم") -> "Fitness & Workout Tracker"
            lower.contains("crypto") || lower.contains("bitcoin") || lower.contains("کرپٹو") || lower.contains("stock") -> "Crypto & Asset Hub"
            lower.contains("restaurant") || lower.contains("food") || lower.contains("کھانا") || lower.contains("ہوٹل") -> "Gourmet Bistro & Menu"
            lower.contains("dashboard") || lower.contains("ڈیش بورڈ") -> "Analytics Dashboard"
            else -> "Custom Web Studio"
        }
    }

    private fun deriveAppName(prompt: String, type: String): String {
        val cleanPrompt = prompt.trim()
        return when (type) {
            "Portfolio" -> "JARVIS Neo Portfolio"
            "Task Manager" -> "CyberTasks Pro"
            "Weather Station" -> "JARVIS Atmospheric Telemetry"
            "Scientific Calculator" -> "Quantum Math Station"
            "Live Chat App" -> "JARVIS Neural Messenger"
            "E-Commerce Showcase" -> "Stark Tech Emporium"
            "Fitness & Workout Tracker" -> "Titan Iron Fitness Core"
            "Crypto & Asset Hub" -> "Apex Quantum Crypto Terminal"
            "Gourmet Bistro & Menu" -> "Royal Oasis Cuisine Hub"
            "Analytics Dashboard" -> "JARVIS Neural Metrics Dashboard"
            else -> if (cleanPrompt.length in 3..35) cleanPrompt else "Stark Cyber Web Studio"
        }
    }

    private fun generateTermuxCommand(pythonCode: String, port: Int): String {
        val base64Code = Base64.encodeToString(pythonCode.toByteArray(), Base64.NO_WRAP)
        return "mkdir -p ~/jarvis_apps && echo '$base64Code' | base64 -d > ~/jarvis_apps/jarvis_app.py && nohup python3 ~/jarvis_apps/jarvis_app.py > ~/jarvis_apps/server.log 2>&1 &"
    }

    private fun generatePythonScript(appName: String, port: Int, htmlContent: String): String {
        val escapedHtml = htmlContent.replace("\\", "\\\\").replace("\"\"\"", "\\\"\\\"\\\"")
        return """
# ==============================================================================
# JARVIS AI SYSTEM - Termux Background Python Server
# App Name: $appName
# Port: $port
# Generated automatically by JARVIS Voice Assistant
# ==============================================================================

import sys
import os
import time
from http.server import HTTPServer, BaseHTTPRequestHandler

PORT = $port

HTML_PAGE = \"\"\"$escapedHtml\"\"\"

class JarvisHttpHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-Type', 'text/html; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(HTML_PAGE.encode('utf-8'))

    def log_message(self, format, *args):
        # Clean logging
        sys.stderr.write(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] {format % args}\n")

def run():
    server_address = ('0.0.0.0', PORT)
    httpd = HTTPServer(server_address, JarvisHttpHandler)
    print(f"==================================================")
    print(f"JARVIS Termux Server ACTIVE: http://localhost:{PORT}")
    print(f"App: $appName")
    print(f"==================================================")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping JARVIS Python Server...")
        httpd.server_close()

if __name__ == '__main__':
    run()
""".trimIndent()
    }

    private fun generateWebPageHtml(appName: String, type: String, prompt: String): String {
        val specificContent = when (type) {
            "Portfolio" -> """
                <div class="hero">
                    <div class="avatar-ring">⚡</div>
                    <h1>Tony Stark</h1>
                    <p class="subtitle">Genius, Futurist, Software Architect & Creator of JARVIS</p>
                    <div class="tags">
                        <span class="tag">Python</span>
                        <span class="tag">Android / Termux</span>
                        <span class="tag">Neural AI</span>
                        <span class="tag">Cybersecurity</span>
                    </div>
                </div>
                <div class="grid">
                    <div class="card">
                        <h3>🚀 Mark VII Arc Reactor</h3>
                        <p>High-yield clean energy synthesis system with continuous monitoring telemetry.</p>
                    </div>
                    <div class="card">
                        <h3>🤖 JARVIS Voice Protocol</h3>
                        <p>Next-gen speech input and output integration running directly on mobile hardware.</p>
                    </div>
                    <div class="card">
                        <h3>🌐 Termux Background Bridge</h3>
                        <p>Direct Python process orchestration on Android mobile terminal environments.</p>
                    </div>
                </div>
            """
            "Task Manager" -> """
                <div class="hero">
                    <h1>⚡ CyberTasks Pro</h1>
                    <p class="subtitle">Interactive Task & Routine Tracker running on Python</p>
                </div>
                <div class="card" style="max-width: 600px; margin: 0 auto;">
                    <div style="display:flex; gap: 10px; margin-bottom: 20px;">
                        <input id="taskInput" placeholder="نیا ٹاسک درج کریں (Enter task)..." style="flex:1; padding: 12px 16px; border-radius: 8px; border: 1px solid #00f0ff; background: #0b1329; color: #fff; font-size: 15px;">
                        <button onclick="addTask()" style="background: #00f0ff; color: #050b14; border:none; padding: 12px 20px; font-weight:bold; border-radius: 8px; cursor:pointer;">شامل کریں</button>
                    </div>
                    <ul id="taskList" style="list-style: none; padding: 0;">
                        <li style="padding: 10px; border-bottom: 1px solid #1e293b; display:flex; justify-content:space-between; align-items:center;">
                            <span>✅ ٹرمکس پائتھن سرور ایکٹیو کریں (Termux Python Active)</span>
                            <span style="color:#00f0ff; font-size:12px;">COMPLETE</span>
                        </li>
                        <li style="padding: 10px; border-bottom: 1px solid #1e293b; display:flex; justify-content:space-between; align-items:center;">
                            <span>⚙️ بیک گراؤنڈ پروسیس چیک کریں</span>
                            <span style="color:#38bdf8; font-size:12px;">ACTIVE</span>
                        </li>
                    </ul>
                </div>
                <script>
                    function addTask() {
                        const input = document.getElementById('taskInput');
                        const text = input.value.trim();
                        if (!text) return;
                        const list = document.getElementById('taskList');
                        const li = document.createElement('li');
                        li.style = 'padding: 10px; border-bottom: 1px solid #1e293b; display:flex; justify-content:space-between; align-items:center;';
                        li.innerHTML = '<span>📌 ' + text + '</span><button onclick="this.parentElement.remove()" style="background:#ef4444; border:none; color:#fff; border-radius:4px; padding:4px 8px; cursor:pointer;">ختم</button>';
                        list.appendChild(li);
                        input.value = '';
                    }
                </script>
            """
            "Scientific Calculator" -> """
                <div class="hero">
                    <h1>⚡ Quantum Math Station</h1>
                    <p class="subtitle">High-speed calculations powered by Python backend</p>
                </div>
                <div class="card" style="max-width: 380px; margin: 0 auto; text-align: center;">
                    <input id="calcScreen" readonly value="0" style="width:100%; box-sizing:border-box; padding: 16px; background:#050b14; color:#00f0ff; border: 1px solid #00f0ff; border-radius: 8px; font-size: 24px; text-align: right; margin-bottom: 16px; font-family: monospace;">
                    <div style="display:grid; grid-template-columns: repeat(4, 1fr); gap: 10px;">
                        <button onclick="calcClear()" style="padding:15px; border-radius:8px; border:none; background:#ef4444; color:#fff; font-weight:bold; cursor:pointer;">C</button>
                        <button onclick="calcPress('/')" style="padding:15px; border-radius:8px; border:none; background:#1e293b; color:#00f0ff; font-weight:bold; cursor:pointer;">÷</button>
                        <button onclick="calcPress('*')" style="padding:15px; border-radius:8px; border:none; background:#1e293b; color:#00f0ff; font-weight:bold; cursor:pointer;">×</button>
                        <button onclick="calcPress('-')" style="padding:15px; border-radius:8px; border:none; background:#1e293b; color:#00f0ff; font-weight:bold; cursor:pointer;">-</button>
                        <button onclick="calcPress('7')" style="padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">7</button>
                        <button onclick="calcPress('8')" style="padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">8</button>
                        <button onclick="calcPress('9')" style="padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">9</button>
                        <button onclick="calcPress('+')" style="padding:15px; border-radius:8px; border:none; background:#1e293b; color:#00f0ff; font-weight:bold; cursor:pointer;">+</button>
                        <button onclick="calcPress('4')" style="padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">4</button>
                        <button onclick="calcPress('5')" style="padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">5</button>
                        <button onclick="calcPress('6')" style="padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">6</button>
                        <button onclick="calcEqual()" style="grid-row: span 2; padding:15px; border-radius:8px; border:none; background:#00f0ff; color:#050b14; font-weight:bold; font-size:20px; cursor:pointer;">=</button>
                        <button onclick="calcPress('1')" style="padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">1</button>
                        <button onclick="calcPress('2')" style="padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">2</button>
                        <button onclick="calcPress('3')" style="padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">3</button>
                        <button onclick="calcPress('0')" style="grid-column: span 2; padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">0</button>
                        <button onclick="calcPress('.')" style="padding:15px; border-radius:8px; border:none; background:#0f172a; color:#fff; font-weight:bold; cursor:pointer;">.</button>
                    </div>
                </div>
                <script>
                    let s = document.getElementById('calcScreen');
                    function calcPress(val) { if(s.value === '0') s.value = ''; s.value += val; }
                    function calcClear() { s.value = '0'; }
                    function calcEqual() { try { s.value = eval(s.value); } catch(e) { s.value = 'Error'; } }
                </script>
            """
            "Fitness & Workout Tracker" -> """
                <div class="hero">
                    <div class="avatar-ring">🏋️</div>
                    <h1>Titan Iron Fitness Core</h1>
                    <p class="subtitle">AI-Driven Workout Splits, Calorie Telemetry & Rep Counter</p>
                    <div class="tags">
                        <span class="tag">Strength & Cardio</span>
                        <span class="tag">Live Timer Active</span>
                        <span class="tag">Termux Engine</span>
                    </div>
                </div>
                <div class="grid">
                    <div class="card">
                        <h3>🔥 Push Day Protocol</h3>
                        <p>Chest Press: 4 sets x 10 reps<br>Shoulder Overhead Press: 3 sets x 12 reps<br>Incline DB Flies: 3 sets x 15 reps</p>
                    </div>
                    <div class="card">
                        <h3>⚡ Pull & Back Engine</h3>
                        <p>Deadlifts: 4 sets x 8 reps<br>Lat Pulldowns: 4 sets x 12 reps<br>Barbell Rows: 3 sets x 10 reps</p>
                    </div>
                    <div class="card">
                        <h3>⏱️ Rest Timer</h3>
                        <div id="fitTimer" style="font-size: 32px; font-weight:bold; color:#00f0ff; margin:10px 0;">01:30</div>
                        <button onclick="resetTimer()" style="background:#00f0ff; color:#050b14; border:none; padding:8px 16px; border-radius:6px; font-weight:bold; cursor:pointer;">ری سیٹ ٹائمر</button>
                    </div>
                </div>
                <script>
                    let t = 90;
                    function resetTimer() { t = 90; }
                    setInterval(() => {
                        if(t > 0) t--;
                        let m = Math.floor(t / 60);
                        let sec = t % 60;
                        document.getElementById('fitTimer').innerText = (m < 10 ? '0' : '') + m + ':' + (sec < 10 ? '0' : '') + sec;
                    }, 1000);
                </script>
            """
            "Crypto & Asset Hub" -> """
                <div class="hero">
                    <div class="avatar-ring">💎</div>
                    <h1>Apex Quantum Crypto Terminal</h1>
                    <p class="subtitle">Real-Time Cryptocurrency Exchange & Portfolio Metrics</p>
                    <div class="tags">
                        <span class="tag">Web3 Finance</span>
                        <span class="tag">Live Rates</span>
                        <span class="tag">Python Server</span>
                    </div>
                </div>
                <div class="grid">
                    <div class="card">
                        <h3>🟠 Bitcoin (BTC)</h3>
                        <div style="font-size:24px; font-weight:bold; color:#00e676;">$67,420.50 <span style="font-size:14px;">(+3.4%)</span></div>
                        <p style="margin-top:6px; font-size:12px;">Market Cap: $1.32 Trillion</p>
                    </div>
                    <div class="card">
                        <h3>🔷 Ethereum (ETH)</h3>
                        <div style="font-size:24px; font-weight:bold; color:#00e676;">$3,540.20 <span style="font-size:14px;">(+2.8%)</span></div>
                        <p style="margin-top:6px; font-size:12px;">Market Cap: $425 Billion</p>
                    </div>
                    <div class="card">
                        <h3>⚡ Solana (SOL)</h3>
                        <div style="font-size:24px; font-weight:bold; color:#00f0ff;">$154.80 <span style="font-size:14px;">(+5.1%)</span></div>
                        <p style="margin-top:6px; font-size:12px;">High Throughput Layer-1</p>
                    </div>
                </div>
            """
            "Gourmet Bistro & Menu" -> """
                <div class="hero">
                    <div class="avatar-ring">🍽️</div>
                    <h1>Royal Oasis Cuisine Hub</h1>
                    <p class="subtitle">Artisan Dining, Signature Grills & Online Reservation</p>
                    <div class="tags">
                        <span class="tag">Mughlai & Continental</span>
                        <span class="tag">Chef Specials</span>
                        <span class="tag">Online Orders</span>
                    </div>
                </div>
                <div class="grid">
                    <div class="card">
                        <h3>🥩 Royal Mutton Kebab</h3>
                        <p>Charcoal-smoked spiced tender cutlets infused with saffron aroma.</p>
                        <div style="color:#00f0ff; font-weight:bold; margin-top:8px;">₨ 1,450</div>
                    </div>
                    <div class="card">
                        <h3>🍗 Smoked Butter Chicken</h3>
                        <p>Velvety tomato gravy simmered with fresh cream and fenugreek herbs.</p>
                        <div style="color:#00f0ff; font-weight:bold; margin-top:8px;">₨ 1,280</div>
                    </div>
                    <div class="card">
                        <h3>🍨 Shahi Tukray & Pistachio Gelato</h3>
                        <p>Crispy golden brioche steeped in cardamom condensed milk with churned gelato.</p>
                        <div style="color:#00f0ff; font-weight:bold; margin-top:8px;">₨ 650</div>
                    </div>
                </div>
            """
            else -> """
                <div class="hero">
                    <div class="avatar-ring">⚡</div>
                    <h1>$appName</h1>
                    <p class="subtitle">Generated dynamically by JARVIS AI Voice Command</p>
                    <div class="tags">
                        <span class="tag">Termux Python Engine</span>
                        <span class="tag">Background Active</span>
                        <span class="tag">Port 5000</span>
                    </div>
                </div>
                <div class="grid">
                    <div class="card">
                        <h3>🟢 Server Status</h3>
                        <p>Running in background on Python. You can manage or stop this anytime by saying <em>"Stop Python"</em> or <em>"بیک گراؤنڈ سے پائتھن بند کرو"</em>.</p>
                    </div>
                    <div class="card">
                        <h3>⚡ Prompt Context</h3>
                        <p>${prompt.replace("<", "&lt;").replace(">", "&gt;")}</p>
                    </div>
                </div>
            """
        }

        return """<!DOCTYPE html>
<html lang="ur" dir="auto">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$appName - JARVIS Termux Server</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background-color: #050b14;
            color: #e2e8f0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            min-height: 100vh;
            padding: 24px;
        }
        .header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px solid #1e293b;
            padding-bottom: 16px;
            margin-bottom: 30px;
        }
        .badge {
            background: rgba(0, 240, 255, 0.15);
            color: #00f0ff;
            border: 1px solid #00f0ff;
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 12px;
            font-family: monospace;
            font-weight: bold;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }
        .hero {
            text-align: center;
            margin-bottom: 36px;
        }
        .avatar-ring {
            width: 72px;
            height: 72px;
            background: linear-gradient(135deg, #00f0ff, #8b5cf6);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 32px;
            margin: 0 auto 16px auto;
            box-shadow: 0 0 25px rgba(0, 240, 255, 0.4);
        }
        h1 {
            font-size: 32px;
            color: #f8fafc;
            margin-bottom: 8px;
            letter-spacing: -0.5px;
        }
        .subtitle {
            font-size: 16px;
            color: #94a3b8;
            max-width: 600px;
            margin: 0 auto 16px auto;
            line-height: 1.5;
        }
        .tags {
            display: flex;
            gap: 8px;
            justify-content: center;
            flex-wrap: wrap;
        }
        .tag {
            background: #0f172a;
            border: 1px solid #334155;
            color: #cbd5e1;
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 12px;
        }
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 20px;
            max-width: 900px;
            margin: 0 auto 40px auto;
        }
        .card {
            background: #0b1329;
            border: 1px solid #1e293b;
            border-radius: 14px;
            padding: 20px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.3);
            transition: transform 0.2s, border-color 0.2s;
        }
        .card:hover {
            border-color: #00f0ff;
            transform: translateY(-2px);
        }
        .card h3 {
            color: #00f0ff;
            margin-bottom: 8px;
            font-size: 18px;
        }
        .card p {
            color: #94a3b8;
            font-size: 14px;
            line-height: 1.6;
        }
        .footer {
            text-align: center;
            color: #64748b;
            font-size: 13px;
            margin-top: 40px;
            padding-top: 20px;
            border-top: 1px solid #1e293b;
        }
    </style>
</head>
<body>
    <div class="header">
        <div style="font-weight:900; font-family:monospace; color:#00f0ff; font-size:18px;">JARVIS // TERMUX PYTHON</div>
        <div class="badge"><span style="width:8px; height:8px; border-radius:50%; background:#22c55e; display:inline-block;"></span> 5000 ACTIVE</div>
    </div>

    $specificContent

    <div class="footer">
        Powered by JARVIS AI • Running in Background on Android / Termux • Port 5000
    </div>
</body>
</html>"""
    }
}
