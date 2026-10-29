package id.ac.stmi.pengingatserviskendaraanapp.data

import android.content.Context
import android.content.SharedPreferences
import id.ac.stmi.pengingatserviskendaraanapp.model.*
import id.ac.stmi.pengingatserviskendaraanapp.util.ServiceCalculator
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class ServisRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("servisku_prefs", Context.MODE_PRIVATE)

    var currentAccountEmail: String?
        get() = prefs.getString("current_email", null)
        set(value) {
            prefs.edit().putString("current_email", value).apply()
        }

    var globalNotificationsEnabled: Boolean
        get() = prefs.getBoolean("global_notif", true)
        set(value) {
            prefs.edit().putBoolean("global_notif", value).apply()
        }

    init {
        if (!prefs.getBoolean("is_initialized", false)) {
            seedDefaultData()
            prefs.edit().putBoolean("is_initialized", true).apply()
        }
    }

    private fun seedDefaultData() {
        val demoAcc = Account("omarbaskara@email.com", "123456", "Omar Baskara", null)
        saveAccount(demoAcc)

        val demoVehicle = Vehicle(
            id = 1,
            name = "Marquez",
            type = "Vario 125",
            brand = "Honda",
            year = "2021",
            plate = "B 1234 XYZ",
            color = "Merah",
            currentKm = 13200
        )
        saveVehicle(demoAcc.email, demoVehicle)
        setSelectedVehicleId(demoAcc.email, demoVehicle.id)

        val cal = Calendar.getInstance()
        val todayIso = ServiceCalculator.getTodayIso()

        // 30 days ago
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val d1 = ServiceCalculator.getTodayIso() // or calculated
        val r1 = ServiceRecord(1, 1, "Oli Mesin", ServiceCalculator.addMonthsIso(todayIso, -1), 12000, "Servis di AHASS")
        val r2 = ServiceRecord(2, 1, "Kampas Rem Depan", ServiceCalculator.addMonthsIso(todayIso, -5), 9500, "")
        val r3 = ServiceRecord(3, 1, "Aki", ServiceCalculator.addMonthsIso(todayIso, -8), 8000, "")
        val r4 = ServiceRecord(4, 1, "Busi", ServiceCalculator.addMonthsIso(todayIso, -3), 11000, "")

        val records = listOf(r1, r2, r3, r4)
        saveServiceRecords(demoAcc.email, records)
        prefs.edit().putInt("next_record_id", 5).putInt("next_vehicle_id", 2).apply()
    }

    // ACCOUNTS
    fun getAccounts(): List<Account> {
        val jsonStr = prefs.getString("accounts_json", "[]") ?: "[]"
        val list = mutableListOf<Account>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Account(
                        email = obj.getString("email"),
                        password = obj.getString("password"),
                        name = obj.getString("name"),
                        avatarUri = if (obj.has("avatarUri") && !obj.isNull("avatarUri")) obj.getString("avatarUri") else null
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun getAccount(email: String): Account? {
        return getAccounts().find { it.email.equals(email, ignoreCase = true) }
    }

    fun saveAccount(account: Account) {
        val accounts = getAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.email.equals(account.email, ignoreCase = true) }
        if (index >= 0) {
            accounts[index] = account
        } else {
            accounts.add(account)
        }
        val arr = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject().apply {
                put("email", acc.email)
                put("password", acc.password)
                put("name", acc.name)
                put("avatarUri", acc.avatarUri ?: JSONObject.NULL)
            }
            arr.put(obj)
        }
        prefs.edit().putString("accounts_json", arr.toString()).apply()
    }

    // VEHICLES
    fun getVehicles(userEmail: String): List<Vehicle> {
        val jsonStr = prefs.getString("vehicles_json_$userEmail", "[]") ?: "[]"
        val list = mutableListOf<Vehicle>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Vehicle(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        type = obj.getString("type"),
                        brand = obj.getString("brand"),
                        year = obj.optString("year", ""),
                        plate = obj.optString("plate", ""),
                        color = obj.optString("color", "Merah"),
                        currentKm = obj.getInt("currentKm")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveVehicle(userEmail: String, vehicle: Vehicle) {
        val list = getVehicles(userEmail).toMutableList()
        val index = list.indexOfFirst { it.id == vehicle.id }
        if (index >= 0) {
            list[index] = vehicle
        } else {
            list.add(vehicle)
        }
        saveVehicleList(userEmail, list)
    }

    private fun saveVehicleList(userEmail: String, list: List<Vehicle>) {
        val arr = JSONArray()
        for (v in list) {
            val obj = JSONObject().apply {
                put("id", v.id)
                put("name", v.name)
                put("type", v.type)
                put("brand", v.brand)
                put("year", v.year)
                put("plate", v.plate)
                put("color", v.color)
                put("currentKm", v.currentKm)
            }
            arr.put(obj)
        }
        prefs.edit().putString("vehicles_json_$userEmail", arr.toString()).apply()
    }

    fun generateVehicleId(): Int {
        val nextId = prefs.getInt("next_vehicle_id", 2)
        prefs.edit().putInt("next_vehicle_id", nextId + 1).apply()
        return nextId
    }

    fun getSelectedVehicleId(userEmail: String): Int {
        val vehicles = getVehicles(userEmail)
        if (vehicles.isEmpty()) return 0
        val savedId = prefs.getInt("selected_vehicle_$userEmail", -1)
        return if (vehicles.any { it.id == savedId }) savedId else vehicles[0].id
    }

    fun setSelectedVehicleId(userEmail: String, vehicleId: Int) {
        prefs.edit().putInt("selected_vehicle_$userEmail", vehicleId).apply()
    }

    // SERVICE RECORDS
    fun getServiceRecords(userEmail: String): List<ServiceRecord> {
        val jsonStr = prefs.getString("records_json_$userEmail", "[]") ?: "[]"
        val list = mutableListOf<ServiceRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ServiceRecord(
                        id = obj.getInt("id"),
                        vehicleId = obj.getInt("vehicleId"),
                        component = obj.getString("component"),
                        date = obj.getString("date"),
                        odometer = obj.getInt("odometer"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun addServiceRecord(userEmail: String, record: ServiceRecord) {
        val list = getServiceRecords(userEmail).toMutableList()
        list.add(0, record)
        saveServiceRecords(userEmail, list)
    }

    fun deleteServiceRecord(userEmail: String, recordId: Int) {
        val list = getServiceRecords(userEmail).filter { it.id != recordId }
        saveServiceRecords(userEmail, list)
    }

    private fun saveServiceRecords(userEmail: String, list: List<ServiceRecord>) {
        val arr = JSONArray()
        for (r in list) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("vehicleId", r.vehicleId)
                put("component", r.component)
                put("date", r.date)
                put("odometer", r.odometer)
                put("notes", r.notes)
            }
            arr.put(obj)
        }
        prefs.edit().putString("records_json_$userEmail", arr.toString()).apply()
    }

    fun generateRecordId(): Int {
        val nextId = prefs.getInt("next_record_id", 5)
        prefs.edit().putInt("next_record_id", nextId + 1).apply()
        return nextId
    }

    // DISABLED REMINDERS PER COMPONENT
    fun getDisabledReminders(userEmail: String, vehicleId: Int): Set<String> {
        return prefs.getStringSet("disabled_reminders_${userEmail}_$vehicleId", emptySet()) ?: emptySet()
    }

    fun toggleReminder(userEmail: String, vehicleId: Int, component: String): Boolean {
        val current = getDisabledReminders(userEmail, vehicleId).toMutableSet()
        val newState: Boolean
        if (current.contains(component)) {
            current.remove(component)
            newState = true // now enabled
        } else {
            current.add(component)
            newState = false // now disabled
        }
        prefs.edit().putStringSet("disabled_reminders_${userEmail}_$vehicleId", current).apply()
        return newState
    }
}
