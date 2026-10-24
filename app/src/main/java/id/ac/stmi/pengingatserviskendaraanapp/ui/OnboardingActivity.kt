package id.ac.stmi.pengingatserviskendaraanapp.ui


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import id.ac.stmi.pengingatserviskendaraanapp.R
import id.ac.stmi.pengingatserviskendaraanapp.util.WindowInsetUtil

class OnboardingActivity : AppCompatActivity() {

    private data class OnboardingPage(
        val iconRes: Int,
        val title: String,
        val subtitle: String
    )

    private val pages = listOf(
        OnboardingPage(R.drawable.ic_history, "Catat servis motor", "Simpan riwayat servis tiap motor kamu di satu tempat."),
        OnboardingPage(R.drawable.ic_plus, "Pantau banyak motor", "Tambahkan beberapa motor dan lihat jadwal servis masing-masing."),
        OnboardingPage(R.drawable.ic_bell, "Diingatkan tepat waktu", "Dapat pengingat saat servis hampir jatuh tempo atau sudah terlewat.")
    )

    private var currentIndex = 0

    private lateinit var ivIcon: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var layoutDots: LinearLayout
    private lateinit var btnSkip: TextView
    private lateinit var btnNext: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        WindowInsetUtil.applySystemBarInsets(findViewById(R.id.root_onboarding))

        ivIcon = findViewById(R.id.iv_onboarding_icon)
        tvTitle = findViewById(R.id.tv_onboarding_title)
        tvSubtitle = findViewById(R.id.tv_onboarding_subtitle)
        layoutDots = findViewById(R.id.layout_dots)
        btnSkip = findViewById(R.id.btn_skip)
        btnNext = findViewById(R.id.btn_next)

        btnSkip.setOnClickListener { navigateToLogin() }
        btnNext.setOnClickListener {
            if (currentIndex < pages.size - 1) {
                currentIndex++
                updateUi()
            } else {
                navigateToLogin()
            }
        }

        updateUi()
    }

    private fun updateUi() {
        val page = pages[currentIndex]
        ivIcon.setImageResource(page.iconRes)
        tvTitle.text = page.title
        tvSubtitle.text = page.subtitle

        val isLast = currentIndex == pages.size - 1
        btnSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
        btnNext.text = if (isLast) "Mulai" else "Lanjut"

        layoutDots.removeAllViews()
        for (i in pages.indices) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(
                if (i == currentIndex) 24.toPx() else 8.toPx(),
                8.toPx()
            ).apply {
                setMargins(4.toPx(), 0, 4.toPx(), 0)
            }
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(this, R.drawable.bg_badge)?.apply {
                setTint(ContextCompat.getColor(this@OnboardingActivity, if (i == currentIndex) R.color.accent_color else R.color.line_color))
            }
            layoutDots.addView(dot)
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}
