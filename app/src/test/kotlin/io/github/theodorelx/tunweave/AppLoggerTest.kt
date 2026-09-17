package io.github.theodorelx.tunweave

import io.github.theodorelx.tunweave.util.AppLogger
import io.github.theodorelx.tunweave.util.LogLevel
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLoggerTest {

    @Before
    fun setUp() {
        AppLogger.configure(true, LogLevel.DEBUG)
    }

    @After
    fun tearDown() {
        AppLogger.clear()
        AppLogger.setSensitiveValues(emptyList())
        AppLogger.configure(false, LogLevel.INFO)
    }

    @Test
    fun errorLogKeepsDiagnosticContextButRedactsUriPassword() {
        val exception = IllegalStateException(
            "connect socks5://alice:correct-horse@192.0.2.1:1080/private failed",
        )

        AppLogger.e("Test", "连接失败", exception)

        val entry = AppLogger.snapshot().single()
        assertTrue(entry.message.contains("IllegalStateException"))
        assertTrue(entry.message.contains("192.0.2.1:1080/private"))
        assertTrue(entry.message.contains("AppLoggerTest"))
        assertTrue(entry.message.contains("socks5://alice:[REDACTED]@"))
        assertFalse(entry.message.contains("correct-horse"))
    }

    @Test
    fun redactsNamedCredentialsAndAuthorizationHeaders() {
        val message = listOf(
            "password=hunter2",
            "Authorization: Bearer abc.def",
            "private_key: 'key-data'",
            "token=token-data",
            "密码: 中文密码",
        ).joinToString("\n")

        val redacted = AppLogger.redactSensitiveData(message)

        assertFalse(redacted.contains("hunter2"))
        assertFalse(redacted.contains("abc.def"))
        assertFalse(redacted.contains("key-data"))
        assertFalse(redacted.contains("token-data"))
        assertFalse(redacted.contains("中文密码"))
        assertTrue(redacted.contains("password=[REDACTED]"))
    }

    @Test
    fun redactsConfiguredPasswordWithoutRemovingEndpointOrPath() {
        AppLogger.setSensitiveValues(listOf("unlabelled-secret"))

        AppLogger.e(
            "Test",
            "连接 192.0.2.1 失败",
            IllegalArgumentException("/data/user/0/app/config: unlabelled-secret"),
        )

        val message = AppLogger.snapshot().single().message
        assertTrue(message.contains("192.0.2.1"))
        assertTrue(message.contains("/data/user/0/app/config"))
        assertTrue(message.contains("[REDACTED]"))
        assertFalse(message.contains("unlabelled-secret"))
    }

    @Test
    fun redactsPemPrivateKeyBlock() {
        val message = """
            key load failed:
            -----BEGIN PRIVATE KEY-----
            cHJpdmF0ZS1rZXktbWF0ZXJpYWw=
            -----END PRIVATE KEY-----
            at /data/user/0/app/key.pem
        """.trimIndent()

        val redacted = AppLogger.redactSensitiveData(message)

        assertFalse(redacted.contains("cHJpdmF0ZS1rZXktbWF0ZXJpYWw="))
        assertTrue(redacted.contains("[REDACTED]"))
        assertTrue(redacted.contains("/data/user/0/app/key.pem"))
    }

    @Test
    fun loggingSwitchAndMinimumLevelAreApplied() {
        AppLogger.configure(false, LogLevel.DEBUG)
        AppLogger.d("Test", "hidden debug message")
        assertTrue(AppLogger.snapshot().isEmpty())

        AppLogger.configure(true, LogLevel.INFO)
        AppLogger.d("Test", "filtered debug message")
        AppLogger.i("Test", "visible info message")
        assertEquals(listOf(LogLevel.INFO), AppLogger.snapshot().map { it.level })
    }

    @Test
    fun backgroundTrimKeepsOnlyWarningsAndErrors() {
        AppLogger.configure(true, LogLevel.DEBUG)
        AppLogger.d("Test", "debug")
        AppLogger.i("Test", "info")
        AppLogger.w("Test", "warning")
        AppLogger.e("Test", "error")

        AppLogger.trimForBackground()

        assertEquals(
            listOf(LogLevel.WARN, LogLevel.ERROR),
            AppLogger.snapshot().map { it.level },
        )
    }

    @Test
    fun exportedTextContainsFilteredEntries() {
        AppLogger.configure(true, LogLevel.WARN)
        AppLogger.i("Test", "not exported")
        AppLogger.w("Test", "exported warning")

        val text = AppLogger.exportText()

        assertTrue(text.contains("[WARN][Test] exported warning"))
        assertFalse(text.contains("not exported"))
    }
}
