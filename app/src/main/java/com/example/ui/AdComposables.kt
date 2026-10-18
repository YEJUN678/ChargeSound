package com.example.ui

import android.app.Activity
import android.content.ContextWrapper
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.ad.AdManager
import com.example.ad.AdUnits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.annotation.StringRes
import com.example.R
import com.example.ui.theme.Slate400
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.PremiumCatalog
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

/**
 * 배너 광고 (Adaptive).
 *
 * 정책 및 UX 고려:
 * - 세션당 노출 수를 억제하기 위해 60초 높이 고정 쿨다운을 사용합니다.
 * - "광고 없는 시간"이 활성화되어 있으면 아예 렌더링하지 않습니다.
 * - 충전 애니메이션 화면에는 이 컴포넌트가 존재하지 않습니다(별도 Activity).
 */
@Composable
fun AdBannerSlot(
    isAdFreeActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (isAdFreeActive) return

    val activity = LocalContext.current.findActivity() ?: return
    // 미설정(앱 키/단위 ID 없음)이면 광고 영역 자체를 그리지 않습니다.
    val adView = remember(activity) {
        AdManager.createBannerView(activity, AdUnits.BANNER_PLACEMENT)
    } ?: return

    AndroidView(
        factory = { adView },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 8.dp)
    )
}

/**
 * 스폰서 카드.
 *
 * 배너보다 eCPM이 높고 시각적 방해가 적어 유틸리티 앱에 적합합니다.
 * "추천 사운드 팩" 영역 근처에 배치하는 것을 권장합니다.
 */
@Composable
fun AdNativeCard(isAdFreeActive: Boolean) {
    if (isAdFreeActive) return

    val activity = LocalContext.current.findActivity() ?: return
    val adView = remember(activity) { AdManager.createSponsoredCardView(activity) } ?: return

    DisposableEffect(adView) {
        AdManager.loadBanner(adView)
        onDispose { AdManager.destroyBanner(adView) }
    }

    AndroidView(
        factory = { adView },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Slate900, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

/** Compose Context에서 Activity를 찾습니다. ironSource 배너는 Activity가 필요합니다. */
internal fun android.content.Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * "광고 없이 N분" 카드.
 *
 * 보상형 광고를 시청하면 배너와 네이티브가 잠시 숨겨집니다.
 * 광고 네트워크가 권장하는 합법적 수익화 방식 — 광고를 강제하지 않고,
 * 사용자가 스스로 시간을 구매합니다.
 */
@Composable
fun AdFreeCard(
    remainingMinutes: Int,
    isAdFreeActive: Boolean,
    onWatchAd: () -> Unit,
    onChooseDuration: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (isAdFreeActive) ElectricCyan.copy(alpha = 0.6f) else ElectricAmber.copy(alpha = 0.35f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAdFreeActive) Icons.Default.PlayArrow else Icons.Default.Block,
                        contentDescription = null,
                        tint = if (isAdFreeActive) ElectricCyan else ElectricAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isAdFreeActive) {
                            stringResource(R.string.adfree_card_active_title)
                        } else {
                            stringResource(R.string.adfree_card_title)
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                if (isAdFreeActive && remainingMinutes > 0) {
                    Text(
                        text = stringResource(R.string.duration_minutes, remainingMinutes),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isAdFreeActive) {
                    stringResource(R.string.adfree_card_active_desc)
                } else {
                    stringResource(R.string.adfree_card_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = Slate600
            )

            if (!isAdFreeActive) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onWatchAd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.adfree_btn_hide),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(
                        onClick = onChooseDuration,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate700,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.adfree_btn_choose), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/**
 * 잠긴 프리미엄 콘텐츠 행.
 *
 * 광고 정책 요구사항을 그대로 반영합니다:
 * - 광고를 거부해도 앱의 다른 기능은 정상 동작합니다.
 * - 보상(무엇을 받는지)이 텍스트와 진행 바로 명확히 표시됩니다.
 */
@Composable
fun PremiumLockedRow(
    @StringRes nameRes: Int,
    @StringRes descriptionRes: Int,
    accent: Color,
    adsWatched: Int,
    adsRequired: Int,
    onWatchAd: () -> Unit
) {
    val remaining = (adsRequired - adsWatched).coerceAtLeast(0)
    val progress = if (adsRequired > 0) adsWatched.toFloat() / adsRequired else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(nameRes),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(descriptionRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600
                    )
                }
            }
            Button(
                onClick = onWatchAd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.copy(alpha = 0.18f),
                    contentColor = accent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = if (remaining <= 0) {
                        stringResource(R.string.premium_btn_unlock)
                    } else {
                        stringResource(R.string.premium_btn_ads_left, remaining)
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            color = accent,
            trackColor = Slate800
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.premium_progress, adsWatched, adsRequired),
            style = MaterialTheme.typography.labelSmall,
            color = Slate600
        )
    }
}

/** 해제된 프리미엄 콘텐츠 행 (바로 사용 가능) */
@Composable
fun PremiumUnlockedRow(
    @StringRes nameRes: Int,
    @StringRes descriptionRes: Int,
    accent: Color,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPreview: (() -> Unit)? = null
) {
    Surface(
        color = if (isSelected) accent.copy(alpha = 0.12f) else Slate800,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(1.dp, accent) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isSelected) accent else Slate600,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(nameRes),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isSelected) accent else Color.White
                    )
                    Text(
                        text = stringResource(descriptionRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600
                    )
                }
            }
            if (onPreview != null) {
                IconButton(onClick = onPreview) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.a11y_play),
                        tint = if (isSelected) accent else Slate600
                    )
                }
            }
        }
    }
}

/**
 * 오버레이 권한 안내 카드.
 *
 * Android 10+ 는 백그라운드 액티비티 시작을 차단하므로, 이 권한 없이는
 * 다른 앱 위에 충전 애니메이션을 띄울 수 없습니다.
 *
 * Play 정책: 사용자가 권한을 거부해도 앱은 정상 동작해야 합니다.
 * 이 카드는 거부 시 대체 동작(알림)을 명확히 안내합니다.
 */
@Composable
fun OverlayPermissionCard(
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (isGranted) ElectricCyan.copy(alpha = 0.5f) else ElectricAmber.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isGranted) ElectricCyan else ElectricAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isGranted) {
                        stringResource(R.string.overlay_title_granted)
                    } else {
                        stringResource(R.string.overlay_title_needed)
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isGranted) {
                    stringResource(R.string.overlay_desc_granted)
                } else {
                    stringResource(R.string.overlay_desc_needed)
                },
                style = MaterialTheme.typography.bodySmall,
                color = Slate600
            )

            if (!isGranted) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.overlay_btn_grant),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * 커스텀 오디오/영상 적용 후 표시하는 보상형 광고 "제안" 카드.
 *
 * 정책 준수:
 * - 광고를 거부해도 아무 일도 일어나지 않습니다 (기능에 영향 없음).
 * - 보상(무료 잠금 해제 크레딧)이 무엇인지 명확히 고지합니다.
 * - 강제 표시가 아니므로 사용자가 직접 선택해야 합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeUnlockOfferDialog(
    appliedLabel: String,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Slate900,
            border = BorderStroke(1.dp, ElectricAmber.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = ElectricAmber,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.offer_title, appliedLabel),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.offer_desc),
                    color = Slate600,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onWatchAd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.offer_btn),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.offer_later),
                    color = Slate400,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}
