package com.example.oa_wallet_android

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oa_wallet_android.OpenAttestation.OaRendererActivity
import com.example.oa_wallet_android.OpenAttestation.OpenAttestation
import java.io.*


class WalletActivity : AppCompatActivity() {
    private var oaDocuments = mutableListOf<File>()
    private lateinit var documentsAdapter: DocumentRVAdapter
    private lateinit var recyclerview: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val openFileActivityLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                handleUriImport(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        progressBar = findViewById<ProgressBar>(R.id.progressBar)

        supportActionBar?.title = "Wallet"

        fetchDocuments()
        recyclerview = findViewById<RecyclerView>(R.id.documentRV)
        recyclerview.layoutManager = LinearLayoutManager(this)
        documentsAdapter = DocumentRVAdapter(oaDocuments)
        documentsAdapter.onItemTap = { document ->
            val oadoc = Utils.readDocument(document)
            if (oadoc != null) {
                viewDocument(oadoc, document.name)
            }
        }
        documentsAdapter.onOptionsTap = { document ->
            presentDocumentOptions(document)
        }
        recyclerview.adapter = documentsAdapter

        var intentUri : Uri? = null
        if (intent.action === Intent.ACTION_VIEW) {
            intentUri = intent.data
        }
        else if (intent.action === Intent.ACTION_SEND) {
            intentUri = intent.clipData?.getItemAt(0)?.uri
        }

        if (intentUri != null) {
            handleUriImport(intentUri)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.walletappbarmenu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_import -> {
            openFileActivityLauncher.launch("*/*")
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun saveToWallet(uri: Uri, filename: String) {
        val outputFile = File(filesDir.path + '/' + filename)
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun fetchDocuments() {
        val files = File(filesDir.path).listFiles()?.filterNotNull()
        if (files != null) {
            oaDocuments.clear()
            oaDocuments.addAll(files.filter {
                it.extension == "oa"
            })
        }
    }

    private fun presentImportOptions(uri: Uri) {
        val filename = Utils.getFileName(this,uri)
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(filename)
        alertDialogBuilder.setItems(arrayOf("Save to wallet", "Verify", "View"),
            DialogInterface.OnClickListener { _, which ->
                when (which) {
                    0 -> {
                        //Save to wallet
                        if (filename != null) {
                            saveToWallet(uri,filename)
                        }
                        fetchDocuments()
                        documentsAdapter.notifyDataSetChanged()
                    }
                    1 -> {
                        //Verify
                        val oadoc = Utils.readDocument(this, uri)
                        if (oadoc != null) {
                            verifyDocument(oadoc)
                        }
                    }
                    2 -> {
                        //View
                        val oadoc = Utils.readDocument(this, uri)
                        if (oadoc != null) {
                            viewDocument(oadoc, filename)
                        }
                    }
                }
            })
        alertDialogBuilder.setPositiveButton("Dismiss") { _, _ ->

        }
        alertDialogBuilder.show()
    }

    private fun presentDocumentOptions(file: File) {
        val filename = file.name
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(filename)
        alertDialogBuilder.setItems(arrayOf("Verify", "View", "Share", "Delete"),
            DialogInterface.OnClickListener { _, which ->
                when (which) {
                    0 -> {
                        //Verify
                        val oadoc = Utils.readDocument(file)
                        if (oadoc != null) {
                            verifyDocument(oadoc)
                        }
                    }
                    1 -> {
                        //View
                        val oadoc = Utils.readDocument(file)
                        if (oadoc != null) {
                            viewDocument(oadoc, filename)
                        }
                    }
                    2 -> {
                        //Share
                        shareDocument(file)
                    }
                    3 -> {
                        //Delete
                        Utils.deleteDocument(file)
                        fetchDocuments()
                        documentsAdapter.notifyDataSetChanged()
                    }
                }
            })
        alertDialogBuilder.setPositiveButton("Dismiss") { _, _ ->

        }
        alertDialogBuilder.show()
    }

    private fun verifyDocument(document: String) {
        showProgressBar()
        val oa = OpenAttestation()
        oa.verifyDocument(this, document) { isValid ->
            hideProgressBar()
            if (isValid) {
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setTitle("Verification successful")
                alertDialogBuilder.setMessage("This document is valid")
                alertDialogBuilder.setPositiveButton("Dismiss", null)
                alertDialogBuilder.show()
            }
            else {
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setTitle("Verification failed")
                alertDialogBuilder.setMessage("This document has been tampered with")
                alertDialogBuilder.setPositiveButton("Dismiss", null)
                alertDialogBuilder.show()
            }
        }
    }
    private fun viewDocument(document: String, filename: String?) {
        showProgressBar()
        val oa = OpenAttestation()
        oa.verifyDocument(this, document) { isValid ->
            hideProgressBar()
            if (isValid) {
                val intent = Intent(this, OaRendererActivity::class.java)
                intent.putExtra(OaRendererActivity.OA_DOCUMENT_KEY, document)
                intent.putExtra(OaRendererActivity.OA_DOCUMENT_FILENAME_KEY, filename)
                startActivity(intent)
            }
            else {
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setTitle("Verification failed")
                alertDialogBuilder.setMessage("This document has been tampered with and cannot be viewed")
                alertDialogBuilder.setPositiveButton("Dismiss", null)
                alertDialogBuilder.show()
            }
        }

    }

    private fun shareDocument(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".provider",
            file
        )

        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/octet-stream"
        }
        startActivity(Intent.createChooser(shareIntent, null))
    }

    private fun handleUriImport(uri: Uri) {
        val filename = Utils.getFileName(this,uri)
        val extension = filename?.split('.')?.last()
        if (extension != "oa") {
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Invalid file type chosen.")
            alertDialogBuilder.setMessage("Only .oa files are supported.")
            alertDialogBuilder.setPositiveButton("Dismiss", null)
            alertDialogBuilder.show()
        } else {
            presentImportOptions(uri)
        }
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }
}

