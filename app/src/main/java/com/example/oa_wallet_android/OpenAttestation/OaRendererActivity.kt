package com.example.oa_wallet_android.OpenAttestation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.example.oa_wallet_android.R

class OaRendererActivity : AppCompatActivity() {
    companion object{
        const val OA_DOCUMENT_KEY = "oadocument"
        const val OA_DOCUMENT_FILENAME_KEY = "oadocumentfilename"
    }

    var filename: String? = null
    var oaDocument: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oa_renderer)

        filename = intent.extras?.getString(OA_DOCUMENT_FILENAME_KEY)
        if (filename != null) {
            title = filename
        }
        oaDocument = intent.extras?.getString(OA_DOCUMENT_KEY)
        if (oaDocument == null) {
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Error: Failed to render document")
            alertDialogBuilder.setMessage("Unable to read document from the activity intent extras")
            alertDialogBuilder.setPositiveButton("Dismiss") { _, _ ->
                finish()
            }
            alertDialogBuilder.show()
        }
    }
}