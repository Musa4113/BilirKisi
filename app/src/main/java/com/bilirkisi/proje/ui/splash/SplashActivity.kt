package com.bilirkisi.proje.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bilirkisi.proje.ui.mainActivity.MainActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // uygulamanın yüklenmesi bitene kadar logoyu göster
        startActivity(Intent(this, MainActivity::class.java))
        finish()


    }
}
