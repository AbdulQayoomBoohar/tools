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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    companion object {
        const val SITE = "http://192.168.100.5:8000"

        /** Web application OAuth Client ID from Google Cloud Console (used for ID tokens) */
        var GOOGLE_WEB_CLIENT_ID = ""
    }

    private lateinit var wv: WebView
    private val credentialManager by lazy { CredentialManager.create(this) }
    private val mainScope = CoroutineScope(Dispatchers.Main)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // read the Google client id from the site config once the page is up
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

        wv.addJavascriptInterface(Bridge(), "InvestlyApp")

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean =
                !r.url.toString().startsWith(SITE)

            override fun onPageFinished(view: WebView?, url: String?) {
                // pull the configured google client id out of the page if exposed
                view?.evaluateJavascript(
                    "(window.__GOOGLE_CLIENT_ID || localStorage.getItem('google_client_id') || '')"
                ) { id ->
                    val clean = id?.trim('"', ' ')
                    if (!clean.isNullOrEmpty()) GOOGLE_WEB_CLIENT_ID = clean
                }
            }
        }

        setContentView(wv)
        if (savedInstanceState != null) wv.restoreState(savedInstanceState)
        else wv.loadUrl(SITE)
    }

    private fun sendToJs(js: String) {
        runOnUiThread { wv.evaluateJavascript(js, null) }
    }

    private fun launchGoogleSignIn() {
        mainScope.launch {
            try {
                val clientId = GOOGLE_WEB_CLIENT_ID.ifEmpty {
                    sendToJs("window.__onGoogleError && window.__onGoogleError('Google sign-in is not configured yet.')")
                    return@launch
                }
                val option = GetGoogleIdOption.Builder()
                    .setServerClientId(clientId)
                    .setFilterByAuthorizedAccounts(false) // show ALL accounts, not just previously used
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()

                val result = credentialManager.getCredential(this@MainActivity, request)
                val cred = result.credential
                if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                    val token = googleCred.idToken.replace("\\", "\\\\").replace("'", "\\'")
                    sendToJs("window.__onGoogleId && window.__onGoogleId('$token')")
                } else {
                    sendToJs("window.__onGoogleError && window.__onGoogleError('Unsupported credential type')")
                }
            } catch (e: GetCredentialException) {
                val raw = ((e.message ?: "") + " " + e.javaClass.simpleName)
                if (!raw.contains("cancel", ignoreCase = true)) {
                    val msg = raw.take(120).replace("'", "").replace("\n", " ").trim()
                    sendToJs("window.__onGoogleError && window.__onGoogleError('$msg')")
                }
            } catch (e: Exception) {
                val msg = (e.message ?: "Google sign-in failed").take(120).replace("'", "").replace("\n", " ")
                sendToJs("window.__onGoogleError && window.__onGoogleError('$msg')")
            }
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun copy(text: String): Boolean = try {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("investly", text))
            true
        } catch (_: Exception) { false }

        @JavascriptInterface
        fun isApp(): Boolean = true

        @JavascriptInterface
        fun googleLogin() {
            // JS interface runs on a background thread — hop to main for the Activity dialog
            runOnUiThread { launchGoogleSignIn() }
        }

        @JavascriptInterface
        fun setGoogleClientId(id: String) {
            if (id.isNotBlank()) GOOGLE_WEB_CLIENT_ID = id
        }
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
