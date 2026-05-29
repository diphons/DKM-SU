package com.diphons.dkmsu.ui.uibench

import android.app.Activity
import android.os.*
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class RenderJitter : ComponentActivity() {
    private val handlerThread = HandlerThread("frameMetricsListener").apply { start() }
    private val metricsHandler = Handler(handlerThread.looper)

    private var frameMetricsState = mutableStateOf(FrameStats())

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tambahkan listener untuk FrameMetrics
        window.addOnFrameMetricsAvailableListener(
            metricsListener(),
            metricsHandler
        )

        setContent {
            MaterialTheme {
                RenderingJitterScreen(frameMetricsState.value)
            }
        }
    }

    private fun metricsListener(): Window.OnFrameMetricsAvailableListener {
        return object : Window.OnFrameMetricsAvailableListener {
            private val WEIGHT = 40.0
            private var prevTotal = 0L
            private var jitterMma = 0.0
            private var uiFrametimeMma = 0.0
            private var rtFrametimeMma = 0.0
            private var totalFrametimeMma = 0.0
            private var mostlyTotalMma = 0.0
            private var first = true

            override fun onFrameMetricsAvailable(
                window: Window,
                frameMetrics: FrameMetrics,
                dropCountSinceLastInvocation: Int
            ) {
                if (frameMetrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) return

                val uiDuration =
                    frameMetrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION) +
                            frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION) +
                            frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) +
                            frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)

                val rtDuration =
                    frameMetrics.getMetric(FrameMetrics.SYNC_DURATION) +
                            frameMetrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)

                val totalDuration = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                val jitter = abs(totalDuration - prevTotal)

                if (first) {
                    jitterMma = 0.0
                    uiFrametimeMma = uiDuration.toDouble()
                    rtFrametimeMma = rtDuration.toDouble()
                    totalFrametimeMma = totalDuration.toDouble()
                    mostlyTotalMma = (uiDuration + rtDuration).toDouble()
                    first = false
                } else {
                    jitterMma = add(jitterMma, jitter.toDouble())
                    uiFrametimeMma = add(uiFrametimeMma, uiDuration.toDouble())
                    rtFrametimeMma = add(rtFrametimeMma, rtDuration.toDouble())
                    totalFrametimeMma = add(totalFrametimeMma, totalDuration.toDouble())
                    mostlyTotalMma = add(mostlyTotalMma, (uiDuration + rtDuration).toDouble())
                }
                prevTotal = totalDuration

                frameMetricsState.value = FrameStats(
                    jitter = jitterMma / 1_000_000.0,
                    ui = uiFrametimeMma / 1_000_000.0,
                    rt = rtFrametimeMma / 1_000_000.0,
                    total = totalFrametimeMma / 1_000_000.0,
                    mostlyTotal = mostlyTotalMma / 1_000_000.0,
                    jitterSample = (jitter / 1000).toInt(),
                    jitterAvg = (jitterMma / 1000).toInt()
                )
            }

            private fun add(prev: Double, new: Double): Double {
                return ((WEIGHT - 1) * prev + new) / WEIGHT
            }
        }
    }
}

data class FrameStats(
    val jitter: Double = 0.0,
    val ui: Double = 0.0,
    val rt: Double = 0.0,
    val total: Double = 0.0,
    val mostlyTotal: Double = 0.0,
    val jitterSample: Int = 0,
    val jitterAvg: Int = 0
)

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun RenderingJitterScreen(stats: FrameStats) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackground())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Jitter: %.3f ms".format(stats.jitter))
            Text(text = "CPU-total duration: %.3f ms".format(stats.mostlyTotal))
            Text(text = "UI duration: %.3f ms".format(stats.ui))
            Text(text = "RT duration: %.3f ms".format(stats.rt))
            Text(text = "Total duration: %.3f ms".format(stats.total))
            Spacer(modifier = Modifier.height(16.dp))
            JitterGraph(stats)
        }
    }
}

/**
 * Versi Compose dari PointGraphView
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun JitterGraph(stats: FrameStats) {
    val density = LocalContext.current.resources.displayMetrics.density
    val points = remember { mutableStateListOf<Pair<Float, Float>>() }
    val avgPoints = remember { mutableStateListOf<Pair<Float, Float>>() }

    val maxPoints = 200

    LaunchedEffect(stats.jitterSample) {
        if (points.size > maxPoints) points.removeFirst()
        if (avgPoints.size > maxPoints) avgPoints.removeFirst()
        points.add(Pair(points.size.toFloat(), stats.jitterSample / 1000f))
        avgPoints.add(Pair(avgPoints.size.toFloat(), stats.jitterAvg / 1000f))
    }

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .background(Color(0x90000000))) {
        val h = size.height
        val w = size.width
        val maxMs = 5f
        val scale = h / (maxMs + 0.5f)

        val colors = listOf(
            Color(0xFF00E676),
            Color(0xFFFFF176),
            Color(0xFFFDD835),
            Color(0xFFFBC02D),
            Color(0xFFF9A825),
            Color(0xFFF57F17),
            Color(0xFFDD2C00)
        )
        val lines = listOf(.5f, 1f, 1.5f, 2f, 3f, 4f, 5f)
        drawIntoCanvas { c ->
            lines.forEachIndexed { i, line ->
                val y = h - scale * line
                drawLine(
                    color = colors[i],
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 2f
                )
            }
            // White points (jitter)
            points.forEach {
                drawCircle(Color.White, 2f, Offset(it.first * 3f, h - it.second * scale))
            }
            // Blue points (avg)
            avgPoints.forEach {
                drawCircle(Color(0xFF2196F3), 2f, Offset(it.first * 3f, h - it.second * scale))
            }
        }
    }
}

@Composable
fun animatedBackground(): Color {
    val infiniteTransition = rememberInfiniteTransition()
    val color by infiniteTransition.animateColor(
        initialValue = Color(0xFF18FFFF),
        targetValue = Color(0xFF40C4FF),
        animationSpec = infiniteRepeatable(
            tween(1400, easing = LinearEasing),
            RepeatMode.Reverse
        )
    )
    return color
}

data class BrushBackground(val color: Color)
