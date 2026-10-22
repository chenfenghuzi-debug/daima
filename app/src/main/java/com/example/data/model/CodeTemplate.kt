package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CodeTemplate(
    val id: String,
    val title: String,
    val language: String,
    val code: String,
    val description: String = "",
    val difficulty: String = "中等",
    val tags: String = "",
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class CodeTemplateUpsert(
    val id: String,
    val title: String,
    val language: String,
    val code: String,
    val description: String,
    val difficulty: String,
    val tags: String
)

data class DailyQuizChallenge(
    val quizId: String,
    val template: CodeTemplate,
    val codeBeforeBlank: String,
    val codeAfterBlank: String,
    val missingElement: String,
    val elementCategory: String,
    val aiHint: String,
    val aiExplanation: String,
    val generatedByAi: Boolean,
    val aiNotice: String? = null
)

enum class QuizSubmissionState {
    IDLE,
    CORRECT,
    INCORRECT,
    REVEALED
}

object StarterCodeTemplates {
    val defaults = listOf(
        CodeTemplate(
            id = "tpl-kotlin-flow-debounce",
            title = "Kotlin Flow 搜索防抖与最新流切换",
            language = "Kotlin",
            difficulty = "中等",
            tags = "Coroutines, Flow, 响应式编程",
            description = "使用 debounce 过滤高频输入，并通过 flatMapLatest 取消未完成的旧请求，仅保留最新搜索结果。",
            code = """
fun observeSearchQuery(queryFlow: Flow<String>): Flow<List<Repo>> {
    return queryFlow
        .debounce(300L)
        .filter { it.isNotBlank() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            repository.searchRepositories(query)
        }
        .flowOn(Dispatchers.IO)
}
            """.trimIndent()
        ),
        CodeTemplate(
            id = "tpl-python-binary-search",
            title = "Python 二分查找左边界 (Lower Bound)",
            language = "Python",
            difficulty = "简单",
            tags = "算法, 二分查找, 数组边界",
            description = "在有序数组中查找第一个大于或等于 target 的索引位置，注意 mid 计算与左右指针收敛条件。",
            code = """
def lower_bound(nums: list[int], target: int) -> int:
    left, right = 0, len(nums)
    while left < right:
        mid = left + (right - left) // 2
        if nums[mid] < target:
            left = mid + 1
        else:
            right = mid
    return left
            """.trimIndent()
        ),
        CodeTemplate(
            id = "tpl-kotlin-compose-derived",
            title = "Jetpack Compose derivedStateOf 性能优化",
            language = "Kotlin",
            difficulty = "中等",
            tags = "Compose, 性能优化, 状态管理",
            description = "当滚动偏移量高频变化时，使用 derivedStateOf 仅在布尔阈值跨界时触发重组。",
            code = """
@Composable
fun ScrollToTopFab(listState: LazyListState) {
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }
    AnimatedVisibility(visible = showButton) {
        FloatingActionButton(onClick = { /* scroll */ }) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Top")
        }
    }
}
            """.trimIndent()
        ),
        CodeTemplate(
            id = "tpl-ts-promise-race",
            title = "TypeScript 异步请求超时控制封装",
            language = "TypeScript",
            difficulty = "中等",
            tags = "Async, Promise, 错误处理",
            description = "利用 Promise.race 将业务请求与定时器竞争，避免网络挂起导致界面无限等待。",
            code = """
async function fetchWithTimeout<T>(
    promise: Promise<T>,
    timeoutMs: number
): Promise<T> {
    const timeout = new Promise<never>((_, reject) =>
        setTimeout(() => reject(new Error("Request timed out")), timeoutMs)
    );
    return Promise.race([promise, timeout]);
}
            """.trimIndent()
        ),
        CodeTemplate(
            id = "tpl-rust-result-and-then",
            title = "Rust Result 链式错误传递与转换",
            language = "Rust",
            difficulty = "困难",
            tags = "Rust, Result, 函数式组合",
            description = "使用 and_then 串联多个可能失败的解析步骤，保持零异常开销的优雅错误处理。",
            code = """
fn parse_port_config(raw: &str) -> Result<u16, String> {
    raw.trim()
        .parse::<u16>()
        .map_err(|e| format!("Invalid number: {}", e))
        .and_then(|port| {
            if port >= 1024 {
                Ok(port)
            } else {
                Err("Port must be unprivileged (>= 1024)".to_string())
            }
        })
}
            """.trimIndent()
        ),
        CodeTemplate(
            id = "tpl-sql-window-rank",
            title = "SQL 窗口函数按部门查询薪资前三名",
            language = "SQL",
            difficulty = "中等",
            tags = "SQL, Window Function, DENSE_RANK",
            description = "通过 PARTITION BY 部门分组与 DENSE_RANK() 排序，精准筛选每个部门的顶尖薪资记录。",
            code = """
WITH RankedSalaries AS (
    SELECT
        employee_id,
        department_id,
        salary,
        DENSE_RANK() OVER (
            PARTITION BY department_id
            ORDER BY salary DESC
        ) AS salary_rank
    FROM employees
)
SELECT employee_id, department_id, salary
FROM RankedSalaries
WHERE salary_rank <= 3;
            """.trimIndent()
        )
    )
}
