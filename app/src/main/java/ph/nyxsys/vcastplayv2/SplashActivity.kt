/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import ph.nyxsys.vcastplayv2.databinding.ActivitySplashBinding
import androidx.core.net.toUri

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

        val videoUri = "android.resource://${packageName}/${R.raw.vcast_high_res2}".toUri()
        binding.splashGif.setVideoURI(videoUri)

        binding.splashGif.setOnCompletionListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.splashGif.setOnPreparedListener { it.isLooping = false }
        binding.splashGif.start()

    }
}