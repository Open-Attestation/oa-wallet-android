package com.example.oa_wallet_android.ui.wallet

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oa_wallet_android.*
import com.example.oa_wallet_android.databinding.FragmentWalletBinding
import com.openattestation.open_attestation_android.OaRendererActivity
import com.openattestation.open_attestation_android.OpenAttestation
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class WalletFragment : Fragment() {
    private var oaDocuments = mutableListOf<File>()
    private lateinit var documentsAdapter: DocumentRVAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerview: RecyclerView

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private val openFileActivityLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                handleUriImport(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val walletViewModel =
            ViewModelProvider(this).get(WalletViewModel::class.java)

        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.walletappbarmenu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_import -> {
                        openFileActivityLauncher.launch("*/*")
                        true
                    }
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        progressBar = binding.progressBar

        fetchDocuments()
        recyclerview = binding.documentRV
        recyclerview.layoutManager = LinearLayoutManager(context)
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
        if (activity?.intent?.action === Intent.ACTION_VIEW) {
            intentUri = requireActivity().intent.data
        }
        else if (activity?.intent?.action === Intent.ACTION_SEND) {
            intentUri = requireActivity().intent.clipData?.getItemAt(0)?.uri
        }

        if (intentUri != null) {
            handleUriImport(intentUri)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun saveToWallet(uri: Uri, filename: String) {
        val outputFile = File(requireActivity().filesDir.path + '/' + filename)
        try {
            requireActivity().contentResolver.openInputStream(uri)?.use { input ->
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
        val files = File(requireActivity().filesDir.path).listFiles()?.filterNotNull()
        if (files != null) {
            oaDocuments.clear()
            oaDocuments.addAll(files.filter {
                it.extension == "oa"
            })
        }
    }

    private fun presentImportOptions(uri: Uri) {
        val filename = Utils.getFileName(requireActivity(),uri)
        val alertDialogBuilder = AlertDialog.Builder(requireActivity())
        alertDialogBuilder.setTitle(filename)
        alertDialogBuilder.setItems(arrayOf("Save to wallet", "Verify", "View", "Generate QR Code"),
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
                        val oadoc = Utils.readDocument(requireActivity(), uri)
                        if (oadoc != null) {
                            verifyDocument(oadoc)
                        }
                    }
                    2 -> {
                        //View
                        val oadoc = Utils.readDocument(requireActivity(), uri)
                        if (oadoc != null) {
                            viewDocument(oadoc, filename)
                        }
                    }
                    3 -> {
                        //Generate QR Code
                        val oadoc = Utils.readDocument(requireActivity(), uri)
                        if (oadoc != null) {
                            displayQrValidityOptions(oadoc)
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
        val alertDialogBuilder = AlertDialog.Builder(requireActivity())
        alertDialogBuilder.setTitle(filename)
        alertDialogBuilder.setItems(arrayOf("Verify", "View", "Share", "Generate QR Code", "Delete"),
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
                        //Generate QR Code
                        val oadoc = Utils.readDocument(file)
                        if (oadoc != null) {
                            displayQrValidityOptions(oadoc)
                        }
                    }
                    4 -> {
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
        oa.verifyDocument(requireActivity(), document) { isValid ->
            hideProgressBar()
            if (isValid) {
                val alertDialogBuilder = AlertDialog.Builder(requireActivity())
                alertDialogBuilder.setTitle("Verification successful")
                alertDialogBuilder.setMessage("This document is valid")
                alertDialogBuilder.setPositiveButton("Dismiss", null)
                alertDialogBuilder.show()
            }
            else {
                val alertDialogBuilder = AlertDialog.Builder(requireActivity())
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
        oa.verifyDocument(requireActivity(), document) { isValid ->
            hideProgressBar()
            if (isValid) {
                val intent = Intent(activity, OaRendererActivity::class.java)
                intent.putExtra(OaRendererActivity.OA_DOCUMENT_KEY, document)
                intent.putExtra(OaRendererActivity.OA_DOCUMENT_FILENAME_KEY, filename)
                startActivity(intent)
            }
            else {
                val alertDialogBuilder = AlertDialog.Builder(requireActivity())
                alertDialogBuilder.setTitle("Verification failed")
                alertDialogBuilder.setMessage("This document has been tampered with and cannot be viewed")
                alertDialogBuilder.setPositiveButton("Dismiss", null)
                alertDialogBuilder.show()
            }
        }

    }

    private fun shareDocument(file: File) {
        val uri = FileProvider.getUriForFile(
            requireActivity(),
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
        val filename = Utils.getFileName(requireActivity(),uri)
        val extension = filename?.split('.')?.last()
        if (extension != "oa") {
            val alertDialogBuilder = AlertDialog.Builder(requireActivity())
            alertDialogBuilder.setTitle("Invalid file type chosen.")
            alertDialogBuilder.setMessage("Only .oa files are supported.")
            alertDialogBuilder.setPositiveButton("Dismiss", null)
            alertDialogBuilder.show()
        } else {
            presentImportOptions(uri)
        }
    }

    private fun displayQrValidityOptions(document: String) {

        if(Config.getuploadurlEndpoint.isEmpty() || Config.getdownloadurlEndpoint.isEmpty()) {
            val alertDialogBuilder = AlertDialog.Builder(requireActivity())
            alertDialogBuilder.setTitle("Error: QR Generation Endpoints not configured!")
            alertDialogBuilder.setMessage("Please open Config.swift to add the endpoints")
            alertDialogBuilder.setPositiveButton("Dismiss", null)
            alertDialogBuilder.show()
            return
        }



        val alertDialogBuilder = AlertDialog.Builder(requireActivity())
        alertDialogBuilder.setTitle("Select validity period")
        alertDialogBuilder.setItems(arrayOf("1 Hour", "1 Day", "3 Days", "7 Days"),
            DialogInterface.OnClickListener { _, which ->
                when (which) {
                    0 -> {
                        //1 Hour
                        displayDocumentQR(document,3600)
                    }
                    1 -> {
                        //1 Day
                        displayDocumentQR(document,86400)
                    }
                    2 -> {
                        //3 Days
                        displayDocumentQR(document,259200)
                    }
                    3 -> {
                        //7 Days
                        displayDocumentQR(document,604800)
                    }
                }
            })
        alertDialogBuilder.setPositiveButton("Cancel") { _, _ ->

        }
        alertDialogBuilder.show()
    }

    private fun displayDocumentQR(document: String, validity: Int) {
        Thread {
            val downloadUrl = DocumentsService.uploadDocument(document,validity)
            val intent = Intent(requireActivity(), QRCodeActivity::class.java)
            intent.putExtra(QRCodeActivity.QR_DATA_KEY, downloadUrl)
            startActivity(intent)
        }.start()
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }

}