package id.ac.stmi.pengingatserviskendaraanapp.model

data class Vehicle(
    var id: Int,
    var name: String,
    var type: String,
    var brand: String,
    var year: String,
    var plate: String,
    var color: String,
    var currentKm: Int
)
