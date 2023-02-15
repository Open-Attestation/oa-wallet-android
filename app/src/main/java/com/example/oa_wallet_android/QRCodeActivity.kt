package com.example.oa_wallet_android

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder

class QRCodeActivity : AppCompatActivity() {
    companion object {
        const val QR_DATA_KEY = "QR_DATA"
    }

    var qrStringValue: String? = null
    lateinit var bitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)

        qrStringValue = intent.extras?.getString(QR_DATA_KEY)
        var qrImageView = findViewById<ImageView>(R.id.qrImageView)
        var qrEncoder = QRGEncoder(qrStringValue, null, QRGContents.Type.TEXT, qrImageView.width)
        try {
            bitmap = qrEncoder.getBitmap(0)
            qrImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.qrcodeappbarmenu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_close -> {
            finish()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }
}