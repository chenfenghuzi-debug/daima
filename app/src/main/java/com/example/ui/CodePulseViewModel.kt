package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.opencode.OpenCodeQuizService
import com.example.data.model.CodeTemplate
import com.example.data.model.DailyQuizChallenge
import com.example.data.model.QuizSubmissionState
import com.example.data.supabase.SupabaseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination {
    DAILY_QUIZ,
    TEMPLATE_MANAGER
}

enum class SnackbarType {
    SUCCESS,
    ERROR,
    INFO
}

data class SnackbarEvent(
    val message: String,
    val type: SnackbarType = SnackbarType.INFO
)

data class CodePulseUiState(
    val currentDestination: AppDestination = AppDestination.DAILY_QUIZ,
    val templates: List<CodeTemplate> = emptyList(),
    val selectedLanguageFilter: String = "全部",
    val searchQuery: String = "",
    val isLoadingTemplates: Boolean = false,
    val isGeneratingQuiz: Boolean = false,
    val isSavingTemplate: Boolean = false,
    val isCloudSynced: Boolean = false,
    val isSupabaseConfigured: Boolean = false,
    val isOpenCodeConfigured: Boolean = false,
    val currentQuiz: DailyQuizChallenge? = null,
    val userAnswerInput: String = "",
    val submissionState: QuizSubmissionState = QuizSubmissionState.IDLE,
    val isHintVisible: Boolean = false,
    val solvedCountToday: Int = 0,
    val streakDays: Int = 1,
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val showSqlGuideDialog: Boolean = false
)

class CodePulseViewModel(application: Application) : AndroidViewModel(application) {

    private val supabaseRepository = SupabaseRepository(application.applicationContext)
    private val openCodeQuizService = OpenCodeQuizService()
    private val statsPrefs = application.applicationContext.getSharedPreferences("codepulse_stats", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        CodePulseUiState(
            isSupabaseConfigured = supabaseRepository.isSupabaseConfigured,
            isOpenCodeConfigured = openCodeQuizService.isOpenCodeKeyConfigured,
            solvedCountToday = statsPrefs.getInt("solved_today", 0),
            streakDays = statsPrefs.getInt("streak_days", 3).coerceAtLeast(1),
            totalAttempts = statsPrefs.getInt("total_attempts", 0),
            correctAttempts = statsPrefs.getInt("correct_attempts", 0)
        )
    )
    val uiState: StateFlow<CodePulseUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    init {
        loadTemplatesAndGenerateInitialQuiz()
    }

