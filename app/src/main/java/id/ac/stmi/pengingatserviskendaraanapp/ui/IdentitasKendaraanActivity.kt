package id.ac.stmi.pengingatserviskendaraanapp.ui


import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import id.ac.stmi.pengingatserviskendaraanapp.MainActivity
import id.ac.stmi.pengingatserviskendaraanapp.R
import id.ac.stmi.pengingatserviskendaraanapp.data.ServisRepository
import id.ac.stmi.pengingatserviskendaraanapp.model.ComponentConstants
import id.ac.stmi.pengingatserviskendaraanapp.model.Vehicle
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil
import java.util.Calendar

class IdentitasKendaraanActivity : AppCompatActivity() {

    private lateinit var repository: ServisRepository
    private var currentUserEmail: String? = null
    private var vehicleId: Int = 0
    private var selectedColorName: String = "Merah"

    private lateinit var btnBack: ImageView
    private lateinit var etName: EditText
    private lateinit var etType: EditText
    private lateinit var etBrand: EditText
    private lateinit var etYear: EditText
    private lateinit var etPlate: EditText
    private lateinit var tvSelectedColor: TextView
    private lateinit var layoutColorPalette: LinearLayout
    private lateinit var etKm: EditText
    private lateinit var tvError: TextView
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_identitas_kendaraan)

        WindowInsetUtil.applySystemBarInsets(findViewById(R.id.root_identitas_kendaraan))

        repository = ServisRepository(this)
        currentUserEmail = repository.currentAccountEmail

        btnBack = findViewById(R.id.btn_back)
        etName = findViewById(R.id.et_veh_name)
        etType = findViewById(R.id.et_veh_type)
        etBrand = findViewById(R.id.et_veh_brand)
        etYear = findViewById(R.id.et_veh_year)
        etPlate = findViewById(R.id.et_veh_plate)
        tvSelectedColor = findViewById(R.id.tv_selected_color)
        layoutColorPalette = findViewById(R.id.layout_color_palette)
        etKm = findViewById(R.id.et_veh_km)
        tvError = findViewById(R.id.tv_veh_error)
        btnSave = findViewById(R.id.btn_save_vehicle)

        vehicleId = intent.getIntExtra("vehicle_id", 0)

        val email = currentUserEmail
        val vehicles = if (email != null) repository.getVehicles(email) else emptyList()

        if (vehicles.isEmpty()) {
            btnBack.visibility = View.GONE
        } else {
            btnBack.setOnClickListener { finish() }
        }

        setupColorPalette()

        if (vehicleId > 0 && email != null) {
            val existing = vehicles.find { it.id == vehicleId }
            existing?.let { populateFields(it) }
            btnSave.text = "Simpan"
        } else {
            btnSave.text = "Tambah"
        }

        btnSave.setOnClickListener { handleSave() }
    }

    private fun setupColorPalette() {
        layoutColorPalette.removeAllViews()

        var rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        var count = 0
        for ((colorName, colorHex) in ComponentConstants.COLORS) {
            val circleView = View(this).apply {
                val size = (44 * resources.displayMetrics.density).toInt()
                val margin = (6 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                background = ContextCompat.getDrawable(this@IdentitasKendaraanActivity, R.drawable.bg_circle)
                backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))
                isClickable = true
                isFocusable = true
            }

            circleView.setOnClickListener {
                selectedColorName = colorName
                tvSelectedColor.text = colorName
            }

            rowLayout.addView(circleView)
            count++

            if (count % 5 == 0) {
                layoutColorPalette.addView(rowLayout)
                rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }
        }

        if (count % 5 != 0) {
            layoutColorPalette.addView(rowLayout)
        }
    }

    private fun populateFields(v: Vehicle) {
        etName.setText(v.name)
        etType.setText(v.type)
        etBrand.setText(v.brand)
        etYear.setText(v.year)
        etPlate.setText(v.plate)
        selectedColorName = v.color
        tvSelectedColor.text = v.color
        etKm.setText(v.currentKm.toString())
    }

    private fun handleSave() {
        val email = currentUserEmail ?: return
        val name = etName.text.toString().trim()
        val type = etType.text.toString().trim()
        val brand = etBrand.text.toString().trim()
        val yearStr = etYear.text.toString().trim()
        val plate = etPlate.text.toString().trim().uppercase()
        val kmStr = etKm.text.toString().trim()

        if (name.isEmpty() || type.isEmpty() || brand.isEmpty()) {
            tvError.text = "Isi nama panggilan, tipe, dan merk"
            return
        }

        if (yearStr.isNotEmpty()) {
            val yr = yearStr.toIntOrNull()
            val currentYr = Calendar.getInstance().get(Calendar.YEAR)
            if (yr == null || yr < 1970 || yr > currentYr + 1) {
                tvError.text = "Tahun tidak valid"
                return
            }
        }

        val kmVal = kmStr.toIntOrNull()
        if (kmVal == null || kmVal < 0) {
            tvError.text = "Isi KM saat ini"
            return
        }

        val targetId = if (vehicleId > 0) vehicleId else repository.generateVehicleId()
        val vehicle = Vehicle(
            id = targetId,
            name = name,
            type = type,
            brand = brand,
            year = yearStr,
            plate = plate,
            color = selectedColorName,
            currentKm = kmVal
        )

        repository.saveVehicle(email, vehicle)
        repository.setSelectedVehicleId(email, vehicle.id)

        Toast.makeText(this, "Data motor disimpan", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
