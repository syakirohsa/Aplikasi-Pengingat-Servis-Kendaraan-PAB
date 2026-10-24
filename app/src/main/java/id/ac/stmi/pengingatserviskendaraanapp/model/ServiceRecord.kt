package id.ac.stmi.pengingatserviskendaraanapp.model

data class ServiceRecord(
    var id: Int,
    var vehicleId: Int,
    var component: String,
    var date: String, // YYYY-MM-DD
    var odometer: Int,
    var notes: String
)
