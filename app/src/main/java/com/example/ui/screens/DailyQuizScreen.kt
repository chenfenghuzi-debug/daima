package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyQuizChallenge
import com.example.data.model.QuizSubmissionState
import com.example.ui.CodePulseUiState
import com.example.ui.components.SyntaxHighlighter
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CodeEditorSurface
import com.example.ui.theme.CodeGutterSurface
import com.example.ui.theme.CodeTextStyle
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElectricIndigoContainer
import com.example.ui.theme.ElectricIndigoLight
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyQuizScreen(
    uiState: CodePulseUiState,
    onAnswerChange: (String) -> Unit,
    onAppendSymbol: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuiz: () -> Unit,
    onToggleHint: () -> Unit,
    onRevealAnswer: () -> Unit,
    onSelectLanguageFilter: (String) -> Unit,
    onNavigateToTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val languages = remember(uiState.templates) {
        listOf("全部") + uiState.templates.map { it.language }.distinct()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("daily_quiz_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Stats & AI Generator Header Card
            item {
                QuizHeroBanner(
                    solvedToday = uiState.solvedCountToday,
                    streakDays = uiState.streakDays,
                    totalAttempts = uiState.totalAttempts,
                    correctAttempts = uiState.correctAttempts,
                    isGenerating = uiState.isGeneratingQuiz,
                    onNextQuiz = onNextQuiz
                )
            }

            // 2. Language Filter Row for Targeted Practice
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(languages) { lang ->
                        val selected = uiState.selectedLanguageFilter == lang
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onSelectLanguageFilter(lang)
                                onNextQuiz()
                            },
                            label = {
                                Text(
                                    text = if (lang == "全部") "全部语言 (${uiState.templates.size})" else lang,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
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

            // 3. Main Quiz Content State (Loading vs Empty vs Active Quiz)
            if (uiState.isGeneratingQuiz) {
                item {
                    QuizLoadingCard()
                }
            } else if (uiState.currentQuiz == null) {
                item {
                    EmptyQuizCard(onNavigateToTemplates = onNavigateToTemplates)
                }
            } else {
                val quiz = uiState.currentQuiz
                item {
                    InteractiveCodeQuizCard(
                        quiz = quiz,
                        userAnswer = uiState.userAnswerInput,
                        submissionState = uiState.submissionState,
                        onFocusInput = {
                            try {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            } catch (_: Exception) {
                            }
                        }
                    )
                }

                // 4. Interactive Blank Input & Quick Code Symbol Toolbar
                item {
                    AnswerInputConsoleCard(
                        quiz = quiz,
                        userAnswer = uiState.userAnswerInput,
                        submissionState = uiState.submissionState,
                        isHintVisible = uiState.isHintVisible,
                        focusRequester = focusRequester,
                        onAnswerChange = onAnswerChange,
                        onAppendSymbol = onAppendSymbol,
                        onSubmitAnswer = {
                            keyboardController?.hide()
                            onSubmitAnswer()
                        },
                        onToggleHint = onToggleHint,
                        onRevealAnswer = onRevealAnswer,
                        onNextQuiz = onNextQuiz
                    )
                }

                // 5. Expandable AI Hint Card
                item {
                    AnimatedVisibility(
                        visible = uiState.isHintVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        AiHintCard(hint = quiz.aiHint, missingLength = quiz.missingElement.length)
                    }
                }

                // 6. Submission Result & AI Explanation Card
                item {
                    AnimatedVisibility(
                        visible = uiState.submissionState != QuizSubmissionState.IDLE,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SubmissionFeedbackCard(
                            quiz = quiz,
                            submissionState = uiState.submissionState,
                            onNextQuiz = onNextQuiz
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun QuizHeroBanner(
    solvedToday: Int,
    streakDays: Int,
    totalAttempts: Int,
    correctAttempts: Int,
    isGenerating: Boolean,
    onNextQuiz: () -> Unit
) {
    val accuracy = if (totalAttempts > 0) (correctAttempts * 100) / totalAttempts else 100

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E1B4B).copy(alpha = 0.75f),
                            Color(0xFF0F172A).copy(alpha = 0.9f),
                            Color(0xFF064E3B).copy(alpha = 0.35f)
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                    drawRect(brush = brush)
                    // Subtle decorative circuit nodes
                    drawCircle(
                        color = Color(0xFF6366F1).copy(alpha = 0.14f),
                        radius = size.minDimension * 0.45f,
                        center = Offset(size.width * 0.88f, size.height * 0.2f)
                    )
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.10f),
                        radius = size.minDimension * 0.35f,
                        center = Offset(size.width * 0.15f, size.height * 0.85f)
                    )
                }
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI 驱动",
                                tint = CyanSpark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "OPENCODE AI · 每日代码填空挑战",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanSpark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "今日核心逻辑训练",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = onNextQuiz,
                        enabled = !isGenerating,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricIndigo,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("next_quiz_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "换一题",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "换一题",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Stats Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMiniPill(
                        label = "今日攻克",
                        value = "$solvedToday 题",
                        accent = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniPill(
                        label = "连续打卡",
                        value = "$streakDays 天",
                        accent = ElectricIndigoLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniPill(
                        label = "正确率",
                        value = "$accuracy%",
                        accent = CyanSpark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMiniPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B0F17).copy(alpha = 0.65f),
        border = BorderStroke(1.dp, SubtleBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
        }
    }
}

@Composable
private fun QuizLoadingCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_loading_indicator"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = ElectricIndigoLight,
                strokeWidth = 3.dp,
                modifier = Modifier.size(42.dp)
            )
            Text(
                text = "正在从 Supabase 抽取代码模板并由 OpenCode AI 生成填空题…",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Text(
                text = "正在分析抽象语法节点与关键条件表达式",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyQuizCard(onNavigateToTemplates: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = ElectricIndigoLight,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "尚无可用代码模板",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = "请先前往「代码模板库」添加或同步存储在 Supabase code_templates 表中的代码片段。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onNavigateToTemplates,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
            ) {
                Text("前往代码模板库")
            }
        }
    }
}

@Composable
private fun InteractiveCodeQuizCard(
    quiz: DailyQuizChallenge,
    userAnswer: String,
    submissionState: QuizSubmissionState,
    onFocusInput: () -> Unit
) {
    val langColor = SyntaxHighlighter.languageAccentColor(quiz.template.language)
    val diffColor = SyntaxHighlighter.difficultyColor(quiz.template.difficulty)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_code_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // IDE Window Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeGutterSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = langColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, langColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = quiz.template.language,
                            style = MaterialTheme.typography.labelMedium,
                            color = langColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = diffColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = quiz.template.difficulty,
                            style = MaterialTheme.typography.labelSmall,
                            color = diffColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // Target Element Category Badge
                Surface(
                    color = ElectricIndigoContainer,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, ElectricIndigoLight.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ElectricIndigoLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "考点：${quiz.elementCategory}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricIndigoLight
                        )
                    }
                }
            }

            HorizontalDivider(color = SubtleBorder, thickness = 1.dp)

            // Template Title & Description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = quiz.template.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (quiz.template.description.isNotBlank()) {
                    Text(
                        text = quiz.template.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Code Editor Area with Line Numbers & Inline Interactive Blank Slot
            val fullPlaceholderCode = quiz.codeBeforeBlank + "___BLANK_SLOT___" + quiz.codeAfterBlank
            val codeLines = remember(fullPlaceholderCode) { fullPlaceholderCode.lines() }
            val horizontalScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeEditorSurface)
                    .border(BorderStroke(1.dp, SubtleBorder.copy(alpha = 0.5f)))
                    .padding(vertical = 12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Line Numbers Gutter
                    Column(
                        modifier = Modifier
                            .background(CodeEditorSurface)
                            .padding(start = 12.dp, end = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        codeLines.forEachIndexed { index, _ ->
                            val hasBlank = codeLines[index].contains("___BLANK_SLOT___")
                            Text(
                                text = "${index + 1}",
                                style = CodeTextStyle.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (hasBlank) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (hasBlank) ElectricIndigoLight else TextMuted,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }

                    // Vertical Gutter Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height((codeLines.size * 24).dp)
                            .background(SubtleBorder)
                    )

                    // Scrollable Code Content with Inline Blank Pill
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                            .padding(horizontal = 12.dp)
                    ) {
                        codeLines.forEach { line ->
                            if (line.contains("___BLANK_SLOT___")) {
                                val parts = line.split("___BLANK_SLOT___", limit = 2)
                                val prefix = parts.getOrNull(0).orEmpty()
                                val suffix = parts.getOrNull(1).orEmpty()

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .height(26.dp)
                                        .background(
                                            color = ElectricIndigoLight.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                ) {
                                    if (prefix.isNotEmpty()) {
                                        Text(
                                            text = SyntaxHighlighter.highlight(prefix),
                                            style = CodeTextStyle
                                        )
                                    }

                                    InlineBlankPill(
                                        userAnswer = userAnswer,
                                        expectedLength = quiz.missingElement.length,
                                        submissionState = submissionState,
                                        onClick = onFocusInput
                                    )

                                    if (suffix.isNotEmpty()) {
                                        Text(
                                            text = SyntaxHighlighter.highlight(suffix),
                                            style = CodeTextStyle
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = SyntaxHighlighter.highlight(line),
                                        style = CodeTextStyle
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineBlankPill(
    userAnswer: String,
    expectedLength: Int,
    submissionState: QuizSubmissionState,
    onClick: () -> Unit
) {
    val (bgColor, borderColor, textColor) = when (submissionState) {
        QuizSubmissionState.CORRECT -> Triple(
            EmeraldContainer,
            EmeraldSuccess,
            Color(0xFFD1FAE5)
        )
        QuizSubmissionState.INCORRECT -> Triple(
            CoralContainer,
            CoralError,
            Color(0xFFFECACA)
        )
        QuizSubmissionState.REVEALED -> Triple(
            AmberContainer,
            AmberWarning,
            Color(0xFFFEF3C7)
        )
        QuizSubmissionState.IDLE -> Triple(
            ElectricIndigoContainer,
            ElectricIndigoLight,
            CyanSpark
        )
    }

    val displayText = if (userAnswer.isBlank()) {
        "  ? (${expectedLength}字符)  "
    } else {
        " $userAnswer "
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clickable(onClick = onClick)
            .testTag("inline_code_blank_pill")
    ) {
        Text(
            text = displayText,
            style = CodeTextStyle.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerInputConsoleCard(
    quiz: DailyQuizChallenge,
    userAnswer: String,
    submissionState: QuizSubmissionState,
    isHintVisible: Boolean,
    focusRequester: FocusRequester,
    onAnswerChange: (String) -> Unit,
    onAppendSymbol: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onToggleHint: () -> Unit,
    onRevealAnswer: () -> Unit,
    onNextQuiz: () -> Unit
) {
    val quickSymbols = listOf("()", ".", "==", "!=", "<", ">=", "->", "&&", "||", "[]")

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
                Text(
                    text = "补全缺失代码",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "目标长度: ${quiz.missingElement.length} 字符",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            OutlinedTextField(
                value = userAnswer,
                onValueChange = onAnswerChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("quiz_blank_input"),
                textStyle = CodeTextStyle.copy(fontSize = 15.sp, color = TextPrimary),
                placeholder = {
                    Text(
                        text = "输入缺失的 ${quiz.elementCategory}…",
                        style = CodeTextStyle.copy(color = TextMuted)
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = ElectricIndigoLight
                    )
                },
                trailingIcon = {
                    if (userAnswer.isNotEmpty()) {
                        IconButton(onClick = { onAnswerChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清空输入",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmitAnswer() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricIndigoLight,
                    unfocusedBorderColor = SubtleBorder,
                    focusedContainerColor = CodeEditorSurface,
                    unfocusedContainerColor = CodeEditorSurface
                )
            )

            // Quick Developer Symbol Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickSymbols) { sym ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CodeEditorSurface,
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.clickable {
                            val toAppend = if (sym == "()") "(" else if (sym == "[]") "[" else sym
                            onAppendSymbol(toAppend)
                        }
                    ) {
                        Text(
                            text = sym,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanSpark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSubmitAnswer,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("submit_quiz_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = Color(0xFF042F1E)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "提交验证",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onToggleHint,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("hint_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isHintVisible) AmberWarning else SubtleBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isHintVisible) AmberWarning else TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "AI 提示",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("提示", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onRevealAnswer,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("reveal_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SubtleBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "揭晓答案",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("答案", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun AiHintCard(hint: String, missingLength: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_hint_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = AmberContainer.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = AmberWarning,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "OpenCode AI 解题线索（答案共 $missingLength 个字符）",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberWarning
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFEF3C7)
                )
            }
        }
    }
}

@Composable
private fun SubmissionFeedbackCard(
    quiz: DailyQuizChallenge,
    submissionState: QuizSubmissionState,
    onNextQuiz: () -> Unit
) {
    val isCorrect = submissionState == QuizSubmissionState.CORRECT
    val isRevealed = submissionState == QuizSubmissionState.REVEALED

    val containerColor = when {
        isCorrect -> EmeraldContainer.copy(alpha = 0.65f)
        isRevealed -> ElectricIndigoContainer.copy(alpha = 0.7f)
        else -> CoralContainer.copy(alpha = 0.65f)
    }
    val borderColor = when {
        isCorrect -> EmeraldSuccess
        isRevealed -> ElectricIndigoLight
        else -> CoralError
    }
    val titleText = when {
        isCorrect -> "回答正确！核心逻辑完全匹配"
        isRevealed -> "已揭晓标准答案：${quiz.missingElement}"
        else -> "答案尚不匹配，请检查拼写、操作符或大小写"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("submission_feedback_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            if (isCorrect || isRevealed) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0B0F17).copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "核心代码解析",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanSpark
                        )
                        Text(
                            text = quiz.aiExplanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }

                Button(
                    onClick = onNextQuiz,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricIndigo,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("继续挑战下一题")
                }
            }
        }
    }
}
