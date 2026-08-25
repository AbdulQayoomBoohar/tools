package com.investly.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    companion object {
        const val SITE = "http://192.168.100.5:8000"
    }

    private lateinit var wv: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wv = WebView(this)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
        }
        wv.setBackgroundColor(0xFFF4F5F7.toInt())
        wv.overScrollMode = View.OVER_SCROLL_NEVER

        // native clipboard bridge — works even though HTTP pages can't use navigator.clipboard
        wv.addJavascriptInterface(object {
            @JavascriptInterface
            fun copy(text: String): Boolean {
                return try {
                    val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("investly", text))
                    true
                } catch (_: Exception) {
                    false
                }
            }

            @JavascriptInterface
            fun isApp(): Boolean = true
        }, "InvestlyApp")

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean =
                !r.url.toString().startsWith(SITE)
        }

        setContentView(wv)
        if (savedInstanceState != null) wv.restoreState(savedInstanceState)
        else wv.loadUrl(SITE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        wv.saveState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::wv.isInitialized && wv.canGoBack()) wv.goBack()
        else @Suppress("DEPRECATION") super.onBackPressed()
    }
}
