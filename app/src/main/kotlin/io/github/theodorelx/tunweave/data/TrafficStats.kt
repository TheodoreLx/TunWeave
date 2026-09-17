package io.github.theodorelx.tunweave.data

data class TrafficStats(
    val uploadSpeed: Long = 0L,
    val downloadSpeed: Long = 0L,
    val totalUpload: Long = 0L,
    val totalDownload: Long = 0L,
    val memory: ProcessMemoryStats = ProcessMemoryStats(),
    val connectedTimeSec: Long = 0L,
)

data class ProcessMemoryStats(
    val totalPssMb: Float = 0f,
    val javaPssMb: Float = 0f,
    val nativePssMb: Float = 0f,
    val otherPssMb: Float = 0f,
    val codePssMb: Float = 0f,
    val stackPssMb: Float = 0f,
    val graphicsPssMb: Float = 0f,
    val privateOtherPssMb: Float = 0f,
    val systemPssMb: Float = 0f,
)
