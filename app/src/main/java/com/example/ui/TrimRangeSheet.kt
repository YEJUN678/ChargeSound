package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MediaTrim
import com.example.data.TrimRange
import com.example.ui.theme.AppShapes
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate600
import com.example.ui.theme.Spacing
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import kotlinx.coroutines.delay
import java.io.File

/**
 * 커스텀 오디오 / 영상 구간 선택 시트.
 *
 * ## 파일을 실제로 자르지 않는 이유
 * 재인코딩하면 화질이 떨어지거나 포맷이 깨지고, 큰 영상은 시간과 임시 공간이 듭니다.
 * 여기서는 **재생할 때 시작점으로 건너뛰고 끝점에서 멈추기만** 합니다.
 * 어떤 포맷에서도 동작하고, 설정에는 숫자 몇 개만 저장됩니다.
 * 자세한 근거는 [TrimRange] 를 참고하세요.
 */
@Composable
fun TrimRangeSheet(
    path: String,
    initialStartMs: Long,
    initialEndMs: Long,
    knownTotalMs: Long = 0L,
    titleRes: Int,
    onDismiss: () -> Unit,
    onApply: (startMs: Long, endMs: Long) -> Unit
) {
    val context = LocalContext.current

    var totalMs by remember(path) { mutableLongStateOf(knownTotalMs) }
    var startMs by remember(path) { mutableLongStateOf(initialStartMs) }
    var endMs by remember(path) { mutableLongStateOf(initialEndMs) }
    var isPlaying by remember { mutableStateOf(false) }
    var playheadMs by remember { mutableLongStateOf(0L) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(path, knownTotalMs) {
        if (knownTotalMs > 0L) {
            totalMs = knownTotalMs
            loadFailed = false
        } else {
            val t = MediaTrim.readTotalMs(context, path)
            totalMs = t
            loadFailed = t <= 0L
        }
    }

    val range = remember(totalMs, startMs, endMs) { TrimRange(totalMs, startMs, endMs) }

    val player = remember(path) { MediaPlayer() }
    DisposableEffect(player) {
        onDispose {
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
    }

    fun previewFromStart() {
        runCatching {
            preparePreview(context, player, path, startMs.toInt())
            player.start()
        }.onSuccess {
            playheadMs = startMs
            isPlaying = true
        }.onFailure {
            loadFailed = true
            isPlaying = false
        }
    }

    // 재생 위치 추적 + 끝점 자동 정지
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val pos = runCatching { player.currentPosition.toLong() }.getOrDefault(startMs)
            playheadMs = pos
            val stopAt = if (range.endMs > 0L) range.endMs else totalMs
            if (stopAt > 0L && pos >= stopAt) {
                runCatching { player.pause() }
                runCatching { player.seekTo(startMs.toInt()) }
                playheadMs = startMs
                isPlaying = false
            }
            delay(50)
        }
    }

    SheetScaffold(titleRes = titleRes, onDismiss = onDismiss) {
        if (loadFailed) {
            Text(
                text = stringResource(R.string.trim_read_error),
                style = MaterialTheme.typography.bodyMedium,
                color = Slate600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.xl))
            TrimApplyRow(
                enabled = true,
                resetLabel = stringResource(R.string.btn_cancel),
                applyLabel = stringResource(R.string.btn_confirm),
                onReset = onDismiss,
                onApply = { onApply(0L, 0L); onDismiss() }
            )
            return@SheetScaffold
        }

        Text(
            text = stringResource(R.string.trim_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Slate600,
            lineHeight = 16.sp
        )

        Spacer(Modifier.height(Spacing.lg))

        TrimTimeline(
            totalMs = totalMs,
            startMs = startMs,
            endMs = if (endMs > 0L) endMs else totalMs,
            playheadMs = playheadMs,
            onScrub = { runCatching { player.pause() }; isPlaying = false },
            onStartChange = { startMs = it },
            onEndChange = { endMs = it }
        )

        Spacer(Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrimTimeChip(
                label = stringResource(R.string.trim_from),
                value = MediaTrim.formatMs(startMs),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "→",
                color = Slate700,
                style = MaterialTheme.typography.titleSmall
            )
            TrimTimeChip(
                label = stringResource(R.string.trim_to),
                value = MediaTrim.formatMs(if (endMs > 0L) endMs else totalMs),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.trim_length, MediaTrim.formatMs(range.lengthMs)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = ElectricCyan
            )
            Text(
                text = stringResource(R.string.trim_total, MediaTrim.formatMs(totalMs)),
                style = MaterialTheme.typography.bodySmall,
                color = Slate600
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            TrimIconButton(
                icon = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.trim_play),
                enabled = totalMs > 0L,
                onClick = { previewFromStart() }
            )
            TrimIconButton(
                icon = Icons.Default.Pause,
                contentDescription = stringResource(R.string.trim_pause),
                enabled = isPlaying,
                onClick = {
                    runCatching { player.pause() }
                    isPlaying = false
                }
            )
            TrimIconButton(
                icon = Icons.Default.VolumeUp,
                contentDescription = stringResource(R.string.trim_preview_start),
                enabled = totalMs > 0L,
                onClick = {
                    runCatching { player.pause() }
                    playheadMs = startMs
                    isPlaying = false
                }
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        if (!range.isFull) {
            Text(
                text = stringResource(R.string.trim_active_note),
                style = MaterialTheme.typography.bodySmall,
                color = Slate600,
                lineHeight = 15.sp
            )
            Spacer(Modifier.height(Spacing.sm))
        }

        TrimApplyRow(
            enabled = true,
            resetLabel = stringResource(R.string.trim_reset),
            applyLabel = stringResource(R.string.trim_apply),
            onReset = {
                runCatching { player.pause() }
                isPlaying = false
                startMs = 0L
                endMs = 0L
                playheadMs = 0L
            },
            onApply = {
                runCatching { player.pause() }
                isPlaying = false
                val s = MediaTrim.sanitize(range)
                onApply(s.startMs, s.endMs)
                onDismiss()
            }
        )
    }
}

private fun preparePreview(context: Context, player: MediaPlayer, path: String, startMs: Int) {
    player.reset()
    if (path.startsWith("content://")) {
        player.setDataSource(context, Uri.parse(path))
    } else if (File(path).exists()) {
        player.setDataSource(path)
    } else {
        error("no such file: $path")
    }
    player.prepare()
    player.seekTo(startMs)
}

/**
 * 드래그로 시작/끝 지점을 고르는 타임라인.
 *
 * 손잡이는 몸통(선택 구간)보다 높은 곳에 솟아 있어 "여기를 잡아 끌 수 있다"가
 * 모양만으로도 읽힙니다.
 */
@Composable
private fun TrimTimeline(
    totalMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    onScrub: () -> Unit,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit
) {
    val trackHeight = 60.dp
    val handleWidth = 4.dp
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(0) } // 0 없음, 1 시작, 2 끝

    fun pxToMs(px: Float): Long {
        if (trackWidthPx <= 0f || totalMs <= 0L) return 0L
        return ((px / trackWidthPx) * totalMs).toLong().coerceIn(0L, totalMs)
    }

    val startPx = if (totalMs > 0L) (startMs.toFloat() / totalMs) * trackWidthPx else 0f
    val endPx = if (totalMs > 0L) (endMs.toFloat() / totalMs) * trackWidthPx else trackWidthPx
    val headPx = if (totalMs > 0L) (playheadMs.toFloat() / totalMs) * trackWidthPx else 0f
    val selectedWidthPx = (endPx - startPx).coerceAtLeast(0f)

    // 손잡이 판정 반경 (dp → px 변환은 density 가 필요하므로 비율로 대신합니다)
    val grabRadiusPx = trackWidthPx * 0.08f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(AppShapes.sm)
            .background(Slate800)
            .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(totalMs) {
                detectTapGestures { offset ->
                    if (totalMs <= 0L) return@detectTapGestures
                    onScrub()
                    val tapMs = pxToMs(offset.x)
                    val toStart = kotlin.math.abs(offset.x - startPx)
                    val toEnd = kotlin.math.abs(offset.x - endPx)
                    when {
                        toStart <= grabRadiusPx || (toStart < toEnd) -> onStartChange(tapMs)
                        else -> onEndChange(tapMs)
                    }
                }
            }
            .pointerInput(totalMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (totalMs <= 0L) return@detectHorizontalDragGestures
                        onScrub()
                        val toStart = kotlin.math.abs(offset.x - startPx)
                        val toEnd = kotlin.math.abs(offset.x - endPx)
                        dragging = when {
                            toStart <= grabRadiusPx -> 1
                            toEnd <= grabRadiusPx -> 2
                            offset.x < startPx -> 1
                            else -> 2
                        }
                    },
                    onDragEnd = { dragging = 0 },
                    onDragCancel = { dragging = 0 }
                ) { change, delta ->
                    if (totalMs <= 0L) return@detectHorizontalDragGestures
                    if (dragging == 0) return@detectHorizontalDragGestures
                    change.consume()
                    val current = if (dragging == 1) startPx + delta else endPx + delta
                    val newMs = pxToMs(current)
                    if (dragging == 1) onStartChange(newMs.coerceAtMost(endMs))
                    else onEndChange(newMs.coerceAtLeast(startMs))
                }
            }
    ) {
        // 선택 구간 강조
        Box(
            modifier = Modifier
                .offset(x = startPx.dp)
                .width(with(androidx.compose.ui.platform.LocalDensity.current) { selectedWidthPx.toDp() })
                .fillMaxHeight()
                .background(ElectricCyan.copy(alpha = 0.22f))
        )
        // 위/아래 경계선 — 선택 구간이 어디부터 어디까지인지 명확하게
        Box(
            modifier = Modifier
                .offset(x = startPx.dp)
                .width(with(androidx.compose.ui.platform.LocalDensity.current) { selectedWidthPx.toDp() })
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(ElectricCyan)
        )
        Box(
            modifier = Modifier
                .offset(x = startPx.dp)
                .width(with(androidx.compose.ui.platform.LocalDensity.current) { selectedWidthPx.toDp() })
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .background(ElectricCyan)
        )
        // 재생 헤드
        if (playheadMs in startMs..endMs) {
            Box(
                modifier = Modifier
                    .offset(x = (headPx - 1f).dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }
        // 손잡이 두 개 (몸통 + 솟은 머리)
        TrimHandle(startPx, dragging == 1)
        TrimHandle(endPx, dragging == 2)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.TrimHandle(xPx: Float, active: Boolean) {
    val w = if (active) 6.dp else 4.dp
    Box(
        modifier = Modifier
            .offset(x = (xPx - w.value / 2f).dp)  // xPx 는 픽셀, w 는 dp -> 값을 맞춰야 뺄셈이 됩니다
            .width(w)
            .height(14.dp)
            .align(Alignment.TopCenter)
            .clip(AppShapes.xs)
            .background(ElectricCyan)
    )
}

@Composable
private fun TrimTimeChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate600)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun TrimIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(AppShapes.md)
            .background(Slate800)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Slate700,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TrimApplyRow(
    enabled: Boolean,
    resetLabel: String,
    applyLabel: String,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(AppShapes.md)
                .background(Slate800)
                .clickable(enabled = enabled) { onReset() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = resetLabel, style = MaterialTheme.typography.labelLarge, color = Slate600)
        }
        Box(
            modifier = Modifier
                .weight(2f)
                .height(52.dp)
                .clip(AppShapes.md)
                .background(if (enabled) ElectricCyan else Slate800)
                .clickable(enabled = enabled) { onApply() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = applyLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.Black else Slate700
            )
        }
    }
}