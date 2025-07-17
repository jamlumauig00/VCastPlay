package ph.nyxsys.vcastplayv2

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import ph.nyxsys.vcastplayv2.databinding.ActivityMainBinding
import ph.nyxsys.vcastplayv2.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setBackgroundDrawableResource(android.R.color.black) // or transparent

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        supportActionBar?.hide()

        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.vcast_high_res2}")
        binding.splashGif.setVideoURI(videoUri)

        binding.splashGif.setOnCompletionListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.splashGif.setOnPreparedListener { it.isLooping = false }
        binding.splashGif.start()

    }
}