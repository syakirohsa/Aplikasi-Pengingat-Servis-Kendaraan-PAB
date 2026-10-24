package id.ac.stmi.pengingatserviskendaraanapp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import id.ac.stmi.pengingatserviskendaraanapp.R
import id.ac.stmi.pengingatserviskendaraanapp.data.ServisRepository
import id.ac.stmi.pengingatserviskendaraanapp.model.RemainderStatus
import id.ac.stmi.pengingatserviskendaraanapp.model.ServiceRemainder
import id.ac.stmi.pengingatserviskendaraanapp.util.ServiceCalculator
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil

class NotifikasiActivity : AppCompatActivity() {

    private lateinit var repository: ServisRepository
    private var currentUserEmail: String? = null

    private lateinit var btnBack: ImageView
    private lateinit var tvGlobalWarning: TextView
    private lateinit var tvNoAttention: TextView
    private lateinit var layoutGroups: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifikasi)

        WindowInsetUtil.applySystemBarInsets(findViewById(R.id.root_notifikasi))

        repository = ServisRepository(this)
        currentUserEmail = repository.currentAccountEmail

        btnBack = findViewById(R.id.btn_back)
        tvGlobalWarning = findViewById(R.id.tv_global_disabled_warning)
        tvNoAttention = findViewById(R.id.tv_no_attention)
        layoutGroups = findViewById(R.id.layout_notif_groups)

        btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val email = currentUserEmail ?: return
        val vehicles = repository.getVehicles(email)
        if (vehicles.isEmpty()) return

        val selectedId = repository.getSelectedVehicleId(email)
        val currentVehicle = vehicles.find { it.id == selectedId } ?: vehicles[0]

        val records = repository.getServiceRecords(email)
        val disabledSet = repository.getDisabledReminders(email, currentVehicle.id)
        val isGlobalActive = repository.globalNotificationsEnabled

        tvGlobalWarning.visibility = if (isGlobalActive) View.GONE else View.VISIBLE

        val reminders = ServiceCalculator.calculateReminders(currentVehicle, records, disabledSet)

        val attentionList = reminders.filter { !it.isNone && it.isEnabled && it.status != RemainderStatus.OK && isGlobalActive }
        val otherList = reminders.filter { !it.isNone && !attentionList.contains(it) }
        val noneList = reminders.filter { it.isNone }

        tvNoAttention.visibility = if (attentionList.isEmpty()) View.VISIBLE else View.GONE

        layoutGroups.removeAllViews()

        if (attentionList.isNotEmpty()) {
            addGroupSection("Perlu perhatian", attentionList, email, currentVehicle.id)
        }

        if (otherList.isNotEmpty()) {
            addGroupSection("Aman", otherList, email, currentVehicle.id)
        }

        if (noneList.isNotEmpty()) {
            addGroupSection("Belum ada catatan", noneList, email, currentVehicle.id)
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun addGroupSection(
        title: String,
        items: List<ServiceRemainder>,
        userEmail: String,
        vehicleId: Int
    ) {
        val headerView = TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@NotifikasiActivity, R.color.muted_color))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 8)
        }
        layoutGroups.addView(headerView)

        val inflater = LayoutInflater.from(this)

        for (item in items) {
            val view = inflater.inflate(R.layout.item_notif_component, layoutGroups, false)

            val dotIndicator: View = view.findViewById(R.id.view_dot_indicator)
            val compName: TextView = view.findViewById(R.id.tv_notif_comp_name)
            val sub1: TextView = view.findViewById(R.id.tv_notif_sub1)
            val sub2: TextView = view.findViewById(R.id.tv_notif_sub2)
            val btnRecord: TextView = view.findViewById(R.id.btn_record_action)
            val swToggle: SwitchMaterial = view.findViewById(R.id.sw_notif_toggle)

            compName.text = item.component

            if (item.isNone) {
                dotIndicator.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.muted_color))
                sub1.text = "Belum ada catatan servis"
                sub2.visibility = View.GONE
                btnRecord.visibility = View.VISIBLE
                btnRecord.text = "Catat"
                swToggle.visibility = View.GONE
            } else {
                val isOk = item.status == RemainderStatus.OK
                val colorRes = if (isOk) R.color.green_color else R.color.red_color
                dotIndicator.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))

                sub1.text = if (item.isEnabled) ServiceCalculator.formatRemainingText(item) else "Pengingat nonaktif"
                sub2.visibility = View.VISIBLE
                sub2.text = "Jadwal: ${ServiceCalculator.formatScheduleText(item)}"

                if (!isOk && item.isEnabled) {
                    btnRecord.visibility = View.VISIBLE
                    btnRecord.text = "Catat servis"
                } else {
                    btnRecord.visibility = View.GONE
                }

                swToggle.visibility = View.VISIBLE
                swToggle.isChecked = item.isEnabled
                swToggle.setOnCheckedChangeListener { _, _ ->
                    val nowEnabled = repository.toggleReminder(userEmail, vehicleId, item.component)
                    Toast.makeText(this, "Pengingat ${item.component} ${if (nowEnabled) "aktif" else "nonaktif"}", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }

            btnRecord.setOnClickListener {
                val intent = Intent(this, TambahServisActivity::class.java).apply {
                    putExtra("component_name", item.component)
                }
                startActivity(intent)
            }

            layoutGroups.addView(view)
        }
    }
}
