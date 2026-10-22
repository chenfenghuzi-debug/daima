package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppDestination
import com.example.ui.CodePulseViewModel
import com.example.ui.SnackbarType
import com.example.ui.screens.DailyQuizScreen
import com.example.ui.screens.SupabaseSqlGuideDialog
import com.example.ui.screens.TemplateManagementScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricIndigoContainer
import com.example.ui.theme.ElectricIndigoLight
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CodePulseApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodePulseApp(
    viewModel: CodePulseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var lastSnackbarType by remember { mutableStateOf(SnackbarType.INFO) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { event ->
            lastSnackbarType = event.type
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = event.message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // BackHandler on secondary screen per navigation guidelines
    if (uiState.currentDestination != AppDestination.DAILY_QUIZ) {
        BackHandler {
            viewModel.navigateTo(AppDestination.DAILY_QUIZ)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianCanvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianCanvas,
                        titleContentColor = TextPrimary
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ElectricIndigoContainer,
                                border = BorderStroke(1.dp, ElectricIndigoLight.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = ElectricIndigoLight,
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (uiState.currentDestination == AppDestination.DAILY_QUIZ) {
                                        "Supabase + OpenCode AI 每日代码特训"
                                    } else {
                                        "Supabase · code_templates 表管理"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    },
                    actions = {
                        // Cloud & AI Connection Status Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CarbonSurface,
                            border = BorderStroke(1.dp, SubtleBorder),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { viewModel.toggleSqlGuideDialog(true) }
                                .testTag("connection_status_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.isCloudSynced) EmeraldSuccess else AmberWarning
                                        )
                                )
                                Text(
                                    text = if (uiState.isCloudSynced) "Supabase 已连接" else "配置指南",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "OpenCode AI 状态",
                                    tint = if (uiState.isOpenCodeConfigured) CyanSpark else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(color = SubtleBorder, thickness = 1.dp)
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val (bgColor, borderColor, icon) = when (lastSnackbarType) {
                    SnackbarType.SUCCESS -> Triple(
                        EmeraldContainer,
                        EmeraldSuccess,
                        Icons.Default.CheckCircle
                    )
                    SnackbarType.ERROR -> Triple(
                        CoralContainer,
                        CoralError,
                        Icons.Default.ErrorOutline
                    )
                    SnackbarType.INFO -> Triple(
                        ElectricIndigoContainer,
                        ElectricIndigoLight,
                        Icons.Default.Info
                    )
                }
                Snackbar(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("app_snackbar"),
                    containerColor = bgColor,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = borderColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = SubtleBorder, thickness = 1.dp)
                NavigationBar(
                    containerColor = CarbonSurface,
                    tonalElevation = 0.dp
                ) {
                    val isQuizSelected = uiState.currentDestination == AppDestination.DAILY_QUIZ
                    NavigationBarItem(
                        selected = isQuizSelected,
                        onClick = { viewModel.navigateTo(AppDestination.DAILY_QUIZ) },
                        icon = {
                            Icon(
                                imageVector = if (isQuizSelected) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                                contentDescription = stringResource(R.string.nav_daily_quiz)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_daily_quiz),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricIndigoLight,
                            selectedTextColor = ElectricIndigoLight,
                            indicatorColor = ElectricIndigoContainer,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_daily_quiz_tab")
                    )

                    val isTemplatesSelected = uiState.currentDestination == AppDestination.TEMPLATE_MANAGER
                    NavigationBarItem(
                        selected = isTemplatesSelected,
                        onClick = { viewModel.navigateTo(AppDestination.TEMPLATE_MANAGER) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.templates.isNotEmpty()) {
                                        Badge(
                                            containerColor = ElectricIndigoLight,
                                            contentColor = Color(0xFF090D16)
                                        ) {
                                            Text("${uiState.templates.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isTemplatesSelected) Icons.Filled.Code else Icons.Outlined.Code,
                                    contentDescription = stringResource(R.string.nav_templates)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_templates),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricIndigoLight,
                            selectedTextColor = ElectricIndigoLight,
                            indicatorColor = ElectricIndigoContainer,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_templates_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (uiState.currentDestination) {
            AppDestination.DAILY_QUIZ -> {
                DailyQuizScreen(
                    uiState = uiState,
                    onAnswerChange = viewModel::updateUserAnswerInput,
                    onAppendSymbol = viewModel::appendSymbolToAnswer,
                    onSubmitAnswer = viewModel::submitQuizAnswer,
                    onNextQuiz = { viewModel.generateRandomQuiz(uiState.selectedLanguageFilter) },
                    onToggleHint = viewModel::toggleHint,
                    onRevealAnswer = viewModel::revealAnswer,
                    onSelectLanguageFilter = viewModel::setLanguageFilter,
                    onNavigateToTemplates = { viewModel.navigateTo(AppDestination.TEMPLATE_MANAGER) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppDestination.TEMPLATE_MANAGER -> {
                TemplateManagementScreen(
                    uiState = uiState,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onSelectLanguageFilter = viewModel::setLanguageFilter,
                    onRefreshSupabase = viewModel::refreshTemplatesFromSupabase,
                    onCreateTemplate = viewModel::createTemplate,
                    onUpdateTemplate = viewModel::updateTemplate,
                    onDeleteTemplate = viewModel::deleteTemplate,
                    onPracticeTemplate = viewModel::startQuizWithTemplate,
                    onResetStarterTemplates = viewModel::resetStarterTemplates,
                    onToggleSqlGuide = viewModel::toggleSqlGuideDialog,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (uiState.showSqlGuideDialog) {
        SupabaseSqlGuideDialog(
            isSupabaseConfigured = uiState.isSupabaseConfigured,
            isOpenCodeConfigured = uiState.isOpenCodeConfigured,
            onDismiss = { viewModel.toggleSqlGuideDialog(false) },
            onCopied = {
                coroutineScope.launch {
                    lastSnackbarType = SnackbarType.SUCCESS
                    snackbarHostState.showSnackbar("已复制 code_templates 建表 SQL 到剪贴板")
                }
            }
        )
    }
}
