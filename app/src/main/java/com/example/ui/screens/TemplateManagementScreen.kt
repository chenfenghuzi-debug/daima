package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CodeTemplate
import com.example.data.supabase.SupabaseRepository
import com.example.ui.CodePulseUiState
import com.example.ui.components.SyntaxHighlighter
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CodeEditorSurface
import com.example.ui.theme.CodeGutterSurface
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElectricIndigoContainer
import com.example.ui.theme.ElectricIndigoLight
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TemplateManagementScreen(
    uiState: CodePulseUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectLanguageFilter: (String) -> Unit,
    onRefreshSupabase: () -> Unit,
    onCreateTemplate: (String, String, String, String, String, String) -> Unit,
    onUpdateTemplate: (CodeTemplate) -> Unit,
    onDeleteTemplate: (CodeTemplate) -> Unit,
    onPracticeTemplate: (CodeTemplate) -> Unit,
    onResetStarterTemplates: () -> Unit,
    onToggleSqlGuide: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditorDialog by rememberSaveable { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<CodeTemplate?>(null) }
    var deletingTemplate by remember { mutableStateOf<CodeTemplate?>(null) }

    val availableLanguages = remember(uiState.templates) {
        listOf("全部") + (uiState.templates.map { it.language } + listOf("Kotlin", "Python", "TypeScript", "Rust", "SQL")).distinct()
    }

    val filteredTemplates = remember(uiState.templates, uiState.selectedLanguageFilter, uiState.searchQuery) {
        uiState.templates.filter { tpl ->
            val matchesLang = uiState.selectedLanguageFilter == "全部" ||
                tpl.language.equals(uiState.selectedLanguageFilter, ignoreCase = true)
            val q = uiState.searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                tpl.title.contains(q, ignoreCase = true) ||
                tpl.code.contains(q, ignoreCase = true) ||
                tpl.tags.contains(q, ignoreCase = true) ||
                tpl.description.contains(q, ignoreCase = true)
            matchesLang && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingTemplate = null
                    showEditorDialog = true
                },
                icon = {
                    Icon(Icons.Default.Add, contentDescription = "新增代码模板")
                },
                text = {
                    Text("新增代码模板", style = MaterialTheme.typography.labelLarge)
                },
                containerColor = ElectricIndigo,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_template_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("template_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Supabase Table Status & Actions Banner
                item {
                    SupabaseStatusHeaderCard(
                        templateCount = uiState.templates.size,
                        isCloudSynced = uiState.isCloudSynced,
                        isLoading = uiState.isLoadingTemplates,
                        onRefresh = onRefreshSupabase,
                        onShowSqlGuide = { onToggleSqlGuide(true) },
                        onResetStarters = onResetStarterTemplates
                    )
                }

                // 2. Search Input & Language Filters
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("template_search_input"),
                            placeholder = {
                                Text("搜索模板标题、标签或代码关键字…", color = TextMuted)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清除搜索", tint = TextSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricIndigoLight,
                                unfocusedBorderColor = SubtleBorder,
                                focusedContainerColor = CarbonSurface,
                                unfocusedContainerColor = CarbonSurface
                            )
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableLanguages) { lang ->
                                val selected = uiState.selectedLanguageFilter == lang
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectLanguageFilter(lang) },
                                    label = { Text(lang) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricIndigoContainer,
                                        selectedLabelColor = ElectricIndigoLight,
                                        containerColor = CarbonSurface,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = SubtleBorder,
                                        selectedBorderColor = ElectricIndigoLight
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Loading or Empty or List of Templates
                if (uiState.isLoadingTemplates) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ElectricIndigoLight)
                        }
                    }
                } else if (filteredTemplates.isEmpty()) {
                    item {
                        EmptyTemplateListCard(
                            onAddClick = {
                                editingTemplate = null
                                showEditorDialog = true
                            },
                            onResetStarters = onResetStarterTemplates
                        )
                    }
                } else {
                    items(filteredTemplates, key = { it.id }) { template ->
                        CodeTemplateItemCard(
                            template = template,
                            onPractice = { onPracticeTemplate(template) },
                            onEdit = {
                                editingTemplate = template
                                showEditorDialog = true
                            },
                            onDelete = {
                                deletingTemplate = template
                            }
                        )
                    }
                }
            }
        }
    }

    // Create / Edit Code Template Dialog
    if (showEditorDialog) {
        TemplateEditorDialog(
            initialTemplate = editingTemplate,
            onDismiss = { showEditorDialog = false },
            onSave = { title, language, code, description, difficulty, tags ->
                val existing = editingTemplate
                if (existing == null) {
                    onCreateTemplate(title, language, code, description, difficulty, tags)
                } else {
                    onUpdateTemplate(
                        existing.copy(
                            title = title,
                            language = language,
                            code = code,
                            description = description,
                            difficulty = difficulty,
                            tags = tags
                        )
                    )
                }
                showEditorDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    deletingTemplate?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingTemplate = null },
            containerColor = CarbonSurface,
            title = {
                Text("确认删除代码模板？", color = TextPrimary)
            },
            text = {
                Text(
                    "即将从 Supabase code_templates 表中删除《${target.title}》，此操作无法撤销。",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTemplate(target)
                        deletingTemplate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("确认删除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTemplate = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SupabaseStatusHeaderCard(
    templateCount: Int,
    isCloudSynced: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onShowSqlGuide: () -> Unit,
    onResetStarters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCloudSynced) EmeraldContainer else ElectricIndigoContainer
                    ) {
                        Icon(
                            imageVector = if (isCloudSynced) Icons.Default.CloudDone else Icons.Default.Storage,
                            contentDescription = null,
                            tint = if (isCloudSynced) EmeraldSuccess else ElectricIndigoLight,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Supabase · code_templates 表",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isCloudSynced) {
                                "云端实时连接正常 · 共 $templateCount 个代码模板"
                            } else {
                                "本地缓存模式已就绪 · 共 $templateCount 个代码模板"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCloudSynced) EmeraldSuccess else TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.testTag("sync_supabase_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "同步 Supabase",
                        tint = CyanSpark
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShowSqlGuide,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SubtleBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("sql_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = CyanSpark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("查看 Supabase 建表 SQL", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }

                OutlinedButton(
                    onClick = onResetStarters,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SubtleBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("reset_starter_templates_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = ElectricIndigoLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("恢复精选模板", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodeTemplateItemCard(
    template: CodeTemplate,
    onPractice: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val langColor = SyntaxHighlighter.languageAccentColor(template.language)
    val diffColor = SyntaxHighlighter.difficultyColor(template.difficulty)
    val previewLines = remember(template.code) { template.code.lines().take(8) }
    val hasMoreLines = remember(template.code) { template.code.lines().size > 8 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("template_card_${template.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = langColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, langColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = template.language,
                                style = MaterialTheme.typography.labelMedium,
                                color = langColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            color = diffColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = template.difficulty,
                                style = MaterialTheme.typography.labelSmall,
                                color = diffColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (template.tags.isNotBlank()) {
                        Text(
                            text = template.tags,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                if (template.description.isNotBlank()) {
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Syntax Highlighted Code Snippet Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeEditorSurface)
                    .padding(vertical = 10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(start = 12.dp, end = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        previewLines.forEachIndexed { idx, _ ->
                            Text(
                                text = "${idx + 1}",
                                style = CodeTextStyle.copy(fontSize = 12.sp),
                                color = TextMuted,
                                modifier = Modifier.height(21.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height((previewLines.size * 21).dp)
                            .background(SubtleBorder)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp)
                    ) {
                        previewLines.forEach { line ->
                            Box(
                                modifier = Modifier.height(21.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = SyntaxHighlighter.highlight(line),
                                    style = CodeTextStyle.copy(fontSize = 13.sp)
                                )
                            }
                        }
                        if (hasMoreLines) {
                            Text(
                                text = "// ... 共 ${template.code.lines().size} 行代码",
                                style = CodeTextStyle.copy(fontSize = 12.sp),
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = SubtleBorder, thickness = 1.dp)

            // CRUD & AI Quiz Action Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeGutterSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPractice,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricIndigo,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("practice_template_${template.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("以此模板 AI 出题", style = MaterialTheme.typography.labelLarge)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_template_${template.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑代码模板",
                            tint = CyanSpark
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_template_${template.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除代码模板",
                            tint = CoralError
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTemplateListCard(
    onAddClick: () -> Unit,
    onResetStarters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = ElectricIndigoLight,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "未找到匹配的代码模板",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "你可以点击下方按钮新建自定义代码片段，或恢复 6 个精选算法与工程模板。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("新增模板")
                }
                OutlinedButton(onClick = onResetStarters) {
                    Text("恢复默认模板")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateEditorDialog(
    initialTemplate: CodeTemplate?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        language: String,
        code: String,
        description: String,
        difficulty: String,
        tags: String
    ) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initialTemplate?.title ?: "") }
    var language by rememberSaveable { mutableStateOf(initialTemplate?.language ?: "Kotlin") }
    var difficulty by rememberSaveable { mutableStateOf(initialTemplate?.difficulty ?: "中等") }
    var tags by rememberSaveable { mutableStateOf(initialTemplate?.tags ?: "") }
    var description by rememberSaveable { mutableStateOf(initialTemplate?.description ?: "") }
    var code by rememberSaveable { mutableStateOf(initialTemplate?.code ?: "") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    val languageOptions = listOf("Kotlin", "Python", "TypeScript", "Rust", "SQL", "Java")
    val difficultyOptions = listOf("简单", "中等", "困难")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarbonSurface,
        title = {
            Text(
                text = if (initialTemplate == null) "新增代码模板 (code_templates)" else "编辑代码模板",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("模板标题 *") },
                    placeholder = { Text("例如：Kotlin 协程异常监督作用域") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricIndigoLight,
                        unfocusedBorderColor = SubtleBorder
                    )
                )

                Text("编程语言", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languageOptions.forEach { lang ->
                        val selected = language.equals(lang, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { language = lang },
                            label = { Text(lang) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricIndigoContainer,
                                selectedLabelColor = ElectricIndigoLight
                            )
                        )
                    }
                }

                Text("难度等级", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    difficultyOptions.forEach { diff ->
                        val selected = difficulty == diff
                        FilterChip(
                            selected = selected,
                            onClick = { difficulty = diff },
                            label = { Text(diff) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricIndigoContainer,
                                selectedLabelColor = ElectricIndigoLight
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("知识标签（逗号分隔）") },
                    placeholder = { Text("例如：并发, 协程, 异常处理") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_tags_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricIndigoLight,
                        unfocusedBorderColor = SubtleBorder
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("核心逻辑说明 / 考点备注") },
                    placeholder = { Text("简述这段代码解决的核心问题…") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_description_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricIndigoLight,
                        unfocusedBorderColor = SubtleBorder
                    )
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        validationError = null
                    },
                    label = { Text("完整代码片段 *") },
                    placeholder = { Text("粘贴或输入完整代码，OpenCode AI 将自动识别关键元素并挖空…") },
                    textStyle = CodeTextStyle.copy(fontSize = 13.sp, color = TextPrimary),
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_code_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricIndigoLight,
                        unfocusedBorderColor = SubtleBorder,
                        focusedContainerColor = CodeEditorSurface,
                        unfocusedContainerColor = CodeEditorSurface
                    )
                )

                validationError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoralError
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        validationError = "请输入模板标题"
                        return@Button
                    }
                    if (code.trim().length < 10) {
                        validationError = "请输入至少 10 个字符的有效代码片段"
                        return@Button
                    }
                    onSave(title, language, code, description, difficulty, tags)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier.testTag("save_template_button")
            ) {
                Text("保存至 Supabase", color = Color(0xFF042F1E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@Composable
fun SupabaseSqlGuideDialog(
    isSupabaseConfigured: Boolean,
    isOpenCodeConfigured: Boolean,
    onDismiss: () -> Unit,
    onCopied: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarbonSurface,
        title = {
            Text(
                text = "Supabase & OpenCode 配置指南",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CodeEditorSurface,
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "1. AI Studio Secrets 密钥状态",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanSpark
                        )
                        Text(
                            text = "• SUPABASE_URL & SUPABASE_ANON_KEY: ${if (isSupabaseConfigured) "已配置" else "未配置（请在 AI Studio Secrets 面板填写）"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSupabaseConfigured) EmeraldSuccess else AmberWarning
                        )
                        Text(
                            text = "• OPENCODE_API_KEY: ${if (isOpenCodeConfigured) "已配置 (Base URL: https://opencode.ai/zen/v1/)" else "未配置（请在 AI Studio Secrets 面板填写）"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOpenCodeConfigured) EmeraldSuccess else AmberWarning
                        )
                    }
                }

                Text(
                    text = "2. 在 Supabase SQL Editor 中运行以下建表语句：",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CodeEditorSurface,
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Text(
                        text = SupabaseRepository.SUPABASE_DDL_SQL,
                        style = CodeTextStyle.copy(fontSize = 12.sp),
                        color = TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(SupabaseRepository.SUPABASE_DDL_SQL))
                    onCopied()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                modifier = Modifier.testTag("copy_sql_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("复制 SQL 脚本", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = TextSecondary)
            }
        }
    )
}
