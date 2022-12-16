package com.example.oa_wallet_android

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oa_wallet_android.OpenAttestation.OaRendererActivity
import com.example.oa_wallet_android.OpenAttestation.OpenAttestation
import java.io.*


class WalletActivity : AppCompatActivity() {
    private var oaDocuments = mutableListOf<File>()
    private lateinit var documentsAdapter: DocumentRVAdapter
    private lateinit var recyclerview: RecyclerView

    private val openFileActivityLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
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
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Wallet"

        fetchDocuments()
        recyclerview = findViewById<RecyclerView>(R.id.documentRV)
        recyclerview.layoutManager = LinearLayoutManager(this)
        documentsAdapter = DocumentRVAdapter(oaDocuments)
        documentsAdapter.onOptionsTap = { document ->
            presentDocumentOptions(document)
        }
        recyclerview.adapter = documentsAdapter

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

    private fun readDocument(uri: Uri): String? {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val content = inputStream.bufferedReader().use(BufferedReader::readText)
                inputStream.close()
                return content
            }

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun readDocument(file: File): String? {
        try {
            val inputStream = file.inputStream()
            val content = inputStream.bufferedReader().use(BufferedReader::readText)
            inputStream.close()

            return content
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
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
                        val oadoc = readDocument(uri)
                        if (oadoc != null) {
                            val oa = OpenAttestation()
                            oa.verifyDocument(this, oadoc) { isValid ->
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
                    }
                    2 -> {
                        //View
                        val oadoc = readDocument(uri)
                        if (oadoc != null) {
                            val intent = Intent(this, OaRendererActivity::class.java)
                            intent.putExtra(OaRendererActivity.OA_DOCUMENT_KEY, oadoc)
                            intent.putExtra(OaRendererActivity.OA_DOCUMENT_FILENAME_KEY, filename)
                            startActivity(intent)
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
        alertDialogBuilder.setItems(arrayOf("Verify", "View"),
            DialogInterface.OnClickListener { _, which ->
                when (which) {
                    0 -> {
                        //Verify
                        val oadoc = readDocument(file)
                        if (oadoc != null) {
                            val oa = OpenAttestation()
                            oa.verifyDocument(this, oadoc) { isValid ->
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
                    }
                    1 -> {
                        //View
                        val oadoc = readDocument(file)
                        if (oadoc != null) {
                            val intent = Intent(this, OaRendererActivity::class.java)
                            intent.putExtra(OaRendererActivity.OA_DOCUMENT_KEY, oadoc)
                            intent.putExtra(OaRendererActivity.OA_DOCUMENT_FILENAME_KEY, filename)
                            startActivity(intent)
                        }
                    }
                }
            })
        alertDialogBuilder.setPositiveButton("Dismiss") { _, _ ->

        }
        alertDialogBuilder.show()
    }

}

