package io.github.theodorelx.tunweave.service

import android.os.Debug
import io.github.theodorelx.tunweave.data.ProcessMemoryStats
import io.github.theodorelx.tunweave.data.TrafficStats

/**
 * Traffic and memory monitoring utility.
 * Tracks upload/download bytes, calculates speed, and reports memory usage.
 */
class TrafficMonitor(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val memoryReader: () -> ProcessMemoryStats = ::readProcessMemoryStats,
) {

    companion object {
        internal const val MEMORY_SAMPLE_INTERVAL_MS = 5_000L

        private fun readProcessMemoryStats(): ProcessMemoryStats {
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            return ProcessMemoryStats(
                totalPssMb = memoryInfo.totalPss.kbToMb(),
                javaPssMb = memoryInfo.dalvikPss.kbToMb(),
                nativePssMb = memoryInfo.nativePss.kbToMb(),
                otherPssMb = memoryInfo.otherPss.kbToMb(),
            )
        }

        private fun Int.kbToMb(): Float = this / 1024f
    }

    private var totalUploadBytes: Long = 0L
    private var totalDownloadBytes: Long = 0L
    private var lastSourceUploadBytes: Long = 0L
    private var lastSourceDownloadBytes: Long = 0L
    private var lastSourceSampleTimeMs: Long = 0L
    private var startTimeMs: Long = 0L
    private var started = false
    private var memoryStats = ProcessMemoryStats()
    private var lastMemorySampleTimeMs: Long? = null

    fun start(uploadBytes: Long = 0L, downloadBytes: Long = 0L) {
        reset()
        val now = currentTimeMillis()
        startTimeMs = now
        lastSourceSampleTimeMs = now
        lastSourceUploadBytes = uploadBytes.coerceAtLeast(0L)
        lastSourceDownloadBytes = downloadBytes.coerceAtLeast(0L)
        started = true
        sampleMemoryIfNeeded(now)
    }

    /**
     * Consumes HEV's absolute byte counters and returns session-relative totals.
     * A missing sample keeps totals intact and reports zero instantaneous speed.
     */
    fun snapshot(
        uploadBytes: Long? = null,
        downloadBytes: Long? = null,
        memorySampleIntervalMs: Long = MEMORY_SAMPLE_INTERVAL_MS,
    ): TrafficStats {
        val now = currentTimeMillis()
        if (!started) {
            startTimeMs = now
            lastSourceSampleTimeMs = now
            lastSourceUploadBytes = uploadBytes?.coerceAtLeast(0L) ?: 0L
            lastSourceDownloadBytes = downloadBytes?.coerceAtLeast(0L) ?: 0L
            started = true
        }

        var uploadSpeed = 0L
        var downloadSpeed = 0L
        if (uploadBytes != null && downloadBytes != null &&
            uploadBytes >= 0L && downloadBytes >= 0L
        ) {
            val uploadDelta = counterDelta(lastSourceUploadBytes, uploadBytes)
            val downloadDelta = counterDelta(lastSourceDownloadBytes, downloadBytes)
            val sampleDurationMs = (now - lastSourceSampleTimeMs).coerceAtLeast(1L)

            totalUploadBytes = saturatedAdd(totalUploadBytes, uploadDelta)
            totalDownloadBytes = saturatedAdd(totalDownloadBytes, downloadDelta)
            uploadSpeed = bytesPerSecond(uploadDelta, sampleDurationMs)
            downloadSpeed = bytesPerSecond(downloadDelta, sampleDurationMs)

            lastSourceUploadBytes = uploadBytes
            lastSourceDownloadBytes = downloadBytes
            lastSourceSampleTimeMs = now
        }

        val connectedSec = (now - startTimeMs).coerceAtLeast(0L) / 1000L
        sampleMemoryIfNeeded(now, memorySampleIntervalMs.coerceAtLeast(1L))

        return TrafficStats(
            uploadSpeed = uploadSpeed,
            downloadSpeed = downloadSpeed,
            totalUpload = totalUploadBytes,
            totalDownload = totalDownloadBytes,
            memory = memoryStats,
            connectedTimeSec = connectedSec,
        )
    }

    fun reset() {
        totalUploadBytes = 0L
        totalDownloadBytes = 0L
        lastSourceUploadBytes = 0L
        lastSourceDownloadBytes = 0L
        lastSourceSampleTimeMs = 0L
        startTimeMs = 0L
        started = false
        memoryStats = ProcessMemoryStats()
        lastMemorySampleTimeMs = null
    }

    private fun counterDelta(previous: Long, current: Long): Long =
        if (current >= previous) current - previous else current

    private fun saturatedAdd(total: Long, delta: Long): Long =
        if (delta > Long.MAX_VALUE - total) Long.MAX_VALUE else total + delta

    private fun bytesPerSecond(bytes: Long, durationMs: Long): Long =
        (bytes.toDouble() * 1000.0 / durationMs.toDouble()).toLong()

    private fun sampleMemoryIfNeeded(now: Long, intervalMs: Long = MEMORY_SAMPLE_INTERVAL_MS) {
        val lastSample = lastMemorySampleTimeMs
        if (lastSample != null && now >= lastSample &&
            now - lastSample < intervalMs
        ) {
            return
        }

        memoryStats = try {
            memoryReader().sanitized()
        } catch (_: Throwable) {
            memoryStats
        }
        lastMemorySampleTimeMs = now
    }

    private fun ProcessMemoryStats.sanitized(): ProcessMemoryStats = copy(
        totalPssMb = totalPssMb.coerceAtLeast(0f),
        javaPssMb = javaPssMb.coerceAtLeast(0f),
        nativePssMb = nativePssMb.coerceAtLeast(0f),
        otherPssMb = otherPssMb.coerceAtLeast(0f),
    )
}
