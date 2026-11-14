package app.blow.location

import app.blow.geo.LatLng
import kotlinx.coroutines.flow.Flow

data class Fix(
    val position: LatLng,
    val headingDeg: Double? = null,
    /** 내비가 알려주는 현재 주소/도로명 (있으면 화면에 표시) */
    val address: String? = null,
)

interface LocationSource {
    val label: String
    fun fixes(): Flow<Fix>
}
