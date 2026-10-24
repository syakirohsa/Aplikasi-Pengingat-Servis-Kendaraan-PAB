package id.ac.stmi.pengingatserviskendaraanapp.model

data class ComponentRule(val intervalKm: Int?, val intervalMonths: Int?)
data class ComponentInfo(val category: String, val typicalRange: String, val note: String)

object ComponentConstants {
    val KOMP = linkedMapOf(
        "Oli Mesin" to ComponentRule(2000, 2),
        "Oli Gardan" to ComponentRule(8000, 12),
        "Filter Udara" to ComponentRule(10000, 12),
        "Busi" to ComponentRule(8000, 12),
        "Filter Bensin" to ComponentRule(10000, 24),
        "Kampas Rem Depan" to ComponentRule(10000, 12),
        "Kampas Rem Belakang" to ComponentRule(10000, 12),
        "Minyak Rem" to ComponentRule(null, 24),
        "Ban Depan" to ComponentRule(20000, 36),
        "Ban Belakang" to ComponentRule(20000, 36),
        "Aki" to ComponentRule(null, 24),
        "Rantai & Gir" to ComponentRule(20000, null),
        "V-Belt & Roller" to ComponentRule(20000, null),
        "Air Radiator" to ComponentRule(null, 24),
        "Oli Shockbreaker" to ComponentRule(20000, null)
    )

    val INFO = mapOf(
        "Oli Mesin" to ComponentInfo("Paling sering", "2.000–4.000 km atau 2–3 bulan", "Servis paling utama, diganti paling rutin."),
        "Oli Gardan" to ComponentInfo("Paling sering", "Sekitar 8.000 km, atau tiap 2–3 kali ganti oli mesin", "Khusus motor matic."),
        "Filter Udara" to ComponentInfo("Paling sering", "Dibersihkan tiap servis, diganti 8.000–12.000 km", ""),
        "Busi" to ComponentInfo("Paling sering", "Dicek tiap servis, diganti 8.000–12.000 km", "Tergantung jenis busi."),
        "Kampas Rem Depan" to ComponentInfo("Sering", "Dicek tiap servis, diganti saat sudah tipis (umumnya 15.000–25.000 km)", "Biasanya lebih cepat habis dari kampas belakang."),
        "Kampas Rem Belakang" to ComponentInfo("Sering", "Dicek tiap servis, diganti saat sudah tipis (umumnya 15.000–25.000 km)", ""),
        "V-Belt & Roller" to ComponentInfo("Sering", "Diganti sekitar 20.000–25.000 km", "Roller kadang perlu dicek lebih awal. Khusus motor matic."),
        "Rantai & Gir" to ComponentInfo("Sering", "Dibersihkan, dilumasi, dan disetel tiap servis", "Untuk motor bebek/sport."),
        "Filter Bensin" to ComponentInfo("Sering", "Diganti sekitar 10.000–20.000 km", ""),
        "Minyak Rem" to ComponentInfo("Jarang", "Sekitar tiap 1–2 tahun atau 20.000–40.000 km", ""),
        "Air Radiator" to ComponentInfo("Jarang", "Sekitar tiap 1–2 tahun", "Untuk motor berpendingin cairan."),
        "Oli Shockbreaker" to ComponentInfo("Jarang", "Sekitar 20.000–30.000 km, atau saat bocor/tidak empuk", ""),
        "Ban Depan" to ComponentInfo("Jarang", "Tergantung kondisi (alur menipis, retak), umumnya 20.000–30.000 km", ""),
        "Ban Belakang" to ComponentInfo("Jarang", "Tergantung kondisi (alur menipis, retak), umumnya 20.000–30.000 km", "Biasanya lebih cepat habis dari ban depan."),
        "Aki" to ComponentInfo("Jarang", "Sekitar tiap 1,5–3 tahun, atau saat starter mulai lemah", "")
    )

    val NAMES = KOMP.keys.toList()

    val COLORS = mapOf(
        "Hitam" to "#1F2937",
        "Putih" to "#F3F4F6",
        "Silver" to "#CBD5E1",
        "Abu-abu" to "#6B7280",
        "Merah" to "#DC2626",
        "Oranye" to "#EA580C",
        "Kuning" to "#EAB308",
        "Hijau" to "#16A34A",
        "Biru" to "#2563EB",
        "Cokelat" to "#78350F"
    )
}