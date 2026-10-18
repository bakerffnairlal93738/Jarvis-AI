package com.example.service

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages the persistent WebSocket connection to Google Gemini Multimodal Live API
 * (BidiGenerateContent) for low-latency, real-time bidirectional audio streaming.
 */
class GeminiLiveService(
    private val toolManager: JarvisToolManager,
    private val onAudioOutputReceived: (ByteArray) -> Unit,
    private val onTranscriptReceived: (String, Boolean) -> Unit, // text, isUser
    private val onStatusChanged: (ConnectionState) -> Unit,
    private val onInterrupted: () -> Unit
) {
    private val tag = "GeminiLiveService"
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectJob: Job? = null
    private var shouldReconnect = true
    private var selectedVoice: String = "Kore"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WebSocket
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        ERROR
    }

    fun setVoice(voice: String) {
        selectedVoice = voice
    }

    fun connect(apiKey: String, coroutineScope: CoroutineScope) {
        if (apiKey.isBlank()) {
            onStatusChanged(ConnectionState.ERROR)
            return
        }

        shouldReconnect = true
        onStatusChanged(ConnectionState.CONNECTING)

        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "Gemini Live WebSocket opened successfully")
                isConnected = true
                onStatusChanged(ConnectionState.CONNECTED)

                // Send the initial setup handshake
                sendSetupHandshake(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text, coroutineScope)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Gemini Live WebSocket closing: $code / $reason")
                isConnected = false
                onStatusChanged(ConnectionState.DISCONNECTED)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Gemini Live WebSocket closed: $code / $reason")
                isConnected = false
                onStatusChanged(ConnectionState.DISCONNECTED)
                if (shouldReconnect) {
                    scheduleReconnect(apiKey, coroutineScope)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "Gemini Live WebSocket failure: ${t.message}", t)
                isConnected = false
                onStatusChanged(ConnectionState.ERROR)
                if (shouldReconnect) {
                    scheduleReconnect(apiKey, coroutineScope)
                }
            }
        })
    }

    private fun scheduleReconnect(apiKey: String, coroutineScope: CoroutineScope) {
        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch(Dispatchers.IO) {
            delay(3000)
            if (shouldReconnect && !isConnected) {
                onStatusChanged(ConnectionState.RECONNECTING)
                connect(apiKey, coroutineScope)
            }
        }
    }

    private fun sendSetupHandshake(ws: WebSocket) {
        try {
            val setupJson = JSONObject().apply {
                val setupObj = JSONObject().apply {
                    put("model", "models/gemini-2.5-flash-native-audio-preview-12-2025")

                    // Generation config with native AUDIO modality and selected voice
                    val genConfig = JSONObject().apply {
                        val modalities = JSONArray().apply { put("AUDIO") }
                        put("responseModalities", modalities)

                        val speechConfig = JSONObject().apply {
                            val voiceConfig = JSONObject().apply {
                                val prebuilt = JSONObject().apply {
                                    put("voiceName", selectedVoice)
                                }
                                put("prebuiltVoiceConfig", prebuilt)
                            }
                            put("voiceConfig", voiceConfig)
                        }
                        put("speechConfig", speechConfig)
                    }
                    put("generationConfig", genConfig)

                    // System Instruction
                    val systemInstruction = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val part = JSONObject().apply {
                                put("text", "You are JARVIS, an advanced, highly intelligent, and witty AI voice assistant. " +
                                        "Speak naturally like a real-time conversational companion. " +
                                        "Respond in the language used by the user (English, Hindi, or Urdu, including natural conversational Hinglish). " +
                                        "Keep conversational spoken answers concise, direct, and engaging. " +
                                        "When an action like opening YouTube/Google, searching, taking notes, creating reminders, checking time/weather, or math is needed, call the appropriate tool. " +
                                        "Never claim you did an action unless the tool execution succeeded.")
                            }
                            put(part)
                        }
                        put("parts", parts)
                    }
                    put("systemInstruction", systemInstruction)

                    // Tools declaration
                    val tools = JSONArray().apply {
                        val toolObj = JSONObject().apply {
                            put("functionDeclarations", createToolDeclarations())
                        }
                        put(toolObj)
                    }
                    put("tools", tools)
                }
                put("setup", setupObj)
            }

            ws.send(setupJson.toString())
            Log.d(tag, "Setup handshake sent")
        } catch (e: Exception) {
            Log.e(tag, "Error sending setup handshake: ${e.message}", e)
        }
    }

    private fun createToolDeclarations(): JSONArray {
        val list = JSONArray()

        // get_current_time
        list.put(JSONObject().apply {
            put("name", "get_current_time")
            put("description", "Get the current accurate time")
        })

        // get_current_date
        list.put(JSONObject().apply {
            put("name", "get_current_date")
            put("description", "Get today's full date and day of the week")
        })

        // open_google
        list.put(JSONObject().apply {
            put("name", "open_google")
            put("description", "Opens Google homepage on the device")
        })

        // open_youtube
        list.put(JSONObject().apply {
            put("name", "open_youtube")
            put("description", "Opens YouTube app or site on the device")
        })

        // search_google
        list.put(JSONObject().apply {
            put("name", "search_google")
            put("description", "Searches Google for the given search query")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The query to search on Google")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // get_weather
        list.put(JSONObject().apply {
            put("name", "get_weather")
            put("description", "Get the current weather conditions for a specified city or location")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("city", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The city name, e.g. London, Mumbai, New York")
                    })
                })
            })
        })

        // get_location
        list.put(JSONObject().apply {
            put("name", "get_location")
            put("description", "Get the current user location and region")
        })

        // create_note
        list.put(JSONObject().apply {
            put("name", "create_note")
            put("description", "Save a note in the local database")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The text content of the note")
                    })
                })
                put("required", JSONArray().apply { put("content") })
            })
        })

        // read_notes
        list.put(JSONObject().apply {
            put("name", "read_notes")
            put("description", "Read back the saved notes from the database")
        })

        // create_reminder
        list.put(JSONObject().apply {
            put("name", "create_reminder")
            put("description", "Create a reminder for a specific number of minutes")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The title or task for the reminder")
                    })
                    put("minutes", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Minutes from now when reminder should trigger")
                    })
                })
                put("required", JSONArray().apply { put("title") })
            })
        })

        // calculate
        list.put(JSONObject().apply {
            put("name", "calculate")
            put("description", "Perform a mathematical calculation safely")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("expression", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The math expression, e.g. '25 * 40' or '15% of 800'")
                    })
                })
                put("required", JSONArray().apply { put("expression") })
            })
        })

        // open_calendar
        list.put(JSONObject().apply {
            put("name", "open_calendar")
            put("description", "Open the device calendar application")
        })

        return list
    }

    /**
     * Streams continuous microphone PCM chunks (16kHz 16-bit LE) to the Gemini Live session.
     */
    fun sendAudioChunk(pcmChunk: ByteArray) {
        if (!isConnected || webSocket == null) return

        try {
            val base64Data = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
            val chunkMessage = JSONObject().apply {
                val realtimeInput = JSONObject().apply {
                    val mediaChunks = JSONArray().apply {
                        val media = JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64Data)
                        }
                        put(media)
                    }
                    put("mediaChunks", mediaChunks)
                }
                put("realtimeInput", realtimeInput)
            }
            webSocket?.send(chunkMessage.toString())
        } catch (e: Exception) {
            Log.e(tag, "Error sending audio chunk: ${e.message}")
        }
    }

    /**
     * Sends user text input directly to the Live session if needed.
     */
    fun sendTextMessage(text: String) {
        if (!isConnected || webSocket == null) return
        try {
            val msg = JSONObject().apply {
                val clientContent = JSONObject().apply {
                    val turns = JSONArray().apply {
                        val turn = JSONObject().apply {
                            put("role", "user")
                            val parts = JSONArray().apply {
                                val p = JSONObject().apply {
                                    put("text", text)
                                }
                                put(p)
                            }
                            put("parts", parts)
                        }
                        put(turn)
                    }
                    put("turns", turns)
                    put("turnComplete", true)
                }
                put("clientContent", clientContent)
            }
            webSocket?.send(msg.toString())
        } catch (e: Exception) {
            Log.e(tag, "Error sending text message: ${e.message}")
        }
    }

    private fun handleServerMessage(text: String, coroutineScope: CoroutineScope) {
        try {
            val root = JSONObject(text)

            // Check for interruption signal
            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")
                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(tag, "Server indicated interruption")
                    onInterrupted()
                    return
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Check text transcript
                            if (part.has("text")) {
                                val transcript = part.getString("text")
                                onTranscriptReceived(transcript, false)
                            }

                            // Check audio data (24kHz PCM)
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64Audio = inlineData.optString("data")
                                if (base64Audio.isNotBlank()) {
                                    val pcmBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                                    onAudioOutputReceived(pcmBytes)
                                }
                            }
                        }
                    }
                }
            }

            // Check for Tool Call
            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        handleFunctionCalls(functionCalls)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling server message: ${e.message}", e)
        }
    }

    private suspend fun handleFunctionCalls(functionCalls: JSONArray) {
        val functionResponses = JSONArray()

        for (i in 0 until functionCalls.length()) {
            val call = functionCalls.getJSONObject(i)
            val callId = call.optString("id", "call_$i")
            val name = call.getString("name")
            val argsObj = call.optJSONObject("args")

            val argsMap = mutableMapOf<String, Any?>()
            if (argsObj != null) {
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    argsMap[k] = argsObj.get(k)
                }
            }

            // Execute safe tool
            val result = toolManager.executeTool(name, argsMap)
            onTranscriptReceived("[ACTION] ${result.displayMessage}", false)

            val respObj = JSONObject().apply {
                val responsePayload = JSONObject().apply {
                    put("output", result.spokenConfirmation)
                    put("status", if (result.isError) "error" else "success")
                }
                put("response", responsePayload)
                put("id", callId)
                put("name", name)
            }
            functionResponses.put(respObj)
        }

        // Send toolResponse back to Gemini Live
        try {
            val toolResponseMsg = JSONObject().apply {
                val toolResponse = JSONObject().apply {
                    put("functionResponses", functionResponses)
                }
                put("toolResponse", toolResponse)
            }
            webSocket?.send(toolResponseMsg.toString())
        } catch (e: Exception) {
            Log.e(tag, "Error sending toolResponse: ${e.message}")
        }
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        isConnected = false
        try {
            webSocket?.close(1000, "User stopped session")
        } catch (e: Exception) {
            Log.e(tag, "Error closing WebSocket: ${e.message}")
        } finally {
            webSocket = null
            onStatusChanged(ConnectionState.DISCONNECTED)
        }
    }

    fun isLive(): Boolean = isConnected
}
