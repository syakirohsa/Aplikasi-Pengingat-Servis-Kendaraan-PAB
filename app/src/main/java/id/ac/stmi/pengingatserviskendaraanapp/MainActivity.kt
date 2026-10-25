package id.ac.stmi.pengingatserviskendaraanapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import id.ac.stmi.pengingatserviskendaraanapp.data.ServisRepository
import id.ac.stmi.pengingatserviskendaraanapp.model.*
import id.ac.stmi.pengingatserviskendaraanapp.ui.*
import id.ac.stmi.pengingatserviskendaraanapp.util.ServiceCalculator
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil

class MainActivity : AppCompatActivity() {

    private lateinit var repository: ServisRepository
    private var currentUserEmail: String? = null
    private var currentVehicle: Vehicle? = null

    private lateinit var tvNotifBadge: TextView
    private lateinit var btnNotif: FrameLayout
    private lateinit var btnMoreOptions: ImageView
    private lateinit var rvVehicleChips: RecyclerView
    private lateinit var tvVehicleSubtitle: TextView
    private lateinit var tvOdometerValue: TextView
    private lateinit var btnEditOdometer: ImageView
    private lateinit var cardNextService: View
    private lateinit var tvMainCompTitle: TextView
    private lateinit var pbMainService: ProgressBar
    private lateinit var tvMainRemaining: TextView
    private lateinit var tvMainStatusChip: TextView
    private lateinit var tvMainSchedule: TextView
    private lateinit var btnRecordMainService: Button
    private lateinit var rvOtherComponents: RecyclerView
    private lateinit var fabAddService: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        WindowInsetUtil.applySystemBarInsets(findViewById(R.id.main_root))

        repository = ServisRepository(this)
        currentUserEmail = repository.currentAccountEmail

        if (currentUserEmail.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupBottomNav()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadDataAndRefreshUi()
    }

