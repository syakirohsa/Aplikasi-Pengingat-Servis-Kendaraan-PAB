package id.ac.stmi.pengingatserviskendaraanapp.model

data class Account(
    var email: String,
    var password: String,
    var name: String,
    var avatarUri: String? = null
)
