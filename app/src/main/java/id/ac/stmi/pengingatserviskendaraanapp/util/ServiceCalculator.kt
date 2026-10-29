package id.ac.stmi.pengingatserviskendaraanapp.util

import id.ac.stmi.pengingatserviskendaraanapp.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

object ServiceCalculator {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("d MMM yyyy", Locale.Builder().setLanguage("id").setRegion("ID").build())

    fun getTodayIso(): String {
        return dateFormat.format(Date())
    }

    fun addMonthsIso(dateIso: String, months: Int): String {
        return try {
            val date = dateFormat.parse(dateIso) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.MONTH, months)
            }
            dateFormat.format(cal.time)
        } catch (_: Exception) {
            dateIso
        }
    }

    fun formatNumber(num: Int): String {
        return String.format(Locale.Builder().setLanguage("id").setRegion("ID").build(), "%,d", num).replace(',', '.')
    }

    fun formatDmy(dateIso: String): String {
        return try {
            val date = dateFormat.parse(dateIso) ?: return dateIso
            displayFormat.format(date)
        } catch (_: Exception) {
            dateIso
        }
    }

    fun calculateSchedule(record: ServiceRecord): Pair<Int?, String?> {
        val rule = ComponentConstants.KOMP[record.component] ?: return Pair(null, null)
        val nk = rule.intervalKm?.let { record.odometer + it }
        val nd = rule.intervalMonths?.let { addMonthsIso(record.date, it) }
        return Pair(nk, nd)
    }

    private fun getEffectiveDays(r: ServiceRemainder): Double {
        if (r.isNone) return Double.MAX_VALUE

        val daysFromDate = r.sisaHari?.toDouble()
        val daysFromKm = r.sisaKm?.let { it.toDouble() / 30.0 }

        return when {
            daysFromDate != null && daysFromKm != null -> minOf(daysFromDate, daysFromKm)
            daysFromDate != null -> daysFromDate
            daysFromKm != null -> daysFromKm
            else -> Double.MAX_VALUE
        }
    }

    fun calculateReminders(
        vehicle: Vehicle,
        records: List<ServiceRecord>,
        disabledReminders: Set<String>
    ): List<ServiceRemainder> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val out = mutableListOf<ServiceRemainder>()

        for (j in ComponentConstants.NAMES) {
            val rule = ComponentConstants.KOMP[j] ?: continue
            val compRecords = records.filter { it.vehicleId == vehicle.id && it.component == j }
                .sortedWith(compareByDescending<ServiceRecord> { it.date }.thenByDescending { it.odometer })

            val isOff = disabledReminders.contains(j)

            if (compRecords.isEmpty()) {
                out.add(
                    ServiceRemainder(
                        component = j,
                        record = null,
                        nextKm = null,
                        nextDate = null,
                        sisaKm = null,
                        sisaHari = null,
                        status = RemainderStatus.OK,
                        percentage = 0,
                        isEnabled = !isOff,
                        isNone = true
                    )
                )
                continue
            }

            val i = compRecords[0]
            val (nk, nd) = calculateSchedule(i)
            val sk = nk?.let { it - vehicle.currentKm }

            val sh = nd?.let {
                try {
                    val d = dateFormat.parse(it)?.time ?: today
                    val diff = d - today
                    (diff / (86400000.0)).toInt()
                } catch (_: Exception) {
                    null
                }
            }

            val late = (sk != null && sk <= 0) || (sh != null && sh <= 0)
            val soon = (sk != null && sk <= 500) || (sh != null && sh <= 14)

            val pk = if (sk != null && rule.intervalKm != null && rule.intervalKm > 0) {
                1.0 - (sk.toDouble() / rule.intervalKm)
            } else 0.0

            val pw = if (sh != null && rule.intervalMonths != null && rule.intervalMonths > 0) {
                1.0 - (sh.toDouble() / (rule.intervalMonths * 30))
            } else 0.0

            val st = if (late) RemainderStatus.LATE else if (soon) RemainderStatus.SOON else RemainderStatus.OK
            val pct = max(0, min(100, (max(pk, pw) * 100).toInt()))

            out.add(
                ServiceRemainder(
                    component = j,
                    record = i,
                    nextKm = nk,
                    nextDate = nd,
                    sisaKm = sk,
                    sisaHari = sh,
                    status = st,
                    percentage = pct,
                    isEnabled = !isOff,
                    isNone = false
                )
            )
        }

        return out.sortedWith(
            compareBy<ServiceRemainder> { if (it.isNone) 2 else if (!it.isEnabled) 1 else 0 }
                .thenBy { getEffectiveDays(it) }
                .thenBy { it.nextDate ?: "9999-12-31" }
                .thenBy { it.sisaKm ?: Int.MAX_VALUE }
        )
    }

    fun formatRemainingText(r: ServiceRemainder): String {
        val parts = mutableListOf<String>()
        if (r.sisaKm != null) {
            if (r.sisaKm > 0) {
                parts.add("Sisa ${formatNumber(r.sisaKm)} km")
            } else {
                parts.add("Lewat ${formatNumber(-r.sisaKm)} km")
            }
        }
        if (r.sisaHari != null) {
            if (r.sisaHari > 0) {
                parts.add("${r.sisaHari} hari lagi")
            } else {
                parts.add("Lewat ${-r.sisaHari} hari")
            }
        }
        return parts.joinToString(" · ")
    }

    fun formatScheduleText(r: ServiceRemainder): String {
        val parts = mutableListOf<String>()
        if (r.nextKm != null) {
            parts.add("${formatNumber(r.nextKm)} km")
        }
        if (!r.nextDate.isNullOrEmpty()) {
            parts.add(formatDmy(r.nextDate!!))
        }
        return parts.joinToString(" atau ")
    }

    fun formatIntervalText(component: String): String {
        val rule = ComponentConstants.KOMP[component] ?: return "Sesuai kondisi"
        val parts = mutableListOf<String>()
        if (rule.intervalKm != null) {
            parts.add("${formatNumber(rule.intervalKm)} km")
        }
        if (rule.intervalMonths != null) {
            val monthText = if (rule.intervalMonths % 12 == 0) {
                "${rule.intervalMonths / 12} tahun"
            } else {
                "${rule.intervalMonths} bulan"
            }
            parts.add(monthText)
        }
        return if (parts.isNotEmpty()) "Setiap ${parts.joinToString(" atau ")}" else "Sesuai kondisi"
    }
}
