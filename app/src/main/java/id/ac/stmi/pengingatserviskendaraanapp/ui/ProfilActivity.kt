package id.ac.stmi.pengingatserviskendaraanapp.ui


import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import id.ac.stmi.pengingatserviskendaraanapp.MainActivity
import id.ac.stmi.pengingatserviskendaraanapp.R
import id.ac.stmi.pengingatserviskendaraanapp.data.ServisRepository
import id.ac.stmi.pengingatserviskendaraanapp.model.Account
import id.ac.stmi.pengingatserviskendaraanapp.model.ComponentConstants
import id.ac.stmi.pengingatserviskendaraanapp.model.Vehicle
import id.ac.stmi.pengingatserviskendaraanapp.util.ServiceCalculator
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil

class ProfilActivity : AppCompatActivity() {

    private lateinit var repository: ServisRepository
    private var currentUserEmail: String? = null
    private var currentAccount: Account? = null

    private lateinit var ivAvatar: ImageView
    private lateinit var tvAvatarInitial: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnEditProfile: ImageView
    private lateinit var rvVehicles: RecyclerView
    private lateinit var btnAddVehicle: Button
    private lateinit var btnLogout: Button
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profil)

        WindowInsetUtil.applySystemBarInsets(
            findViewById(R.id.root_profil)
        )

        repository = ServisRepository(this)
        currentUserEmail = repository.currentAccountEmail

        if (currentUserEmail.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        ivAvatar = findViewById(R.id.iv_profile_avatar)
        tvAvatarInitial = findViewById(R.id.tv_avatar_initial)
        tvName = findViewById(R.id.tv_profile_name)
        tvEmail = findViewById(R.id.tv_profile_email)
        btnEditProfile = findViewById(R.id.btn_edit_profile)
        rvVehicles = findViewById(R.id.rv_profile_vehicles)
        btnAddVehicle = findViewById(R.id.btn_add_vehicle)
        btnLogout = findViewById(R.id.btn_logout)
        bottomNav = findViewById(R.id.bottom_navigation)

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfilActivity::class.java))
        }

        btnAddVehicle.setOnClickListener {
            val intent = Intent(this, IdentitasKendaraanActivity::class.java).apply {
                putExtra("vehicle_id", 0)
            }
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            repository.currentAccountEmail = null
            Toast.makeText(this, "Kamu sudah logout", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    private fun setupBottomNav() {
        bottomNav.selectedItemId = R.id.navigation_profil
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_riwayat -> {
                    startActivity(Intent(this, RiwayatActivity::class.java))
                   overridePendingTransition(0, 0)
                    finish()
                    true
               }
                R.id.navigation_profil -> true
                else -> false
            }
        }
    }

    private fun loadProfileData() {
        val email = currentUserEmail ?: return
        currentAccount = repository.getAccount(email)

        val acc = currentAccount ?: return
        tvName.text = acc.name
        tvEmail.text = acc.email

        val firstLetter = if (acc.name.isNotBlank()) acc.name.trim()[0].uppercaseChar().toString() else "?"
        tvAvatarInitial.text = firstLetter

        if (!acc.avatarUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(acc.avatarUri)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    ivAvatar.setImageBitmap(bitmap)
                    ivAvatar.visibility = View.VISIBLE
                    tvAvatarInitial.visibility = View.GONE
                } else {
                    ivAvatar.visibility = View.GONE
                    tvAvatarInitial.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                ivAvatar.visibility = View.GONE
                tvAvatarInitial.visibility = View.VISIBLE
            }
        } else {
            ivAvatar.visibility = View.GONE
            tvAvatarInitial.visibility = View.VISIBLE
        }

        val vehicles = repository.getVehicles(email)
        rvVehicles.adapter = VehicleCardAdapter(vehicles)
    }

    inner class VehicleCardAdapter(private val list: List<Vehicle>) :
        RecyclerView.Adapter<VehicleCardAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val viewColor: View = v.findViewById(R.id.view_vehicle_color)
            val tvName: TextView = v.findViewById(R.id.tv_vehicle_item_name)
            val tvDetails: TextView = v.findViewById(R.id.tv_vehicle_item_details)
            val btnEdit: Button = v.findViewById(R.id.btn_edit_vehicle_card)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_vehicle_card, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val v = list[position]
            holder.tvName.text = v.name

            val colorHex = ComponentConstants.COLORS[v.color] ?: "#1E6FD9"
            holder.viewColor.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorHex))

            val platePart = if (v.plate.isNotEmpty()) " · ${v.plate}" else ""
            holder.tvDetails.text = "${v.brand} ${v.type}$platePart\n${ServiceCalculator.formatNumber(v.currentKm)} km"

            holder.btnEdit.setOnClickListener {
                val intent = Intent(this@ProfilActivity, IdentitasKendaraanActivity::class.java).apply {
                    putExtra("vehicle_id", v.id)
                }
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int = list.size
    }
}
