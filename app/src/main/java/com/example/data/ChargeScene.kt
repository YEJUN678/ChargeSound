package com.example.data

/** 사운드·빛·애니메이션·음성 안내를 한 번에 바꾸는 충전 장면 프리셋. */
data class ChargeScene(
    val id: String,
    val name: String,
    val description: String,
    val accentKey: String,
    val soundId: String,
    val animation: AnimationMode,
    val voiceTemplate: String,
    val glowStart: Long,
    val glowMid: Long,
    val glowEnd: Long
)

object ChargeScenes {
    val defaults = listOf(
        ChargeScene("cyber_night", "Cyber Night", "네온 시안과 리액터 사운드", "cyan", "cyber_chime", AnimationMode.BUILTIN_REACTOR, "충전을 시작합니다. 현재 (%)퍼센트입니다.", 0xFF22D3EEL, 0xFF4ADE80L, 0xFF8B5CF6L),
        ChargeScene("quiet_night", "조용한 밤", "차분한 라임과 짧은 안내", "lime", "gentle_ding", AnimationMode.BUILTIN_PLASMA, "조용히 충전을 시작합니다. 현재 (%)퍼센트입니다.", 0xFF4ADE80L, 0xFF86EFACL, 0xFF22D3EEL),
        ChargeScene("game_charge", "게임 충전", "보랏빛 펄스와 강한 전기음", "violet", "lightning_spark", AnimationMode.BUILTIN_LIGHTNING, "전원 연결. 배터리 (%)퍼센트.", 0xFF8B5CF6L, 0xFFEC4899L, 0xFF22D3EEL)
    )
}
