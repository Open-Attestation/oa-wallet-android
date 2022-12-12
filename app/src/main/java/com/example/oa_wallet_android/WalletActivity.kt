package com.example.oa_wallet_android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu

class WalletActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Wallet"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.walletappbarmenu,menu)
        return super.onCreateOptionsMenu(menu)
    }
}