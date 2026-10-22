package com.example.data.supabase

import android.content.Context
import com.example.BuildConfig
import com.example.data.model.CodeTemplate
import com.example.data.model.CodeTemplateUpsert
import com.example.data.model.StarterCodeTemplates
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

data class SyncResult<T>(
    val data: T,
    val isCloudSynced: Boolean,
    val message: String? = null
)

class SupabaseRepository(context: Context) {

    private val prefs = context.getSharedPreferences("codepulse_supabase_cache", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val rawSupabaseUrl: String = BuildConfig.SUPABASE_URL.trim()
    val rawSupabaseKey: String = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isSupabaseConfigured: Boolean
        get() = rawSupabaseUrl.startsWith("https://") &&
            !rawSupabaseUrl.contains("MY_SUPABASE_URL") &&
            !rawSupabaseUrl.contains("your-project") &&
            rawSupabaseKey.isNotEmpty() &&
            rawSupabaseKey != "MY_SUPABASE_ANON_KEY"

    private val supabaseClient: SupabaseClient? by lazy {
        if (!isSupabaseConfigured) {
            null
        } else {
            try {
                createSupabaseClient(
                    supabaseUrl = rawSupabaseUrl,
                    supabaseKey = rawSupabaseKey
                ) {
                    install(Postgrest)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun fetchTemplates(): SyncResult<List<CodeTemplate>> = withContext(Dispatchers.IO) {
        val client = supabaseClient
        if (client != null) {
            try {
                val remoteList = client.from(TABLE_NAME)
                    .select {
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<CodeTemplate>()

                if (remoteList.isNotEmpty()) {
                    saveLocalCache(remoteList)
                    return@withContext SyncResult(
                        data = remoteList,
                        isCloudSynced = true,
                        message = "已从 Supabase ($TABLE_NAME) 同步 ${remoteList.size} 个代码模板"
                    )
                } else {
                    // Remote table exists and is empty; seed starter templates into Supabase
                    val seeded = seedStarterTemplatesToCloud(client)
                    return@withContext SyncResult(
                        data = seeded,
                        isCloudSynced = true,
                        message = "Supabase 表为空，已自动初始化 ${seeded.size} 个默认代码模板"
                    )
                }
            } catch (e: Exception) {
                val local = loadLocalCache()
                return@withContext SyncResult(
                    data = local,
                    isCloudSynced = false,
                    message = "Supabase 连接异常 (${e.localizedMessage?.take(60) ?: "请检查 code_templates 表是否已创建"})，已切换至本地缓存"
                )
            }
        } else {
            val local = loadLocalCache()
            return@withContext SyncResult(
                data = local,
                isCloudSynced = false,
                message = "未检测到 SUPABASE_URL 密钥，当前使用本地持久化模式（可在 AI Studio Secrets 面板配置）"
            )
        }
    }

    suspend fun createTemplate(
        title: String,
        language: String,
        code: String,
        description: String,
        difficulty: String,
        tags: String
    ): SyncResult<List<CodeTemplate>> = withContext(Dispatchers.IO) {
        val newId = UUID.randomUUID().toString()
        val upsertPayload = CodeTemplateUpsert(
            id = newId,
            title = title.trim(),
            language = language.trim(),
            code = code.trimEnd(),
            description = description.trim(),
            difficulty = difficulty.trim(),
            tags = tags.trim()
        )
        val newTemplate = CodeTemplate(
            id = newId,
            title = upsertPayload.title,
            language = upsertPayload.language,
            code = upsertPayload.code,
            description = upsertPayload.description,
            difficulty = upsertPayload.difficulty,
            tags = upsertPayload.tags
        )

        val currentLocal = loadLocalCache().toMutableList()
        currentLocal.add(0, newTemplate)
        saveLocalCache(currentLocal)

        val client = supabaseClient
        if (client != null) {
            try {
                client.from(TABLE_NAME).insert(upsertPayload)
                val updatedRemote = client.from(TABLE_NAME)
                    .select { order("created_at", Order.DESCENDING) }
                    .decodeList<CodeTemplate>()
                if (updatedRemote.isNotEmpty()) {
                    saveLocalCache(updatedRemote)
                    return@withContext SyncResult(
                        data = updatedRemote,
                        isCloudSynced = true,
                        message = "已成功保存至 Supabase code_templates 表"
                    )
                }
                return@withContext SyncResult(
                    data = currentLocal,
                    isCloudSynced = true,
                    message = "已成功写入 Supabase code_templates 表"
                )
            } catch (e: Exception) {
                return@withContext SyncResult(
                    data = currentLocal,
                    isCloudSynced = false,
                    message = "Supabase 写入失败 (${e.localizedMessage?.take(45)})，已保存在本地缓存"
                )
            }
        }

        SyncResult(
            data = currentLocal,
            isCloudSynced = false,
            message = "模板《${newTemplate.title}》已保存（配置 Supabase 密钥后自动同步云端）"
        )
    }

    suspend fun updateTemplate(template: CodeTemplate): SyncResult<List<CodeTemplate>> = withContext(Dispatchers.IO) {
        val upsertPayload = CodeTemplateUpsert(
            id = template.id,
            title = template.title.trim(),
            language = template.language.trim(),
            code = template.code.trimEnd(),
            description = template.description.trim(),
            difficulty = template.difficulty.trim(),
            tags = template.tags.trim()
        )

        val currentLocal = loadLocalCache().map {
            if (it.id == template.id) template.copy(
                title = upsertPayload.title,
                language = upsertPayload.language,
                code = upsertPayload.code,
                description = upsertPayload.description,
                difficulty = upsertPayload.difficulty,
                tags = upsertPayload.tags
            ) else it
        }
        saveLocalCache(currentLocal)

        val client = supabaseClient
        if (client != null) {
            try {
                client.from(TABLE_NAME).update(upsertPayload) {
                    filter {
                        eq("id", template.id)
                    }
                }
                return@withContext SyncResult(
                    data = currentLocal,
                    isCloudSynced = true,
                    message = "已同步更新 Supabase 中的《${template.title}》"
                )
            } catch (e: Exception) {
                return@withContext SyncResult(
                    data = currentLocal,
                    isCloudSynced = false,
                    message = "云端更新失败 (${e.localizedMessage?.take(45)})，已更新本地副本"
                )
            }
        }

        SyncResult(
            data = currentLocal,
            isCloudSynced = false,
            message = "已更新模板《${template.title}》"
        )
    }

    suspend fun deleteTemplate(id: String): SyncResult<List<CodeTemplate>> = withContext(Dispatchers.IO) {
        val currentLocal = loadLocalCache().filterNot { it.id == id }
        saveLocalCache(currentLocal)

        val client = supabaseClient
        if (client != null) {
            try {
                client.from(TABLE_NAME).delete {
                    filter {
                        eq("id", id)
                    }
                }
                return@withContext SyncResult(
                    data = currentLocal,
                    isCloudSynced = true,
                    message = "已从 Supabase code_templates 表删除该模板"
                )
            } catch (e: Exception) {
                return@withContext SyncResult(
                    data = currentLocal,
                    isCloudSynced = false,
                    message = "云端删除异常 (${e.localizedMessage?.take(45)})，已从本地移除"
                )
            }
        }

        SyncResult(
            data = currentLocal,
            isCloudSynced = false,
            message = "代码模板已删除"
        )
    }

    suspend fun resetAndPushStarterTemplates(): SyncResult<List<CodeTemplate>> = withContext(Dispatchers.IO) {
        val defaults = StarterCodeTemplates.defaults
        saveLocalCache(defaults)
        val client = supabaseClient
        if (client != null) {
            try {
                val seeded = seedStarterTemplatesToCloud(client)
                return@withContext SyncResult(
                    data = seeded,
                    isCloudSynced = true,
                    message = "已向 Supabase code_templates 推送 ${seeded.size} 个精选模板"
                )
            } catch (e: Exception) {
                return@withContext SyncResult(
                    data = defaults,
                    isCloudSynced = false,
                    message = "已恢复默认模板至本地 (${e.localizedMessage?.take(40)})"
                )
            }
        }
        SyncResult(
            data = defaults,
            isCloudSynced = false,
            message = "已恢复 6 个精选代码模板"
        )
    }

    private suspend fun seedStarterTemplatesToCloud(client: SupabaseClient): List<CodeTemplate> {
        val payloads = StarterCodeTemplates.defaults.map {
            CodeTemplateUpsert(
                id = it.id,
                title = it.title,
                language = it.language,
                code = it.code,
                description = it.description,
                difficulty = it.difficulty,
                tags = it.tags
            )
        }
        client.from(TABLE_NAME).upsert(payloads)
        saveLocalCache(StarterCodeTemplates.defaults)
        return StarterCodeTemplates.defaults
    }

    private fun loadLocalCache(): List<CodeTemplate> {
        val raw = prefs.getString(KEY_TEMPLATES_JSON, null)
        if (raw.isNullOrBlank()) {
            saveLocalCache(StarterCodeTemplates.defaults)
            return StarterCodeTemplates.defaults
        }
        return try {
            val parsed = json.decodeFromString<List<CodeTemplate>>(raw)
            parsed.ifEmpty { StarterCodeTemplates.defaults }
        } catch (e: Exception) {
            StarterCodeTemplates.defaults
        }
    }

    private fun saveLocalCache(list: List<CodeTemplate>) {
        prefs.edit().putString(KEY_TEMPLATES_JSON, json.encodeToString(list)).apply()
    }

    companion object {
        const val TABLE_NAME = "code_templates"
        private const val KEY_TEMPLATES_JSON = "cached_code_templates_v1"

        val SUPABASE_DDL_SQL = """
-- 在 Supabase SQL Editor 中运行以下脚本以创建 code_templates 表：
CREATE TABLE IF NOT EXISTS public.code_templates (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    language TEXT NOT NULL DEFAULT 'Kotlin',
    code TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    difficulty TEXT NOT NULL DEFAULT '中等',
    tags TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 启用 Row Level Security 并允许匿名客户端读写（开发学习自用）：
ALTER TABLE public.code_templates ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public CRUD on code_templates"
ON public.code_templates
FOR ALL
USING (true)
WITH CHECK (true);
        """.trimIndent()
    }
}
