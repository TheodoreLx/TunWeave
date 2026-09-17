package io.github.theodorelx.tunweave

import io.github.theodorelx.tunweave.data.ProcessMemoryStats
import io.github.theodorelx.tunweave.service.TrafficMonitor
import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficMonitorTest {

    @Test
    fun absoluteCountersProduceSessionTotalsAndRates() {
        var now = 10_000L
        val monitor = createMonitor({ now })
        monitor.start(uploadBytes = 100L, downloadBytes = 200L)

        now += 1_000L
        val snapshot = monitor.snapshot(uploadBytes = 1_124L, downloadBytes = 4_296L)
        assertEquals(1024L, snapshot.totalUpload)
        assertEquals(4096L, snapshot.totalDownload)
        assertEquals(1024L, snapshot.uploadSpeed)
        assertEquals(4096L, snapshot.downloadSpeed)
        assertEquals(1L, snapshot.connectedTimeSec)
        assertEquals(32f, snapshot.memory.totalPssMb)
        assertEquals(8f, snapshot.memory.javaPssMb)
        assertEquals(16f, snapshot.memory.nativePssMb)
        assertEquals(8f, snapshot.memory.otherPssMb)
    }

    @Test
    fun ratesUseActualSamplingInterval() {
        var now = 0L
        val monitor = createMonitor({ now })
        monitor.start()

        now += 500L
        val snapshot = monitor.snapshot(uploadBytes = 500L, downloadBytes = 1_000L)
        assertEquals(1_000L, snapshot.uploadSpeed)
        assertEquals(2_000L, snapshot.downloadSpeed)
    }

    @Test
    fun missingSampleDoesNotLoseBytesOrInflateNextRate() {
        var now = 0L
        val monitor = createMonitor({ now })
        monitor.start()

        now += 1_000L
        val missing = monitor.snapshot()
        assertEquals(0L, missing.uploadSpeed)

        now += 1_000L
        val recovered = monitor.snapshot(uploadBytes = 2_000L, downloadBytes = 4_000L)
        assertEquals(2_000L, recovered.totalUpload)
        assertEquals(4_000L, recovered.totalDownload)
        assertEquals(1_000L, recovered.uploadSpeed)
        assertEquals(2_000L, recovered.downloadSpeed)
    }

    @Test
    fun nativeCounterResetPreservesSessionTotals() {
        var now = 0L
        val monitor = createMonitor({ now })
        monitor.start(uploadBytes = 5_000L, downloadBytes = 8_000L)

        now += 1_000L
        monitor.snapshot(uploadBytes = 6_000L, downloadBytes = 10_000L)
        now += 1_000L
        val afterReset = monitor.snapshot(uploadBytes = 250L, downloadBytes = 400L)

        assertEquals(1_250L, afterReset.totalUpload)
        assertEquals(2_400L, afterReset.totalDownload)
        assertEquals(250L, afterReset.uploadSpeed)
        assertEquals(400L, afterReset.downloadSpeed)
    }

    @Test
    fun resetClearsTotalsAndConnectionTime() {
        var now = 0L
        val monitor = createMonitor({ now })
        monitor.start()
        now += 1_000L
        monitor.snapshot(uploadBytes = 5_000L, downloadBytes = 2_000L)
        monitor.reset()

        val snapshot = monitor.snapshot()
        assertEquals(0L, snapshot.totalUpload)
        assertEquals(0L, snapshot.totalDownload)
        assertEquals(0L, snapshot.uploadSpeed)
        assertEquals(0L, snapshot.connectedTimeSec)
    }

    @Test
    fun pssMemoryIsSampledAtMostEveryFiveSeconds() {
        var now = 0L
        var reads = 0
        val monitor = TrafficMonitor(
            currentTimeMillis = { now },
            memoryReader = {
                reads++
                ProcessMemoryStats(totalPssMb = reads.toFloat())
            },
        )

        monitor.start()
        assertEquals(1, reads)
        now += TrafficMonitor.MEMORY_SAMPLE_INTERVAL_MS - 1L
        assertEquals(1f, monitor.snapshot().memory.totalPssMb)
        assertEquals(1, reads)

        now += 1L
        assertEquals(2f, monitor.snapshot().memory.totalPssMb)
        assertEquals(2, reads)
    }

    @Test
    fun pssMemorySupportsLongerBackgroundSampleInterval() {
        var now = 0L
        var reads = 0
        val monitor = TrafficMonitor(
            currentTimeMillis = { now },
            memoryReader = {
                reads++
                ProcessMemoryStats(totalPssMb = reads.toFloat())
            },
        )

        monitor.start()
        now += 30_000L
        monitor.snapshot(memorySampleIntervalMs = 60_000L)
        assertEquals(1, reads)
        now += 30_000L
        monitor.snapshot(memorySampleIntervalMs = 60_000L)
        assertEquals(2, reads)
    }

    @Test
    fun detailedPssBreakdownIsPreservedAndSanitized() {
        val monitor = TrafficMonitor(
            currentTimeMillis = { 0L },
            memoryReader = {
                ProcessMemoryStats(
                    totalPssMb = 42f,
                    javaPssMb = 8f,
                    nativePssMb = 5f,
                    otherPssMb = 29f,
                    codePssMb = 12f,
                    stackPssMb = 1f,
                    graphicsPssMb = 4f,
                    privateOtherPssMb = 3f,
                    systemPssMb = -1f,
                )
            },
        )

        monitor.start()
        val memory = monitor.snapshot().memory
        assertEquals(12f, memory.codePssMb)
        assertEquals(1f, memory.stackPssMb)
        assertEquals(4f, memory.graphicsPssMb)
        assertEquals(3f, memory.privateOtherPssMb)
        assertEquals(0f, memory.systemPssMb)
    }

    private fun createMonitor(currentTimeMillis: () -> Long): TrafficMonitor =
        TrafficMonitor(
            currentTimeMillis = currentTimeMillis,
            memoryReader = {
                ProcessMemoryStats(
                    totalPssMb = 32f,
                    javaPssMb = 8f,
                    nativePssMb = 16f,
                    otherPssMb = 8f,
                )
            },
        )
}
