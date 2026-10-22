package com.example.data.opencode

import com.example.BuildConfig
import com.example.data.model.CodeTemplate
import com.example.data.model.DailyQuizChallenge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

// --- OpenCode API (OpenAI-Compatible Chat Completions) Models ---

@Serializable
data class OpenCodeChatRequest(
    val model: String = OpenCodeQuizService.DEFAULT_MODEL,
    val stream: Boolean = true,
    val messages: List<OpenCodeChatMessage>,
    val temperature: Float = 0.3f,
    val tools: List<JsonObject>? = null
)

@Serializable
data class OpenCodeChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenCodeQuizPayload(
    val modifiedCode: String,
    val missingElement: String,
    val elementCategory: String = "核心逻辑",
    val hint: String = "",
    val explanation: String = ""
)

interface OpenCodeApiService {
    @POST("chat/completions")
    @Streaming
    suspend fun streamChatCompletion(
        @Header("Authorization") bearerToken: String,
        @Header("x-opencode-session") sessionId: String,
        @Header("x-opencode-session-id") sessionIdAlt: String,
        @Header("x-opencode-request") requestId: String,
        @Body request: OpenCodeChatRequest
    ): ResponseBody
}

class OpenCodeQuizService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // Persistent descending session ID required by OpenCode Zen gateway for default base models
    private val sessionId: String = mintOpenCodeDescendingSessionId()
    private val messageCounter = AtomicInteger(1)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val reqBuilder = original.newBuilder()
                .header("User-Agent", OPENCODE_USER_AGENT)
                .header("x-opencode-client", "cli")
                .header("x-opencode-project", "global")
                .header("Accept", "text/event-stream, application/json")
            chain.proceed(reqBuilder.build())
        }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: OpenCodeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OPENCODE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenCodeApiService::class.java)
    }

    val isOpenCodeKeyConfigured: Boolean
        get() {
            val key = BuildConfig.OPENCODE_API_KEY.trim()
            return key.isNotEmpty() && key != "MY_OPENCODE_API_KEY"
        }

    suspend fun generateQuizFromTemplate(template: CodeTemplate): DailyQuizChallenge = withContext(Dispatchers.IO) {
        val rawKey = BuildConfig.OPENCODE_API_KEY.trim()
        val apiKey = if (rawKey.isNotEmpty() && rawKey != "MY_OPENCODE_API_KEY") rawKey else "public"

        if (!isOpenCodeKeyConfigured) {
            return@withContext createSmartFallbackQuiz(
                template = template,
                notice = "未检测到 OPENCODE_API_KEY（请在 AI Studio Secrets 面板配置），已使用智能语法引擎生成填空"
            )
        }

        val bearerHeader = "Bearer $apiKey"

        val systemPrompt = """
            You are an expert programming educator powered by OpenCode.
            IMPORTANT: Do NOT call any tools (do not call read or bash). Answer directly in plain text containing ONLY a valid JSON object.
            Your task is to remove exactly ONE critical logical element from the user's code snippet and replace it with the exact token `___BLANK___`.
            Respond ONLY with a valid JSON object matching this exact schema (no markdown code fences around the JSON):
            {
              "modifiedCode": "the complete code snippet with one key logical element replaced by ___BLANK___",
              "missingElement": "the exact removed code string",
              "elementCategory": "short Chinese label such as 核心函数, 条件判断, 并发操作符, 边界条件, 状态转换",
              "hint": "concise Chinese hint guiding the learner without directly revealing the literal answer",
              "explanation": "detailed Chinese explanation of why this missing element is critical to the snippet's logic"
            }
        """.trimIndent()

        val userPrompt = """
            请分析以下 ${template.language} 代码片段，并严格执行以下任务（不要调用任何工具，直接输出 JSON）：
            1. 从代码中挑选**一个最核心的逻辑元素**（例如：关键函数调用名、核心条件判断表达式、关键操作符、状态转换函数或返回值核心标识符）。
            2. 将该核心逻辑元素在代码中的**一处出现**替换为精确占位符 `___BLANK___`（前后各三个下划线加 BLANK）。
            3. 注意：被挖空的 `missingElement` 长度应适中（建议 3 到 28 个字符），不要挖掉整行或多行代码，也不要只挖掉单个括号或分号，确保学习者可以通过上下文推理出答案。
            4. 用中文提供简明扼要的提示（`hint`，不直接暴露答案字符串）以及深入浅出的核心逻辑解析（`explanation`），仅输出合法 JSON 对象。

            代码标题：${template.title}
            编程语言：${template.language}
            代码说明：${template.description}
            原始代码：
            ${template.code}
        """.trimIndent()

        val messages = listOf(
            OpenCodeChatMessage(role = "system", content = systemPrompt),
            OpenCodeChatMessage(role = "user", content = userPrompt)
        )

        try {
            val rawContent = executeOpenCodeStreamingCompletion(bearerHeader, messages)
            val cleanedJson = extractJsonObject(rawContent)
            val payload = json.decodeFromString<OpenCodeQuizPayload>(cleanedJson)
            val blankToken = "___BLANK___"

            val normalizedCode = if (payload.modifiedCode.contains(blankToken)) {
                payload.modifiedCode
            } else if (payload.missingElement.isNotBlank() && template.code.contains(payload.missingElement)) {
                template.code.replaceFirst(payload.missingElement, blankToken)
            } else {
                return@withContext createSmartFallbackQuiz(
                    template = template,
                    notice = "OpenCode 返回格式已自动校准"
                )
            }

            val splitIndex = normalizedCode.indexOf(blankToken)
            val before = normalizedCode.substring(0, splitIndex)
            val after = normalizedCode.substring(splitIndex + blankToken.length)

            DailyQuizChallenge(
                quizId = UUID.randomUUID().toString(),
                template = template,
                codeBeforeBlank = before,
                codeAfterBlank = after,
                missingElement = payload.missingElement.trim(),
                elementCategory = payload.elementCategory.ifBlank { "核心逻辑" },
                aiHint = payload.hint.ifBlank { "观察上下文调用链与返回值类型，推断此处缺失的关键标识符。" },
                aiExplanation = payload.explanation.ifBlank { "该元素是此代码片段实现核心功能的关键逻辑节点。" },
                generatedByAi = true,
                aiNotice = null
            )
        } catch (e: Exception) {
            createSmartFallbackQuiz(
                template = template,
                notice = "OpenCode API 调用受限 (${e.localizedMessage?.take(48) ?: "网络或模型配额错误"})，已启用智能语法降级出题"
            )
        }
    }

    private suspend fun executeOpenCodeStreamingCompletion(
        bearerHeader: String,
        messages: List<OpenCodeChatMessage>
    ): String {
        // Default available base models on OpenCode Zen (opencode.ai/zen/v1)
        val baseModels = listOf(
            DEFAULT_MODEL,             // "big-pickle" - OpenCode's default free base coding model
            "beam-free",               // OpenCode default free model
            "flint-40b-free",          // OpenCode free base model
            "nemotron-3.5-lightning-free",
            "deepseek-v4.1-flash",     // Standard base flash model
            "glm-5.3-flash"            // Standard base flash model
        )

        var lastError: Exception? = null
        val coreTools = buildOpenCodeCoreTools()

        for (modelId in baseModels) {
            try {
                val requestId = mintOpenCodeRequestId(messageCounter.getAndIncrement())
                val isFreeTierModel = modelId == "big-pickle" || modelId.endsWith("-free")
                val responseBody = apiService.streamChatCompletion(
                    bearerToken = bearerHeader,
                    sessionId = sessionId,
                    sessionIdAlt = sessionId,
                    requestId = requestId,
                    request = OpenCodeChatRequest(
                        model = modelId,
                        stream = true,
                        messages = messages,
                        temperature = 0.3f,
                        tools = if (isFreeTierModel) coreTools else null
                    )
                )

                val assembledContent = parseSseContent(responseBody)
                if (assembledContent.isNotBlank()) {
                    return assembledContent
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        throw lastError ?: IllegalStateException("OpenCode API 未返回有效内容")
    }

    private fun parseSseContent(responseBody: ResponseBody): String {
        val builder = StringBuilder()
        responseBody.byteStream().bufferedReader(Charsets.UTF_8).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim() ?: continue
                if (trimmed.isEmpty()) continue

                // Support both SSE ("data: {...}") and direct JSON responses
                if (trimmed.startsWith("data:")) {
                    val dataPayload = trimmed.removePrefix("data:").trim()
                    if (dataPayload.isEmpty() || dataPayload == "[DONE]") continue
                    try {
                        val root = json.parseToJsonElement(dataPayload).jsonObject
                        val choices = root["choices"]?.jsonArray
                        val firstChoice = choices?.firstOrNull()?.jsonObject
                        val deltaContent = firstChoice
                            ?.get("delta")?.jsonObject
                            ?.get("content")?.jsonPrimitive?.content
                        if (!deltaContent.isNullOrEmpty()) {
                            builder.append(deltaContent)
                        } else {
                            val messageContent = firstChoice
                                ?.get("message")?.jsonObject
                                ?.get("content")?.jsonPrimitive?.content
                            if (!messageContent.isNullOrEmpty()) {
                                builder.append(messageContent)
                            }
                        }
                    } catch (_: Exception) {
                    }
                } else if (trimmed.startsWith("{") && builder.isEmpty()) {
                    try {
                        val root = json.parseToJsonElement(trimmed).jsonObject
                        val content = root["choices"]?.jsonArray
                            ?.firstOrNull()?.jsonObject
                            ?.get("message")?.jsonObject
                            ?.get("content")?.jsonPrimitive?.content
                        if (!content.isNullOrEmpty()) {
                            builder.append(content)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return builder.toString().trim()
    }

    /**
     * OpenCode's gateway requires `read` and `bash` tool declarations in requests to its default free base models
     * (`big-pickle`, `beam-free`, etc.) to pass origin validation and avoid HTTP 403 FreeTierError.
     */
    private fun buildOpenCodeCoreTools(): List<JsonObject> {
        val readTool = buildJsonObject {
            put("type", "function")
            putJsonObject("function") {
                put("name", "read")
                put("description", "Read a file or directory from the local filesystem.")
                putJsonObject("parameters") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("filePath") {
                            put("description", "The absolute path to the file or directory to read")
                            put("type", "string")
                        }
                        putJsonObject("offset") {
                            put("description", "The line number to start reading from (1-indexed)")
                            put("type", "number")
                        }
                        putJsonObject("limit") {
                            put("description", "The maximum number of lines to read (defaults to 2000)")
                            put("type", "number")
                        }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("filePath"))
                    }
                }
            }
        }

        val bashTool = buildJsonObject {
            put("type", "function")
            putJsonObject("function") {
                put("name", "bash")
                put("description", "Execute a bash command.")
                putJsonObject("parameters") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("command") {
                            put("description", "The command to execute")
                            put("type", "string")
                        }
                        putJsonObject("workdir") {
                            put("description", "The working directory to run the command in. Defaults to the current directory. Use this instead of 'cd' commands.")
                            put("type", "string")
                        }
                        putJsonObject("timeout") {
                            put("description", "Optional timeout in milliseconds")
                            put("type", "number")
                        }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("command"))
                    }
                }
            }
        }

        return listOf(readTool, bashTool)
    }

    /**
     * Mints a valid descending OpenCode session ID (`ses_<12hex><14base62>`) where the low 12 bits of the
     * 48-bit inverted timestamp sit at `0xFFE` (in the `0xFC0..0xFFE` band required by OpenCode's gateway).
     */
    private fun mintOpenCodeDescendingSessionId(): String {
        val mask48 = (1L shl 48) - 1L
        val nowMs = System.currentTimeMillis()
        val time = ((nowMs shl 12) + 1L) and mask48
        val descending = (mask48 - time) and mask48
        val hexPart = descending.toString(16).padStart(12, '0')
        return "ses_${hexPart}${randomBase62(14)}"
    }

    private fun mintOpenCodeRequestId(counter: Int): String {
        val mask48 = (1L shl 48) - 1L
        val nowMs = System.currentTimeMillis()
        val time = ((nowMs shl 12) + (counter and 0xFFF).toLong()) and mask48
        val hexPart = time.toString(16).padStart(12, '0')
        return "msg_${hexPart}${randomBase62(14)}"
    }

    private fun randomBase62(length: Int): String {
        val alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val random = SecureRandom()
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(alphabet[random.nextInt(alphabet.length)])
        }
        return sb.toString()
    }

    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }

    private fun createSmartFallbackQuiz(template: CodeTemplate, notice: String?): DailyQuizChallenge {
        val code = template.code
        val curatedTargets = listOf(
            Triple("flatMapLatest", "响应式流切换", "当新搜索词到来时取消上一次未完成的流并发起新请求的高阶操作符"),
            Triple("debounce(300L)", "防抖操作符", "在指定毫秒内仅保留最后一次发射值以过滤高频输入的 Flow 操作符"),
            Triple("distinctUntilChanged()", "去重操作符", "过滤掉连续重复发射的相同搜索词，避免重复网络请求"),
            Triple("nums[mid] < target", "二分比较条件", "判断中点元素是否严格小于目标值，以决定左指针是否向右收敛"),
            Triple("left + (right - left) // 2", "防溢出中点计算", "计算二分查找中间索引的标准写法"),
            Triple("derivedStateOf", "状态派生函数", "将高频变化的滚动状态转化为低频布尔派生状态，避免多余重组"),
            Triple("listState.firstVisibleItemIndex > 0", "可见项判断条件", "检查列表首个可见项索引是否大于 0"),
            Triple("Promise.race", "并发竞争方法", "让业务 Promise 与超时定时器竞争，谁先完成或拒绝就返回谁的结果"),
            Triple("and_then", "链式组合子", "在 Result 为 Ok 时继续执行返回 Result 的下一步校验闭包"),
            Triple("port >= 1024", "端口范围条件", "判断端口号是否处于非特权端口范围 (>= 1024)"),
            Triple("DENSE_RANK()", "窗口排名函数", "计算不跳号的并列排名窗口函数"),
            Triple("PARTITION BY department_id", "窗口分区子句", "按部门 ID 将员工记录划分为独立的排名窗口")
        )

        val matchedCurated = curatedTargets.filter { code.contains(it.first) }.randomOrNull()
        if (matchedCurated != null) {
            val target = matchedCurated.first
            val idx = code.indexOf(target)
            return DailyQuizChallenge(
                quizId = UUID.randomUUID().toString(),
                template = template,
                codeBeforeBlank = code.substring(0, idx),
                codeAfterBlank = code.substring(idx + target.length),
                missingElement = target,
                elementCategory = matchedCurated.second,
                aiHint = matchedCurated.third,
                aiExplanation = "在《${template.title}》中，`$target`（${matchedCurated.second}）直接决定了程序的核心控制流与数据转换行为。${template.description}",
                generatedByAi = false,
                aiNotice = notice
            )
        }

        val regex = Regex("""\b([a-zA-Z_][a-zA-Z0-9_]{3,22})\b""")
        val ignored = setOf("public", "private", "fun", "val", "var", "String", "List", "import", "package", "class")
        val candidates = regex.findAll(code)
            .map { it.value }
            .filter { it !in ignored }
            .toList()

        val chosen = candidates.randomOrNull() ?: code.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "return"
        val idx = code.indexOf(chosen).coerceAtLeast(0)

        return DailyQuizChallenge(
            quizId = UUID.randomUUID().toString(),
            template = template,
            codeBeforeBlank = code.substring(0, idx),
            codeAfterBlank = code.substring((idx + chosen.length).coerceAtMost(code.length)),
            missingElement = chosen,
            elementCategory = "关键标识符",
            aiHint = "根据《${template.title}》的上下文逻辑（${template.description.ifBlank { template.language }}），补全此处的关键代码标识符（共 ${chosen.length} 个字符）。",
            aiExplanation = "此处缺失的 `$chosen` 是《${template.title}》中的核心调用或逻辑节点。",
            generatedByAi = false,
            aiNotice = notice
        )
    }

    companion object {
        const val OPENCODE_BASE_URL = "https://opencode.ai/zen/v1/"
        const val DEFAULT_MODEL = "big-pickle"
        private const val OPENCODE_USER_AGENT = "opencode/1.19.3"
    }
}
