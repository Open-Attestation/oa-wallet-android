package com.example.oa_wallet_android.ui.scanner

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.oa_wallet_android.R
import com.example.oa_wallet_android.databinding.FragmentScannerBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.openattestation.open_attestation_android.OaRendererActivity
import com.openattestation.open_attestation_android.OpenAttestation
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class ScannerFragment : Fragment() {
    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector

    private var scannedValue = ""

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                setupControls()

                val navController = findNavController()
                navController.run {
                    popBackStack()
                    navigate(R.id.navigation_scanner)
                }
            } else {
                Toast.makeText(requireActivity(), "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if (ContextCompat.checkSelfPermission(requireActivity(),CAMERA) != PackageManager.PERMISSION_GRANTED) {
            askForCameraPermission()
        } else {
            setupControls()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraSource.stop()
        _binding = null
    }

    private fun setupControls() {
        barcodeDetector = BarcodeDetector.Builder(requireActivity()).setBarcodeFormats(Barcode.ALL_FORMATS).build()
        cameraSource = CameraSource.Builder(requireActivity(), barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true)
            .build()

        binding.cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            @SuppressLint("MissingPermission")
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                try {
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
                Toast.makeText(context, "Scanner has been closed", Toast.LENGTH_SHORT).show()
            }

            @SuppressLint("MissingPermission")
            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems
                if (barcodes.size() == 1) {
                    scannedValue = barcodes.valueAt(0).rawValue

                    requireActivity().runOnUiThread {
                        
                        cameraSource.stop()
                    }
                    if (!URLUtil.isValidUrl(scannedValue)) {
                        requireActivity().runOnUiThread {
                            val alertDialogBuilder = AlertDialog.Builder(requireActivity())
                            alertDialogBuilder.setTitle("Invalid QR code")
                            alertDialogBuilder.setMessage("Please scan a compatible QR code.")
                            alertDialogBuilder.setPositiveButton("Dismiss") { _, _ ->
                                try {
                                    cameraSource.start(binding.cameraSurfaceView.holder)
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }
                            alertDialogBuilder.show()
                        }
                        return
                    }

                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url(scannedValue)
                        .build()

                    var document: String?
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            requireActivity().runOnUiThread {
                                val alertDialogBuilder = AlertDialog.Builder(requireActivity())
                                alertDialogBuilder.setTitle("Unable to download document!")
                                alertDialogBuilder.setMessage("The QR code is no longer valid.")
                                alertDialogBuilder.setPositiveButton("Dismiss") { _, _ ->
                                    try {
                                        cameraSource.start(binding.cameraSurfaceView.holder)
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                                alertDialogBuilder.show()
                            }
                            return
                        }
                        document = response.body!!.string()
                    }

                    requireActivity().runOnUiThread {
                        val oa = OpenAttestation()
                        oa.verifyDocument(requireActivity(), document!!) { isValid ->
                            if (isValid) {
                                val intent = Intent(requireActivity(), OaRendererActivity::class.java)
                                intent.putExtra(OaRendererActivity.OA_DOCUMENT_KEY, document)
                                startActivity(intent)
                            } else {
                                val alertDialogBuilder = AlertDialog.Builder(requireActivity())
                                alertDialogBuilder.setTitle("Verification failed")
                                alertDialogBuilder.setMessage("This document has been tampered with and cannot be viewed")
                                alertDialogBuilder.setPositiveButton("Dismiss") { _, _ ->
                                    try {
                                        cameraSource.start(binding.cameraSurfaceView.holder)
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                                alertDialogBuilder.show()
                            }
                        }
                    }
                }
            }
        })
    }

    private fun askForCameraPermission() {
        activityResultLauncher.launch(CAMERA)
    }
}