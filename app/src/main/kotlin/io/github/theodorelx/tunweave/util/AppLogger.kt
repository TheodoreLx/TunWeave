package io.github.theodorelx.tunweave.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
)

enum class LogLevel(val displayName: String) {
    DEBUG("调试"), INFO("信息"), WARN("警告"), ERROR("错误")
}

object AppLogger {

    private const val MAX_LOGS = 500
    private const val REDACTED = "[REDACTED]"
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)

    private val pemPrivateKeyPattern = Regex(
        "-----BEGIN [^-]*(?:PRIVATE KEY|SECRET)[^-]*-----[\\s\\S]*?" +
            "-----END [^-]*(?:PRIVATE KEY|SECRET)[^-]*-----",
        RegexOption.IGNORE_CASE,
    )
    private val uriPasswordPattern = Regex(
        "(?i)([a-z][a-z0-9+.-]*://[^\\s/@:]+:)([^@\\s/]+)(@)",
    )
    private val authorizationPattern = Regex(
        "(?im)\\b(authorization|proxy-authorization|cookie|set-cookie)(\\s*[:=]\\s*)[^\\r\\n]+",
    )
    private val bearerPattern = Regex(
        "(?i)\\b(Bearer|Basic)(\\s+)[A-Za-z0-9._~+/=-]+",
    )
    private val namedSecretPattern = Regex(
        "(?i)\\b(password|passwd|pwd|pass|passphrase|private[_ -]?key|secret(?:[_ -]?key)?|" +
            "api[_ -]?key|access[_ -]?key|signing[_ -]?key|encryption[_ -]?key|token|" +
            "auth[_ -]?token|access[_ -]?token|refresh[_ -]?token|client[_ -]?secret|" +
            "密码|密钥|私钥|令牌)" +
            "(\\s*[:=]\\s*)(?:\"[^\"]*\"|'[^']*'|[^\\s,;&}\\]]+)",
    )

    @Volatile
    private var sensitiveValues: Set<String> = emptySet()

    @Volatile
    private var loggingEnabled = false

    @Volatile
    private var minimumLevel = LogLevel.INFO

    private val logs = ArrayDeque<LogEntry>(MAX_LOGS)

    @Synchronized
    fun log(level: LogLevel, tag: String, message: String) {
        if (!loggingEnabled || level.ordinal < minimumLevel.ordinal) return
        val safeMessage = redactSensitiveData(message)
        // Output to Android logcat safely (handles JVM unit tests without Log stubbing)
        try {
            when (level) {
                LogLevel.DEBUG -> Log.d(tag, safeMessage)
                LogLevel.INFO -> Log.i(tag, safeMessage)
                LogLevel.WARN -> Log.w(tag, safeMessage)
                LogLevel.ERROR -> Log.e(tag, safeMessage)
            }
        } catch (_: Throwable) {
            // JVM unit test environment
        }

        // Add to in-memory log buffer
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = safeMessage,
        )

        if (logs.size >= MAX_LOGS) {
            logs.removeFirst()
        }
        logs.addLast(entry)
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val stackTrace = throwable?.let {
            try {
                Log.getStackTraceString(it).takeIf { trace -> trace.isNotBlank() }
                    ?: it.stackTraceToString()
            } catch (_: Throwable) {
                it.stackTraceToString()
            }
        }
        val fullMessage = if (stackTrace != null) "$message\n$stackTrace" else message
        log(LogLevel.ERROR, tag, fullMessage)
    }

    /**
     * Replaces the current runtime secrets that must be removed even when an
     * exception embeds them without a recognizable `password=` style label.
     */
    fun setSensitiveValues(values: Iterable<String>) {
        sensitiveValues = values.filter { it.isNotEmpty() }.toSet()
    }

    @Synchronized
    fun configure(enabled: Boolean, level: LogLevel) {
        loggingEnabled = enabled
        minimumLevel = level
        if (!enabled) logs.clear()
    }

    internal fun redactSensitiveData(message: String): String {
        var redacted = message
        sensitiveValues.sortedByDescending { it.length }.forEach { secret ->
            redacted = redacted.replace(secret, REDACTED)
        }
        redacted = pemPrivateKeyPattern.replace(redacted, REDACTED)
        redacted = uriPasswordPattern.replace(redacted, "$1$REDACTED$3")
        redacted = authorizationPattern.replace(redacted, "$1$2$REDACTED")
        redacted = bearerPattern.replace(redacted, "$1$2$REDACTED")
        redacted = namedSecretPattern.replace(redacted, "$1$2$REDACTED")
        return redacted
    }

    @Synchronized
    fun clear() {
        logs.clear()
    }

    @Synchronized
    fun snapshot(): List<LogEntry> = logs.toList()

    @Synchronized
    fun exportText(): String = logs.joinToString("\n") { entry ->
        "[${entry.timestamp}][${entry.level}][${entry.tag}] ${entry.message}"
    }

    /** Keeps actionable diagnostics while releasing screen-only log history. */
    @Synchronized
    fun trimForBackground() {
        val essential = logs.filter { it.level == LogLevel.WARN || it.level == LogLevel.ERROR }
        logs.clear()
        logs.addAll(essential)
    }
}