    private fun loadTemplatesAndGenerateInitialQuiz() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTemplates = true, isGeneratingQuiz = true) }
            val result = supabaseRepository.fetchTemplates()
            _uiState.update {
                it.copy(
                    templates = result.data,
                    isCloudSynced = result.isCloudSynced,
                    isLoadingTemplates = false
                )
            }
            if (result.data.isNotEmpty()) {
                val randomTemplate = result.data.random()
                generateQuizForTemplateInternal(randomTemplate, announceSnackbar = false)
            } else {
                _uiState.update { it.copy(isGeneratingQuiz = false) }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
    }

    fun setLanguageFilter(language: String) {
        _uiState.update { it.copy(selectedLanguageFilter = language) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSqlGuideDialog(show: Boolean) {
        _uiState.update { it.copy(showSqlGuideDialog = show) }
    }

    fun refreshTemplatesFromSupabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTemplates = true) }
            val result = supabaseRepository.fetchTemplates()
            _uiState.update {
                it.copy(
                    templates = result.data,
                    isCloudSynced = result.isCloudSynced,
                    isLoadingTemplates = false
                )
            }
            result.message?.let { msg ->
                _snackbarEvents.tryEmit(
                    SnackbarEvent(
                        message = msg,
                        type = if (result.isCloudSynced) SnackbarType.SUCCESS else SnackbarType.INFO
                    )
                )
            }
        }
    }

    fun generateRandomQuiz(preferredLanguage: String? = null) {
        val currentList = _uiState.value.templates
        if (currentList.isEmpty()) {
            _snackbarEvents.tryEmit(
                SnackbarEvent("当前没有可用的代码模板，请先在「代码模板库」中添加模板", SnackbarType.ERROR)
            )
            return
        }
        val filteredPool = if (!preferredLanguage.isNullOrBlank() && preferredLanguage != "全部") {
            currentList.filter { it.language.equals(preferredLanguage, ignoreCase = true) }.ifEmpty { currentList }
        } else {
            currentList
        }

        // Try to pick a different template than the current one if multiple exist
        val currentId = _uiState.value.currentQuiz?.template?.id
        val candidatePool = if (filteredPool.size > 1 && currentId != null) {
            filteredPool.filterNot { it.id == currentId }
        } else {
            filteredPool
        }
        val chosen = candidatePool.random()

        viewModelScope.launch {
            generateQuizForTemplateInternal(chosen, announceSnackbar = true)
        }
    }

    fun startQuizWithTemplate(template: CodeTemplate) {
        _uiState.update { it.copy(currentDestination = AppDestination.DAILY_QUIZ) }
        viewModelScope.launch {
            generateQuizForTemplateInternal(template, announceSnackbar = true)
        }
    }

    private suspend fun generateQuizForTemplateInternal(template: CodeTemplate, announceSnackbar: Boolean) {
        _uiState.update {
            it.copy(
                isGeneratingQuiz = true,
                userAnswerInput = "",
                submissionState = QuizSubmissionState.IDLE,
                isHintVisible = false
            )
        }
        val quiz = openCodeQuizService.generateQuizFromTemplate(template)
        _uiState.update {
            it.copy(
                currentQuiz = quiz,
                isGeneratingQuiz = false,
                userAnswerInput = "",
                submissionState = QuizSubmissionState.IDLE,
                isHintVisible = false
            )
        }
        if (announceSnackbar) {
            if (quiz.generatedByAi) {
                _snackbarEvents.tryEmit(
                    SnackbarEvent("OpenCode AI 已从《${template.title}》生成新填空题", SnackbarType.SUCCESS)
                )
            } else if (quiz.aiNotice != null) {
                _snackbarEvents.tryEmit(
                    SnackbarEvent(quiz.aiNotice, SnackbarType.INFO)
                )
            }
        }
    }

    fun updateUserAnswerInput(input: String) {
        _uiState.update { state ->
            val nextSubmissionState = if (state.submissionState == QuizSubmissionState.INCORRECT) {
                QuizSubmissionState.IDLE
            } else {
                state.submissionState
            }
            state.copy(
                userAnswerInput = input,
                submissionState = nextSubmissionState
            )
        }
    }

    fun appendSymbolToAnswer(symbol: String) {
        _uiState.update { state ->
            state.copy(
                userAnswerInput = state.userAnswerInput + symbol,
                submissionState = if (state.submissionState == QuizSubmissionState.INCORRECT) {
                    QuizSubmissionState.IDLE
                } else {
                    state.submissionState
                }
            )
        }
    }

    fun toggleHint() {
        _uiState.update { it.copy(isHintVisible = !it.isHintVisible) }
    }

    fun revealAnswer() {
        val quiz = _uiState.value.currentQuiz ?: return
        _uiState.update {
            it.copy(
                userAnswerInput = quiz.missingElement,
                submissionState = QuizSubmissionState.REVEALED,
                isHintVisible = true
            )
        }
        _snackbarEvents.tryEmit(
            SnackbarEvent("已揭晓正确答案：${quiz.missingElement}", SnackbarType.INFO)
        )
    }

    fun submitQuizAnswer() {
        val state = _uiState.value
        val quiz = state.currentQuiz ?: return
        val rawInput = state.userAnswerInput.trim()

        if (rawInput.isEmpty()) {
            _snackbarEvents.tryEmit(
                SnackbarEvent("请先输入缺失的关键代码后再提交验证", SnackbarType.ERROR)
            )
            return
        }

        // Normalize whitespace around operators while keeping exact identifier case
        val normalize = { s: String -> s.replace(Regex("""\s+"""), " ").trim() }
        val isMatch = normalize(rawInput) == normalize(quiz.missingElement) ||
            rawInput.replace(" ", "") == quiz.missingElement.replace(" ", "")

        val newTotal = state.totalAttempts + 1
        val newCorrect = if (isMatch) state.correctAttempts + 1 else state.correctAttempts
        val newSolvedToday = if (isMatch && state.submissionState != QuizSubmissionState.CORRECT) {
            state.solvedCountToday + 1
        } else {
            state.solvedCountToday
        }

        statsPrefs.edit()
            .putInt("total_attempts", newTotal)
            .putInt("correct_attempts", newCorrect)
            .putInt("solved_today", newSolvedToday)
            .apply()

        if (isMatch) {
            _uiState.update {
                it.copy(
                    submissionState = QuizSubmissionState.CORRECT,
                    totalAttempts = newTotal,
                    correctAttempts = newCorrect,
                    solvedCountToday = newSolvedToday
                )
            }
            _snackbarEvents.tryEmit(
                SnackbarEvent("回答正确！核心逻辑「${quiz.missingElement}」完全匹配", SnackbarType.SUCCESS)
            )
        } else {
            _uiState.update {
                it.copy(
                    submissionState = QuizSubmissionState.INCORRECT,
                    totalAttempts = newTotal,
                    correctAttempts = newCorrect
                )
            }
            _snackbarEvents.tryEmit(
                SnackbarEvent("答案不匹配，请结合上下文或点击「查看 AI 提示」再试一次", SnackbarType.ERROR)
            )
        }
    }

    fun createTemplate(
        title: String,
        language: String,
        code: String,
        description: String,
        difficulty: String,
        tags: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingTemplate = true) }
            val result = supabaseRepository.createTemplate(
                title = title,
                language = language,
                code = code,
                description = description,
                difficulty = difficulty,
                tags = tags
            )
            _uiState.update {
                it.copy(
                    templates = result.data,
                    isCloudSynced = result.isCloudSynced,
                    isSavingTemplate = false
                )
            }
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = result.message ?: "代码模板已创建",
                    type = if (result.isCloudSynced) SnackbarType.SUCCESS else SnackbarType.INFO
                )
            )
        }
    }

    fun updateTemplate(template: CodeTemplate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingTemplate = true) }
            val result = supabaseRepository.updateTemplate(template)
            _uiState.update { state ->
                val updatedQuiz = if (state.currentQuiz?.template?.id == template.id) {
                    state.currentQuiz.copy(template = template)
                } else {
                    state.currentQuiz
                }
                state.copy(
                    templates = result.data,
                    isCloudSynced = result.isCloudSynced,
                    isSavingTemplate = false,
                    currentQuiz = updatedQuiz
                )
            }
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = result.message ?: "代码模板已更新",
                    type = if (result.isCloudSynced) SnackbarType.SUCCESS else SnackbarType.INFO
                )
            )
        }
    }

    fun deleteTemplate(template: CodeTemplate) {
        viewModelScope.launch {
            val result = supabaseRepository.deleteTemplate(template.id)
            _uiState.update {
                it.copy(
                    templates = result.data,
                    isCloudSynced = result.isCloudSynced
                )
            }
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = result.message ?: "已删除《${template.title}》",
                    type = SnackbarType.INFO
                )
            )
            if (_uiState.value.currentQuiz?.template?.id == template.id && result.data.isNotEmpty()) {
                generateQuizForTemplateInternal(result.data.random(), announceSnackbar = false)
            }
        }
    }

    fun resetStarterTemplates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTemplates = true) }
            val result = supabaseRepository.resetAndPushStarterTemplates()
            _uiState.update {
                it.copy(
                    templates = result.data,
                    isCloudSynced = result.isCloudSynced,
                    isLoadingTemplates = false
                )
            }
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = result.message ?: "已恢复默认精选代码模板",
                    type = SnackbarType.SUCCESS
                )
            )
        }
    }
}
