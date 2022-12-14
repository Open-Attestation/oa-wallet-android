package com.example.oa_wallet_android.OpenAttestation

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient

class OpenAttestation {
    fun verifyDocument(context: Context, oaDocument: String, callback: (Boolean) -> Unit) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val js = "verifySignature($oaDocument);"
                webView.evaluateJavascript(js) { result ->
                    callback(result.toBoolean())
                }
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.loadDataWithBaseURL(null, "<html><head></head><body><script src=\"file:///android_asset/oabundle.js\" type=\"text/javascript\"></script></body></html>", "text/html", "utf-8", null);
    }
}