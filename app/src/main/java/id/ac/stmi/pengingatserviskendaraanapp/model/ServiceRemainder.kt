package id.ac.stmi.pengingatserviskendaraanapp.model

enum class RemainderStatus {
    OK, SOON, LATE
}

data class ServiceRemainder(
    val component: String,
    val record: ServiceRecord?,
    val nextKm: Int?,
    val nextDate: String?,
    val sisaKm: Int?,
    val sisaHari: Int?,
    val status: RemainderStatus,
    val percentage: Int,
    val isEnabled: Boolean,
    val isNone: Boolean
)