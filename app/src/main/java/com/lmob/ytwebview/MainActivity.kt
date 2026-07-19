package com.lmob.ytwebview

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout

    // Pakai versi mobile ringan YouTube, bukan desktop (jauh lebih hemat CPU/RAM)
    private val startUrl = "https://m.youtube.com/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout dibuat lewat kode, tanpa XML inflate berat
        val root = ViewGroup.LayoutParams.MATCH_PARENT

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = ViewGroup.LayoutParams(root, ViewGroup.LayoutParams.WRAP_CONTENT)
            max = 100
            visibility = View.VISIBLE
        }

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(root, root)
            setBackgroundColor(Color.BLACK)
        }

        swipeRefresh = SwipeRefreshLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(root, root)
            addView(webView)
            setOnRefreshListener {
                webView.reload()
            }
        }

        val container = android.widget.FrameLayout(this)
        container.addView(swipeRefresh)
        container.addView(progressBar)
        setContentView(container)

        setupWebView()
        webView.loadUrl(startUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings

        // --- Pengaturan inti biar YouTube jalan ---
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true

        // --- Optimasi memori & CPU untuk HP low-end Android 7 ---
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        settings.mediaPlaybackRequiresUserGesture = true // video tidak autoplay sendiri, hemat data & CPU
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.textZoom = 100

        // Matikan fitur yang tidak perlu buat nonton YT, biar ringan
        settings.saveFormData = false
        settings.setGeolocationEnabled(false)
        settings.allowContentAccess = false
        settings.allowFileAccess = false

        // User-Agent mobile biasa (bukan desktop) supaya YouTube kasih versi ringan m.youtube.com
        settings.userAgentString = settings.userAgentString
            .replace("; wv", "") // hilangkan tanda WebView agar beberapa fitur YT tidak dibatasi

        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = false

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                // Semua link YouTube & Google auth dibuka di dalam WebView ini
                return if (url.contains("youtube.com") || url.contains("youtu.be") ||
                    url.contains("google.com") || url.contains("accounts.google")
                ) {
                    false
                } else {
                    // Link keluar (misal share ke app lain) dilempar ke browser luar
                    try {
                        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: Exception) {
                        Toast.makeText(this@MainActivity, "Tidak bisa membuka link", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
                progressBar.visibility = View.GONE
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress >= 95) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    // Tombol back fisik -> mundur di riwayat WebView dulu, baru keluar app
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        // Hemat baterai/CPU saat app di-background: pause semua JS timer (termasuk video)
        webView.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
