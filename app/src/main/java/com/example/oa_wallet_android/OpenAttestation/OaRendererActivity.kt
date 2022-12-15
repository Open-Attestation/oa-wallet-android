package com.example.oa_wallet_android.OpenAttestation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import com.example.oa_wallet_android.R
import org.json.JSONObject

class OaRendererActivity : AppCompatActivity() {
    companion object {
        const val OA_DOCUMENT_KEY = "oadocument"
        const val OA_DOCUMENT_FILENAME_KEY = "oadocumentfilename"
    }

    var isDocumentRendered = false
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
        } else {
            val webView = WebView(this)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    if (isDocumentRendered) {
                        return
                    }

                    val js = "getData($oaDocument);"
                    webView.evaluateJavascript(js) { result ->
                        try {
                            val jsonObj = JSONObject(result)
                            val template = jsonObj.getJSONObject("\$template")
                            val templateUrl = template["url"].toString()

                            var rendererHtml =
                                application.assets.open("oarenderer.html").bufferedReader().use {
                                    it.readText()
                                }

                            rendererHtml =
                                rendererHtml.replace("<TEMPLATE_RENDERER_URL>", templateUrl)
                            rendererHtml = rendererHtml.replace("<OA_DOCUMENT>", jsonObj.toString())

                            isDocumentRendered = true
                            webView.loadDataWithBaseURL(
                                null,
                                rendererHtml,
                                "text/html",
                                "utf-8",
                                null
                            );

                        } catch (e: Exception) {
                            Log.e("", e.toString())
                        }
                    }
                }
            }

            webView.setInitialScale(1);
            webView.settings.loadWithOverviewMode = true
            webView.settings.useWideViewPort = true;
            webView.settings.javaScriptEnabled = true

            setContentView(webView)
            webView.loadDataWithBaseURL(
                null,
                "<html><head></head><body><script src=\"file:///android_asset/oabundle.js\" type=\"text/javascript\"></script>Initialising</body></html>",
                "text/html",
                "utf-8",
                null
            );
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_oa_renderer, menu)
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