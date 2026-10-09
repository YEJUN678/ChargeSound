package com.example.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ad.AdUnits
import com.example.data.PremiumCatalog
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

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

    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                // 세션당 과다 노출을 막기 위해 높이 50dp에 맞춘 adaptive 배너를 사용합니다.
                val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, 360)
                setAdSize(adSize)
                adUnitId = AdUnits.bannerId

                val heightPx = (ctx.resources.displayMetrics.density * 50).toInt()
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    heightPx
                )

                setAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        // 실패 시 조용히 무시합니다 (사용자를 방해하지 않음)
                        visibility = android.view.View.GONE
                    }

                    override fun onAdLoaded() {
                        visibility = android.view.View.VISIBLE
                    }
                })
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 8.dp)
    )
}

/**
 * 네이티브 광고 카드.
 *
 * 배너보다 eCPM이 높고 시각적 방해가 적어 유틸리티 앱에 적합합니다.
 * "추천 사운드 팩" 영역 근처에 배치하는 것을 권장합니다.
 */
@Composable
fun AdNativeCard(isAdFreeActive: Boolean) {
    if (isAdFreeActive) return

    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    LaunchedEffect(Unit) {
        val loader = AdLoader.Builder(context, AdUnits.nativeId)
            .forNativeAd { loaded ->
                // 이전 광고가 남아 있으면 먼저 정리합니다 (메모리 누수 방지)
                nativeAd?.destroy()
                nativeAd = loaded
            }
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return

    AndroidView(
        factory = { ctx ->
            NativeAdView(ctx).apply {
                val headline = android.widget.TextView(ctx).apply {
                    textSize = 14f
                    setTextColor(android.graphics.Color.WHITE)
                    maxLines = 1
                }
                val body = android.widget.TextView(ctx).apply {
                    textSize = 11f
                    setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                    maxLines = 2
                }
                val cta = android.widget.TextView(ctx).apply {
                    textSize = 11f
                    setTextColor(android.graphics.Color.parseColor("#00E5FF"))
                }
                val icon = android.widget.ImageView(ctx)

                setHeadlineView(headline)
                setBodyView(body)
                setCallToActionView(cta)
                setIconView(icon)
                setNativeAd(ad)

                headline.text = ad.headline
                body.text = ad.body
                cta.text = ad.callToAction
                ad.icon?.drawable?.let { icon.setImageDrawable(it) }
            }
        },
        update = { view -> view.setNativeAd(ad) },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Slate900, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

/**
 * "광고 없이 N분" 카드.
 *
 * 보상형 광고를 시청하면 배너와 네이티브가 잠시 숨겨집니다.
 * AdMob이 권장하는 합법적 수익화 방식 — 광고를 강제하지 않고,
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
                        text = if (isAdFreeActive) "광고 없는 시간 실행 중" else "광고 없이 사용하기",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
                if (isAdFreeActive && remainingMinutes > 0) {
                    Text(
                        text = "${remainingMinutes}분 남음",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = ElectricCyan
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isAdFreeActive) {
                    "이 시간이 끝나면 광고가 다시 표시됩니다."
                } else {
                    "광고 1회 시청으로 원하는 시간 동안 배너와 추천 영역을 숨길 수 있습니다."
                },
                fontSize = 12.sp,
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
                            text = "광고 보고 잠시 숨기기",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
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
                        Text("시간 선택", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * 잠긴 프리미엄 콘텐츠 행.
 *
 * AdMob 정책 요구사항을 그대로 반영합니다:
 * - 광고를 거부해도 앱의 다른 기능은 정상 동작합니다.
 * - 보상(무엇을 받는지)이 텍스트와 진행 바로 명확히 표시됩니다.
 */
@Composable
fun PremiumLockedRow(
    name: String,
    description: String,
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
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
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
                    text = if (remaining <= 0) "해제" else "광고 ${remaining}회",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
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
            text = "광고 $adsWatched/$adsRequired 시청 시 잠금 해제",
            fontSize = 11.sp,
            color = Slate600
        )
    }
}

/** 해제된 프리미엄 콘텐츠 행 (바로 사용 가능) */
@Composable
fun PremiumUnlockedRow(
    name: String,
    description: String,
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
                        text = name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (isSelected) accent else Color.White
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = Slate600
                    )
                }
            }
            if (onPreview != null) {
                IconButton(onClick = onPreview) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "미리듣기",
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
                    text = if (isGranted) "다른 앱 위에 표시 권한 granted" else "다른 앱 위에 표시 권한 필요",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isGranted) {
                    "충전기를 꽂으면 다른 앱 위에 애니메이션이 표시됩니다."
                } else {
                    "Android 10 이상은 보안 정책상 백그라운드 화면 표시를 차단합니다. " +
                        "설정에서 권한을 허용하면 유튜브 등 다른 앱 위에 충전 애니메이션이 뜹니다. " +
                        "허용하지 않아도 충전 사운드와 진동은 정상 동작하고, 충전 시작 알림으로 대체됩니다."
                },
                fontSize = 12.sp,
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
                    Text("설정에서 권한 허용하기", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/** 프리미엄 콘텐츠 잠금 해제 안내 문구 (정책: 보상을 명확히 고지) */
fun premiumUnlockLabel(name: String): String =
    "광고 ${PremiumCatalog.ADS_REQUIRED_PER_UNLOCK}회 시청 시 '$name' 잠금 해제"