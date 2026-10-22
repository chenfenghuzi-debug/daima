package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricIndigoLight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType
import com.example.ui.theme.TextPrimary

object SyntaxHighlighter {

    private val keywords = setOf(
        "fun", "val", "var", "if", "else", "when", "for", "while", "return", "suspend",
        "class", "interface", "object", "package", "import", "by", "in", "is", "as",
        "def", "async", "await", "function", "const", "let", "new", "try", "catch",
        "fn", "pub", "mut", "match", "impl", "struct", "enum", "use", "true", "false", "null", "None",
        "WITH", "SELECT", "FROM", "WHERE", "AS", "OVER", "PARTITION", "BY", "ORDER", "DESC", "ASC", "AND", "OR"
    )

    private val keywordRegex = Regex("""\b(${keywords.joinToString("|")})\b""")
    private val stringRegex = Regex(""""[^"\\]*(\\.[^"\\]*)*"|'[^'\\]*(\\.[^'\\]*)*'""")
    private val numberRegex = Regex("""\b\d+(\.\d+)?[Lfd]?\b""")
    private val functionCallRegex = Regex("""\b([a-zA-Z_][a-zA-Z0-9_]*)\s*(?=\()""")
    private val typeRegex = Regex("""\b([A-Z][a-zA-Z0-9_]*)\b""")
    private val commentRegex = Regex("""(//.*$|#.*$|--.*$|/\*[\s\S]*?\*/)""", RegexOption.MULTILINE)

    fun highlight(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = TextPrimary), 0, code.length)

            typeRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = SyntaxType), match.range.first, match.range.last + 1)
            }

            functionCallRegex.findAll(code).forEach { match ->
                val group = match.groups[1]
                if (group != null) {
                    addStyle(SpanStyle(color = SyntaxFunction), group.range.first, group.range.last + 1)
                }
            }

            numberRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = SyntaxNumber), match.range.first, match.range.last + 1)
            }

            keywordRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold),
                    match.range.first,
                    match.range.last + 1
                )
            }

            stringRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = SyntaxString), match.range.first, match.range.last + 1)
            }

            commentRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = SyntaxComment), match.range.first, match.range.last + 1)
            }
        }
    }

    fun languageAccentColor(language: String): Color {
        return when (language.lowercase()) {
            "kotlin" -> Color(0xFFA855F7)
            "python" -> Color(0xFF38BDF8)
            "typescript", "javascript" -> Color(0xFFFBBF24)
            "rust" -> Color(0xFFF97316)
            "sql" -> EmeraldSuccess
            "java" -> Color(0xFFEC4899)
            else -> ElectricIndigoLight
        }
    }

    fun difficultyColor(difficulty: String): Color {
        return when (difficulty) {
            "简单" -> EmeraldSuccess
            "困难" -> Color(0xFFEF4444)
            else -> CyanSpark
        }
    }
}
