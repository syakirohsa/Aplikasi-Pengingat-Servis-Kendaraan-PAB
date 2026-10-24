package id.ac.stmi.pengingatserviskendaraanapp.ui


import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import id.ac.stmi.pengingatserviskendaraanapp.R
import id.ac.stmi.pengingatserviskendaraanapp.data.ServisRepository
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil

class PengaturanActivity : AppCompatActivity() {

    private lateinit var repository: ServisRepository
    private lateinit var btnBack: ImageView
    private lateinit var swGlobalNotif: SwitchMaterial

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pengaturan)

        WindowInsetUtil.applySystemBarInsets(findViewById(R.id.root_pengaturan))

        repository = ServisRepository(this)

        btnBack = findViewById(R.id.btn_back)
        swGlobalNotif = findViewById(R.id.sw_global_notif)

        btnBack.setOnClickListener { finish() }

        swGlobalNotif.isChecked = repository.globalNotificationsEnabled

        swGlobalNotif.setOnCheckedChangeListener { _, isChecked ->
            repository.globalNotificationsEnabled = isChecked
            Toast.makeText(
                this,
                "Pengingat servis ${if (isChecked) "aktif" else "nonaktif"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
