package io.github.theodorelx.tunweave.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
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

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    @Synchronized
    fun log(level: LogLevel, tag: String, message: String) {
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

        val currentList = _logs.value.toMutableList()
        if (currentList.size >= MAX_LOGS) {
            currentList.removeAt(0)
        }
        currentList.add(entry)
        _logs.value = currentList
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

    fun clear() {
        _logs.value = emptyList()
    }
}
