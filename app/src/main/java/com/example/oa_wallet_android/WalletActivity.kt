package com.example.oa_wallet_android

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
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
    var oaDocuments = listOf<File>()
    private lateinit var documentsAdapter: DocumentRVAdapter
    private lateinit var recyclerview: RecyclerView

    val openFileActivityLauncher =
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
                    val alertDialogBuilder = AlertDialog.Builder(this)
                    alertDialogBuilder.setTitle(filename)
                    alertDialogBuilder.setItems(arrayOf("Save to wallet", "Verify", "View"),
                        DialogInterface.OnClickListener { dialog, which ->
                            when (which) {
                                0 -> {
                                    //Save to wallet
                                    saveToWallet(uri,filename)
                                    fetchDocuments()
                                    documentsAdapter = DocumentRVAdapter(oaDocuments)
                                    recyclerview.adapter = documentsAdapter
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

    private fun fetchDocuments() {
        val files = File(filesDir.path).listFiles().filterNotNull()
        oaDocuments = files.filter {
            it.extension == "oa"
        }
    }
}

