package id.ac.stmi.pengingatserviskendaraanapp.ui


import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import id.ac.stmi.pengingatserviskendaraanapp.R
import id.ac.stmi.pengingatserviskendaraanapp.data.ServisRepository
import id.ac.stmi.pengingatserviskendaraanapp.model.Account
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil

class EditProfilActivity : AppCompatActivity() {

    private lateinit var repository: ServisRepository
    private var currentUserEmail: String? = null
    private var currentAccount: Account? = null
    private var selectedAvatarUriStr: String? = null

    private lateinit var btnBack: ImageView
    private lateinit var ivAvatar: ImageView
    private lateinit var tvAvatarInitial: TextView
    private lateinit var btnPickImage: Button
    private lateinit var btnRemoveImage: Button
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var tvError: TextView
    private lateinit var btnSave: Button

    private val getContentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            selectedAvatarUriStr = uri.toString()
            displayAvatar(selectedAvatarUriStr)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profil)

        WindowInsetUtil.applySystemBarInsets(findViewById(R.id.root_edit_profil))

        repository = ServisRepository(this)
        currentUserEmail = repository.currentAccountEmail

        btnBack = findViewById(R.id.btn_back)
        ivAvatar = findViewById(R.id.iv_edit_avatar)
        tvAvatarInitial = findViewById(R.id.tv_edit_avatar_initial)
        btnPickImage = findViewById(R.id.btn_pick_image)
        btnRemoveImage = findViewById(R.id.btn_remove_image)
        etName = findViewById(R.id.et_edit_name)
        etEmail = findViewById(R.id.et_edit_email)
        tvError = findViewById(R.id.tv_edit_error)
        btnSave = findViewById(R.id.btn_save_profile)

        btnBack.setOnClickListener { finish() }

        btnPickImage.setOnClickListener {
            getContentLauncher.launch("image/*")
        }

        btnRemoveImage.setOnClickListener {
            selectedAvatarUriStr = null
            displayAvatar(null)
        }

        btnSave.setOnClickListener { handleSave() }

        loadData()
    }

    private fun loadData() {
        val email = currentUserEmail ?: return
        currentAccount = repository.getAccount(email)

        val acc = currentAccount ?: return
        etName.setText(acc.name)
        etEmail.setText(acc.email)
        selectedAvatarUriStr = acc.avatarUri

        displayAvatar(selectedAvatarUriStr)
    }

    private fun displayAvatar(uriStr: String?) {
        val name = etName.text.toString().ifEmpty { currentAccount?.name ?: "?" }
        val firstLetter = if (name.isNotBlank()) name.trim()[0].uppercaseChar().toString() else "?"
        tvAvatarInitial.text = firstLetter

        if (!uriStr.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(uriStr)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    ivAvatar.setImageBitmap(bitmap)
                    ivAvatar.visibility = View.VISIBLE
                    tvAvatarInitial.visibility = View.GONE
                    btnRemoveImage.visibility = View.VISIBLE
                } else {
                    ivAvatar.visibility = View.GONE
                    tvAvatarInitial.visibility = View.VISIBLE
                    btnRemoveImage.visibility = View.GONE
                }
            } catch (_: Exception) {
                ivAvatar.visibility = View.GONE
                tvAvatarInitial.visibility = View.VISIBLE
                btnRemoveImage.visibility = View.GONE
            }
        } else {
            ivAvatar.visibility = View.GONE
            tvAvatarInitial.visibility = View.VISIBLE
            btnRemoveImage.visibility = View.GONE
        }
    }

    private fun handleSave() {
        val acc = currentAccount ?: return
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim().lowercase()

        if (name.isEmpty()) {
            tvError.text = "Nama tidak boleh kosong"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvError.text = "Email tidak valid"
            return
        }

        if (!email.equals(acc.email, ignoreCase = true) && repository.getAccount(email) != null) {
            tvError.text = "Email sudah dipakai akun lain"
            return
        }

        acc.name = name
        acc.email = email
        acc.avatarUri = selectedAvatarUriStr

        repository.saveAccount(acc)
        repository.currentAccountEmail = email

        Toast.makeText(this, "Profil diperbarui", Toast.LENGTH_SHORT).show()
        finish()
    }
}
