package id.ac.stmi.pengingatserviskendaraanapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import id.ac.stmi.pengingatserviskendaraanapp.MainActivity
import id.ac.stmi.pengingatserviskendaraanapp.R
import id.ac.stmi.pengingatserviskendaraanapp.data.ServisRepository
import id.ac.stmi.pengingatserviskendaraanapp.model.ServiceRecord
import id.ac.stmi.pengingatserviskendaraanapp.model.Vehicle
import id.ac.stmi.pengingatserviskendaraanapp.util.ServiceCalculator
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil
import kotlin.collections.get

class RiwayatActivity : AppCompatActivity() {

    private lateinit var repository: ServisRepository
    private var currentUserEmail: String? = null
    private var currentVehicle: Vehicle? = null

    private lateinit var btnAdd: ImageView
    private lateinit var tvVehicleName: TextView
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_riwayat)


        repository = ServisRepository(this)
        currentUserEmail = repository.currentAccountEmail

        if (currentUserEmail.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        btnAdd = findViewById(R.id.btn_add_from_history)
        tvVehicleName = findViewById(R.id.tv_vehicle_name)
        rvHistory = findViewById(R.id.rv_history_records)
        tvEmpty = findViewById(R.id.tv_empty_history)
        bottomNav = findViewById(R.id.bottom_navigation)

        btnAdd.setOnClickListener {
            startActivity(Intent(this, TambahServisActivity::class.java))
        }

        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.navigation_riwayat
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_riwayat -> true
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

    private fun loadData() {
        val email = currentUserEmail ?: return
        val vehicles = repository.getVehicles(email)
        if (vehicles.isEmpty()) return

        val selectedId = repository.getSelectedVehicleId(email)
        currentVehicle = vehicles.find { it.id == selectedId } ?: vehicles[0]
        val v = currentVehicle!!

        tvVehicleName.text = v.name

        val allRecords = repository.getServiceRecords(email)
        val vehicleRecords = allRecords.filter { it.vehicleId == v.id }

        if (vehicleRecords.isEmpty()) {
            rvHistory.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rvHistory.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            rvHistory.adapter = HistoryAdapter(vehicleRecords)
        }
    }

    private fun showDeleteDialog(record: ServiceRecord) {
        AlertDialog.Builder(this)
            .setTitle("Hapus data servis?")
            .setMessage("Data yang dihapus tidak bisa dikembalikan.")
            .setPositiveButton("Hapus") { _, _ ->
                currentUserEmail?.let { email ->
                    repository.deleteServiceRecord(email, record.id)
                    Toast.makeText(this, "Data dihapus", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun shareRecord(record: ServiceRecord) {
        val shareText = "Servis ${record.component} motor ${currentVehicle?.name ?: ""}: Tanggal ${ServiceCalculator.formatDmy(record.date)}, Odometer ${ServiceCalculator.formatNumber(record.odometer)} km. ${record.notes}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Memilih aplikasi untuk membagikan"))
    }

    inner class HistoryAdapter(private val list: List<ServiceRecord>) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvComp: TextView = v.findViewById(R.id.tv_history_component)
            val tvSubtitle: TextView = v.findViewById(R.id.tv_history_subtitle)
            val btnDots: ImageView = v.findViewById(R.id.btn_history_dots)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history_record, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val record = list[position]
            holder.tvComp.text = record.component
            holder.tvSubtitle.text = "${ServiceCalculator.formatDmy(record.date)} · ${ServiceCalculator.formatNumber(record.odometer)} km"

            holder.itemView.setOnClickListener {
                val intent = Intent(this@RiwayatActivity, DetailActivity::class.java).apply {
                    putExtra("service_id", record.id)
                }
                startActivity(intent)
            }

            holder.itemView.setOnLongClickListener {
                showDeleteDialog(record)
                true
            }

            holder.btnDots.setOnClickListener { view ->
                val popup = PopupMenu(this@RiwayatActivity, view)
                popup.menu.add("Hapus")
                popup.menu.add("Bagikan")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Hapus" -> {
                            showDeleteDialog(record)
                            true
                        }
                        "Bagikan" -> {
                            shareRecord(record)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }

        override fun getItemCount(): Int = list.size
    }
}
