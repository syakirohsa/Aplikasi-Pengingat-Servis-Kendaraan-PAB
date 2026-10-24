package id.ac.stmi.pengingatserviskendaraanapp.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import id.ac.stmi.pengingatserviskendaraanapp.R
import id.ac.stmi.pengingatserviskendaraanapp.data.ServisRepository
import id.ac.stmi.pengingatserviskendaraanapp.model.ComponentConstants
import id.ac.stmi.pengingatserviskendaraanapp.model.ServiceRecord
import id.ac.stmi.pengingatserviskendaraanapp.util.ServiceCalculator
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil
import java.util.Calendar

class TambahServisActivity : AppCompatActivity() {

    private lateinit var repository: ServisRepository
    private var currentUserEmail: String? = null

    private lateinit var btnBack: ImageView
    private lateinit var spComponent: Spinner
    private lateinit var etDate: EditText
    private lateinit var etOdometer: EditText
    private lateinit var etNotes: EditText
    private lateinit var cbReminder: CheckBox
    private lateinit var tvFormError: TextView
    private lateinit var btnSave: Button

    private var selectedDateIso = ServiceCalculator.getTodayIso()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tambah_servis)

        WindowInsetUtil.applySystemBarInsets(findViewById(R.id.root_tambah_servis))

        repository = ServisRepository(this)
        currentUserEmail = repository.currentAccountEmail

        btnBack = findViewById(R.id.btn_back)
        spComponent = findViewById(R.id.sp_component)
        etDate = findViewById(R.id.et_date)
        etOdometer = findViewById(R.id.et_odometer)
        etNotes = findViewById(R.id.et_notes)
        cbReminder = findViewById(R.id.cb_reminder)
        tvFormError = findViewById(R.id.tv_form_error)
        btnSave = findViewById(R.id.btn_save_service)

        btnBack.setOnClickListener { finish() }

        // Setup Component Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ComponentConstants.NAMES)
        spComponent.adapter = adapter

        // Prefill component name if passed via Intent
        val prefilledComp = intent.getStringExtra("component_name")
        if (prefilledComp != null) {
            val idx = ComponentConstants.NAMES.indexOf(prefilledComp)
            if (idx >= 0) spComponent.setSelection(idx)
        }

        // Setup Date
        etDate.setText(selectedDateIso)
        etDate.setOnClickListener { showDatePicker() }

        // Prefill odometer placeholder with current vehicle km
        val email = currentUserEmail
        if (email != null) {
            val selectedVehicleId = repository.getSelectedVehicleId(email)
            val vehicle = repository.getVehicles(email).find { it.id == selectedVehicleId }
            vehicle?.let {
                etOdometer.hint = it.currentKm.toString()
            }
        }

        btnSave.setOnClickListener { handleSaveService() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(this, { _, y, m, d ->
            val monthFormatted = String.format("%02d", m + 1)
            val dayFormatted = String.format("%02d", d)
            selectedDateIso = "$y-$monthFormatted-$dayFormatted"
            etDate.setText(selectedDateIso)
        }, year, month, day)

        dpd.show()
    }

    private fun handleSaveService() {
        val email = currentUserEmail ?: return
        val odometerStr = etOdometer.text.toString().trim()

        if (odometerStr.isEmpty()) {
            tvFormError.text = "Isi odometer dulu"
            return
        }

        val odometerVal = odometerStr.toIntOrNull()
        if (odometerVal == null || odometerVal <= 0) {
            tvFormError.text = "Isi odometer yang valid"
            return
        }

        val selectedComponent = spComponent.selectedItem.toString()
        val selectedVehicleId = repository.getSelectedVehicleId(email)
        val notes = etNotes.text.toString().trim()

        val recordId = repository.generateRecordId()
        val record = ServiceRecord(
            id = recordId,
            vehicleId = selectedVehicleId,
            component = selectedComponent,
            date = selectedDateIso,
            odometer = odometerVal,
            notes = notes
        )

        repository.addServiceRecord(email, record)

        // Update vehicle odometer if higher
        val vehicle = repository.getVehicles(email).find { it.id == selectedVehicleId }
        if (vehicle != null && odometerVal > vehicle.currentKm) {
            vehicle.currentKm = odometerVal
            repository.saveVehicle(email, vehicle)
        }

        // Handle reminder toggle if disabled vs enabled
        val disabledSet = repository.getDisabledReminders(email, selectedVehicleId)
        val wantsReminder = cbReminder.isChecked
        if (!wantsReminder && !disabledSet.contains(selectedComponent)) {
            repository.toggleReminder(email, selectedVehicleId, selectedComponent)
        } else if (wantsReminder && disabledSet.contains(selectedComponent)) {
            repository.toggleReminder(email, selectedVehicleId, selectedComponent)
        }

        Toast.makeText(this, "Servis tersimpan, jadwal dihitung ulang", Toast.LENGTH_SHORT).show()
        finish()
    }
}