    private fun initViews() {
        tvNotifBadge = findViewById(R.id.tv_notif_badge)
        btnNotif = findViewById(R.id.btn_notif)
        btnMoreOptions = findViewById(R.id.btn_more_options)
        rvVehicleChips = findViewById(R.id.rv_vehicle_chips)
        tvVehicleSubtitle = findViewById(R.id.tv_vehicle_subtitle)
        tvOdometerValue = findViewById(R.id.tv_odometer_value)
        btnEditOdometer = findViewById(R.id.btn_edit_odometer)
        cardNextService = findViewById(R.id.card_next_service)
        tvMainCompTitle = findViewById(R.id.tv_main_comp_title)
        pbMainService = findViewById(R.id.pb_main_service)
        tvMainRemaining = findViewById(R.id.tv_main_remaining)
        tvMainStatusChip = findViewById(R.id.tv_main_status_chip)
        tvMainSchedule = findViewById(R.id.tv_main_schedule)
        btnRecordMainService = findViewById(R.id.btn_record_main_service)
        rvOtherComponents = findViewById(R.id.rv_other_components)
        fabAddService = findViewById(R.id.fab_add_service)
        bottomNav = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.navigation_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_riwayat -> {
                    startActivity(Intent(this, RiwayatActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_profil -> {
                    startActivity(Intent(this, ProfilActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupListeners() {
        btnNotif.setOnClickListener {
            startActivity(Intent(this, NotifikasiActivity::class.java))
        }

        btnMoreOptions.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("Riwayat servis")
            popup.menu.add("Pengaturan")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Riwayat servis" -> {
                        startActivity(Intent(this, RiwayatActivity::class.java))
                        true
                    }
                    "Pengaturan" -> {
                        startActivity(Intent(this, PengaturanActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        btnEditOdometer.setOnClickListener {
            showEditOdometerDialog()
        }

        fabAddService.setOnClickListener {
            val intent = Intent(this, TambahServisActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadDataAndRefreshUi() {
        val email = currentUserEmail ?: return
        val vehicles = repository.getVehicles(email)

        if (vehicles.isEmpty()) {
            startActivity(Intent(this, IdentitasKendaraanActivity::class.java))
            finish()
            return
        }

        val selectedId = repository.getSelectedVehicleId(email)
        currentVehicle = vehicles.find { it.id == selectedId } ?: vehicles[0]
        val v = currentVehicle!!

        // Setup vehicle chips
        rvVehicleChips.adapter = VehicleChipAdapter(vehicles, v.id) { vehicleId ->
            if (vehicleId == -1) {
                startActivity(Intent(this, IdentitasKendaraanActivity::class.java))
            } else {
                repository.setSelectedVehicleId(email, vehicleId)
                loadDataAndRefreshUi()
            }
        }

        // Subtitle & Odometer
        val yearPart = if (v.year.isNotEmpty()) " · ${v.year}" else ""
        val platePart = if (v.plate.isNotEmpty()) " · ${v.plate}" else ""
        tvVehicleSubtitle.text = "${v.brand} ${v.type}$yearPart$platePart"
        tvOdometerValue.text = "${ServiceCalculator.formatNumber(v.currentKm)} km"

        // Records & Reminders
        val records = repository.getServiceRecords(email)
        val disabledReminders = repository.getDisabledReminders(email, v.id)
        val isGlobalActive = repository.globalNotificationsEnabled

        val reminders = ServiceCalculator.calculateReminders(v, records, disabledReminders)

        // Count alerts for notification badge
        val alertCount = reminders.count { !it.isNone && it.isEnabled && it.status != RemainderStatus.OK && isGlobalActive }
        if (alertCount > 0) {
            tvNotifBadge.visibility = View.VISIBLE
            tvNotifBadge.text = alertCount.toString()
        } else {
            tvNotifBadge.visibility = View.GONE
        }

        // Main Card (top item)
        val topRemainder = reminders.find { !it.isNone }
        val restReminders = if (topRemainder != null) reminders.filter { it != topRemainder } else reminders

        if (topRemainder != null) {
            cardNextService.visibility = View.VISIBLE
            tvMainCompTitle.text = topRemainder.component
            pbMainService.progress = topRemainder.percentage

            val isOk = topRemainder.status == RemainderStatus.OK
            val colorRes = if (isOk) R.color.green_color else R.color.red_color
            val bgRes = if (isOk) R.drawable.bg_chip_ok else R.drawable.bg_chip_alert

            pbMainService.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))
            tvMainRemaining.text = ServiceCalculator.formatRemainingText(topRemainder)

            tvMainStatusChip.text = when (topRemainder.status) {
                RemainderStatus.OK -> "Aman"
                RemainderStatus.SOON -> "Segera servis"
                RemainderStatus.LATE -> "Terlewat"
            }
            tvMainStatusChip.background = ContextCompat.getDrawable(this, bgRes)
            tvMainStatusChip.setTextColor(ContextCompat.getColor(this, colorRes))

            val disabledText = if (!topRemainder.isEnabled) " (Pengingat nonaktif)" else ""
            tvMainSchedule.text = "Jadwal: ${ServiceCalculator.formatScheduleText(topRemainder)}$disabledText"

            btnRecordMainService.setOnClickListener {
                val intent = Intent(this, TambahServisActivity::class.java).apply {
                    putExtra("component_name", topRemainder.component)
                }
                startActivity(intent)
            }
        } else {
            cardNextService.visibility = View.GONE
        }

        // Other components list
        rvOtherComponents.adapter = MiniComponentAdapter(restReminders,
            onRecordedClick = { remainder ->
                remainder.record?.let { r ->
                    val intent = Intent(this, DetailActivity::class.java).apply {
                        putExtra("service_id", r.id)
                    }
                    startActivity(intent)
                }
            },
            onAddClick = { componentName ->
                val intent = Intent(this, TambahServisActivity::class.java).apply {
                    putExtra("component_name", componentName)
                }
                startActivity(intent)
            }
        )
    }

    private fun showEditOdometerDialog() {
        val v = currentVehicle ?: return
        val et = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(v.currentKm.toString())
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Ubah Odometer saat ini (km)")
            .setView(FrameLayout(this).apply {
                setPadding(50, 20, 50, 20)
                addView(et)
            })
            .setPositiveButton("Simpan") { _, _ ->
                val inputKm = et.text.toString().toIntOrNull()
                if (inputKm == null || inputKm <= 0) {
                    Toast.makeText(this, "Isi angka odometer yang valid", Toast.LENGTH_SHORT).show()
                } else if (inputKm < v.currentKm) {
                    Toast.makeText(this, "Tidak boleh lebih kecil dari km sebelumnya (${v.currentKm})", Toast.LENGTH_SHORT).show()
                } else {
                    v.currentKm = inputKm
                    currentUserEmail?.let { email ->
                        repository.saveVehicle(email, v)
                        Toast.makeText(this, "Odometer diperbarui", Toast.LENGTH_SHORT).show()
                        loadDataAndRefreshUi()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // Vehicle Chips Adapter
    inner class VehicleChipAdapter(
        private val list: List<Vehicle>,
        private val selectedId: Int,
        private val onClick: (Int) -> Unit
    ) : RecyclerView.Adapter<VehicleChipAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val container: LinearLayout = v.findViewById(R.id.chip_container)
            val colorDot: View = v.findViewById(R.id.view_color_dot)
            val nameText: TextView = v.findViewById(R.id.tv_chip_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_vehicle_chip, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position < list.size) {
                val vehicle = list[position]
                holder.nameText.text = vehicle.name
                val colorHex = ComponentConstants.COLORS[vehicle.color] ?: "#1E6FD9"
                holder.colorDot.visibility = View.VISIBLE
                holder.colorDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))

                val isSelected = vehicle.id == selectedId
                holder.container.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    if (isSelected) R.drawable.bg_button_primary else R.drawable.bg_button_outline
                )
                holder.nameText.setTextColor(
                    ContextCompat.getColor(this@MainActivity, if (isSelected) R.color.on_accent_color else R.color.ink_color)
                )

                holder.container.setOnClickListener { onClick(vehicle.id) }
            } else {
                // "+ Tambah" button chip
                holder.nameText.text = "+ Tambah"
                holder.colorDot.visibility = View.GONE
                holder.container.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_button_outline)
                holder.nameText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_color))
                holder.container.setOnClickListener { onClick(-1) }
            }
        }

        override fun getItemCount(): Int = list.size + 1
    }

    // Mini Component Adapter
    inner class MiniComponentAdapter(
        private val list: List<ServiceRemainder>,
        private val onRecordedClick: (ServiceRemainder) -> Unit,
        private val onAddClick: (String) -> Unit
    ) : RecyclerView.Adapter<MiniComponentAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val compName: TextView = v.findViewById(R.id.tv_mini_name)
            val statusChip: TextView = v.findViewById(R.id.tv_mini_status_chip)
            val infoText: TextView = v.findViewById(R.id.tv_mini_info)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mini_component, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.compName.text = item.component

            if (item.isNone) {
                holder.statusChip.visibility = View.GONE
                holder.infoText.text = "Belum ada catatan"
                holder.itemView.setOnClickListener { onAddClick(item.component) }
            } else {
                holder.statusChip.visibility = View.VISIBLE
                holder.statusChip.text = when (item.status) {
                    RemainderStatus.OK -> "Aman"
                    RemainderStatus.SOON -> "Segera servis"
                    RemainderStatus.LATE -> "Terlewat"
                }

                val isOk = item.status == RemainderStatus.OK
                val colorRes = if (isOk) R.color.green_color else R.color.red_color
                val bgRes = if (isOk) R.drawable.bg_chip_ok else R.drawable.bg_chip_alert

                holder.statusChip.background = ContextCompat.getDrawable(this@MainActivity, bgRes)
                holder.statusChip.setTextColor(ContextCompat.getColor(this@MainActivity, colorRes))

                holder.infoText.text = ServiceCalculator.formatRemainingText(item)
                holder.itemView.setOnClickListener { onRecordedClick(item) }
            }
        }

        override fun getItemCount(): Int = list.size
    }
}
