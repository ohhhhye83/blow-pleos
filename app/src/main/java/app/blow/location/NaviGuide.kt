package app.blow.location

/**
 * Pleos 내비가 안내 중인 내용의 요약 — Blow 화면의 내비 카드에 그대로 옮겨 보여 준다.
 * 길안내가 없으면 null.
 */
data class NaviGuide(
    /** 다음 회전 방향. */
    val turn: Turn,
    val turnDistanceMeters: Double,
    /** 예: "대왕판교IC 방면" */
    val turnDescription: String,
    val destination: String?,
    val remainMeters: Int?,
    val remainSeconds: Int?,
) {
    enum class Turn(val glyph: String) { STRAIGHT("↑"), LEFT("↰"), RIGHT("↱"), UTURN("↶"), ARRIVE("⚑") }
}
