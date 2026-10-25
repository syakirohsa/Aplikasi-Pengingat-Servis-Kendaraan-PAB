package id.ac.stmi.pengingatserviskendaraanapp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import id.ac.stmi.pengingatserviskendaraanapp.MainActivity
import id.ac.stmi.pengingatserviskendaraanapp.R
import id.ac.stmi.pengingatserviskendaraanapp.data.ServisRepository
import id.ac.stmi.pengingatserviskendaraanapp.model.Account
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil

class LoginActivity : AppCompatActivity() {

    private lateinit var repository: ServisRepository

    private var isRegisterMode = false
    private var isPasswordVisible = false

    private lateinit var tvSubtitle: TextView
    private lateinit var lblName: TextView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: ImageView
    private lateinit var tvError: TextView
    private lateinit var btnSubmit: Button
    private lateinit var btnSwitchMode: TextView
    private lateinit var tvDemoHint: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        WindowInsetUtil.applySystemBarInsets(findViewById(R.id.root_login))

        repository = ServisRepository(this)

        tvSubtitle = findViewById(R.id.tv_login_subtitle)
        lblName = findViewById(R.id.lbl_name)
        etName = findViewById(R.id.et_name)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnTogglePassword = findViewById(R.id.btn_toggle_password)
        tvError = findViewById(R.id.tv_error)
        btnSubmit = findViewById(R.id.btn_login_submit)
        btnSwitchMode = findViewById(R.id.btn_switch_mode)
        tvDemoHint = findViewById(R.id.tv_demo_hint)

        btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        btnSwitchMode.setOnClickListener {
            isRegisterMode = !isRegisterMode
            tvError.text = ""
            updateModeUi()
        }

        btnSubmit.setOnClickListener {
            handleAuthSubmit()
        }

        updateModeUi()
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        val selectionStart = etPassword.selectionStart
        val selectionEnd = etPassword.selectionEnd

        if (isPasswordVisible) {
            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            btnTogglePassword.setImageResource(R.drawable.ic_eye)
        } else {
            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
        }

        if (selectionStart >= 0 && selectionEnd >= 0) {
            etPassword.setSelection(selectionStart, selectionEnd)
        }
    }

    private fun updateModeUi() {
        if (isRegisterMode) {
            tvSubtitle.text = "Buat akun baru"
            lblName.visibility = View.VISIBLE
            etName.visibility = View.VISIBLE
            btnSubmit.text = "Daftar"
            btnSwitchMode.text = "Sudah punya akun? Masuk"
            tvDemoHint.visibility = View.GONE
        } else {
            tvSubtitle.text = "Masuk ke akunmu"
            lblName.visibility = View.GONE
            etName.visibility = View.GONE
            btnSubmit.text = "Masuk"
            btnSwitchMode.text = "Belum punya akun? Daftar"
            tvDemoHint.visibility = View.VISIBLE
        }
    }

    private fun handleAuthSubmit() {
        val email = etEmail.text.toString().trim().lowercase()
        val password = etPassword.text.toString()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvError.text = "Email tidak valid"
            return
        }

        if (password.length < 6) {
            tvError.text = "Kata sandi minimal 6 karakter"
            return
        }

        var acc = repository.getAccount(email)

        if (isRegisterMode) {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                tvError.text = "Isi nama dulu"
                return
            }
            if (acc != null) {
                tvError.text = "Email sudah terdaftar"
                return
            }
            acc = Account(email, password, name, null)
            repository.saveAccount(acc)
            Toast.makeText(this, "Akun berhasil dibuat", Toast.LENGTH_SHORT).show()
        } else {
            if (acc == null || acc.password != password) {
                tvError.text = "Email atau kata sandi salah"
                return
            }
        }

        repository.currentAccountEmail = acc.email

        val vehicles = repository.getVehicles(acc.email)
        if (vehicles.isEmpty()) {
            Toast.makeText(this, "Isi identitas motor dulu", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, IdentitasKendaraanActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Selamat datang, ${acc.name}", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }
}
