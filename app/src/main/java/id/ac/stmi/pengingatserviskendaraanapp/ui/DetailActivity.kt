package id.ac.stmi.pengingatserviskendaraanapp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import id.ac.stmi.pengingatserviskendaraanapp.R
import id.ac.stmi.pengingatserviskendaraanapp.data.ServisRepository
import id.ac.stmi.pengingatserviskendaraanapp.model.ComponentConstants
import id.ac.stmi.pengingatserviskendaraanapp.model.ServiceRecord
import id.ac.stmi.pengingatserviskendaraanapp.util.ServiceCalculator
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil

class DetailActivity : AppCompatActivity() {

    private lateinit var repository: ServisRepository
    private var currentUserEmail: String? = null
    private var currentRecord: ServiceRecord? = null

    private lateinit var btnBack: ImageView
    private lateinit var tvComponent: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvOdometer: TextView
    private lateinit var tvNotes: TextView
    private lateinit var tvNextSchedule: TextView

    private lateinit var cardTargetInfo: View
    private lateinit var tvInfoTargetTitle: TextView
    private lateinit var tvInfoInterval: TextView
    private lateinit var tvInfoCategory: TextView
    private lateinit var tvInfoTypicalRange: TextView
    private lateinit var tvInfoNote: TextView

    private lateinit var btnShare: Button
    private lateinit var btnDelete: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail)

        WindowInsetUtil.applySystemBarInsets(findViewById(R.id.root_detail))

        repository = ServisRepository(this)
        currentUserEmail = repository.currentAccountEmail

        btnBack = findViewById(R.id.btn_back)
        tvComponent = findViewById(R.id.tv_det_component)
        tvDate = findViewById(R.id.tv_det_date)
        tvOdometer = findViewById(R.id.tv_det_odometer)
        tvNotes = findViewById(R.id.tv_det_notes)
        tvNextSchedule = findViewById(R.id.tv_det_next_schedule)

        cardTargetInfo = findViewById(R.id.card_target_info)
        tvInfoTargetTitle = findViewById(R.id.tv_info_target_title)
        tvInfoInterval = findViewById(R.id.tv_info_interval)
        tvInfoCategory = findViewById(R.id.tv_info_category)
        tvInfoTypicalRange = findViewById(R.id.tv_info_typical_range)
        tvInfoNote = findViewById(R.id.tv_info_note)

        btnShare = findViewById(R.id.btn_share_service)
        btnDelete = findViewById(R.id.btn_delete_service)

        btnBack.setOnClickListener { finish() }

        val serviceId = intent.getIntExtra("service_id", -1)
        loadRecord(serviceId)
    }

    private fun loadRecord(serviceId: Int) {
        val email = currentUserEmail ?: return
        val records = repository.getServiceRecords(email)
        currentRecord = records.find { it.id == serviceId }

        val r = currentRecord
        if (r == null) {
            Toast.makeText(this, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvComponent.text = r.component
        tvDate.text = ServiceCalculator.formatDmy(r.date)
        tvOdometer.text = "${ServiceCalculator.formatNumber(r.odometer)} km"
        tvNotes.text = if (r.notes.isNotEmpty()) r.notes else "-"

        val (nk, nd) = ServiceCalculator.calculateSchedule(r)
        val scheduleParts = mutableListOf<String>()
        nk?.let { scheduleParts.add("${ServiceCalculator.formatNumber(it)} km") }
        nd?.let { scheduleParts.add(ServiceCalculator.formatDmy(it)) }
        tvNextSchedule.text = if (scheduleParts.isNotEmpty()) scheduleParts.joinToString(" atau ") else "-"

        // Info Card
        val info = ComponentConstants.INFO[r.component]
        if (info != null) {
            cardTargetInfo.visibility = View.VISIBLE
            tvInfoTargetTitle.text = "Target servis ${r.component}"
            tvInfoInterval.text = ServiceCalculator.formatIntervalText(r.component)
            tvInfoCategory.text = info.category
            tvInfoTypicalRange.text = "Kisaran umum: ${info.typicalRange}"
            tvInfoNote.text = info.note
            tvInfoNote.visibility = if (info.note.isNotEmpty()) View.VISIBLE else View.GONE
        } else {
            cardTargetInfo.visibility = View.GONE
        }

        btnShare.setOnClickListener { shareRecord(r) }
        btnDelete.setOnClickListener { showDeleteDialog(r) }
    }

    private fun shareRecord(r: ServiceRecord) {
        val shareText = "Servis ${r.component}: Tanggal ${ServiceCalculator.formatDmy(r.date)}, Odometer ${ServiceCalculator.formatNumber(r.odometer)} km. ${r.notes}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Memilih aplikasi untuk membagikan"))
    }

    private fun showDeleteDialog(r: ServiceRecord) {
        AlertDialog.Builder(this)
            .setTitle("Hapus data servis?")
            .setMessage("Data yang dihapus tidak bisa dikembalikan.")
            .setPositiveButton("Hapus") { _, _ ->
                currentUserEmail?.let { email ->
                    repository.deleteServiceRecord(email, r.id)
                    Toast.makeText(this, "Data dihapus", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
