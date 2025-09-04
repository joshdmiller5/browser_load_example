package com.example.browser_load_example

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewCompat

class WebViewActivity : AppCompatActivity() {
    private var notSentYet = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_web_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val myWebView: WebView = findViewById(R.id.webview)
        val shouldUseCache = intent.getBooleanExtra(PREWARM, false)
        val shouldPreconnect = intent.getBooleanExtra(URL_PRECONNECT, false)
        val useCachedContent = intent.getBooleanExtra("use_cached_content", false)
        val urlToLoad = intent.getStringExtra(EXTRA_URL_TO_USE)!!
        
        myWebView.settings.cacheMode = if (shouldUseCache || shouldPreconnect) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_NO_CACHE
        myWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true

            // Additional performance settings
            blockNetworkImage = false
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            // Enable modern web features
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        myWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }
        myWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100 && notSentYet) {
                    SessionHolder.browserPageLoaded()
                    notSentYet = false
                }
            }
        }
        if (shouldPreconnect) {
            WebViewCompat.setProfile(myWebView, "TEST_PROFILE_1")
        }
        
        // Load content based on the test type
        if (useCachedContent) {
            // Try to load cached HTML content
            val cachedHtml = MainActivity.HtmlCache.getCachedHtml(urlToLoad)
            if (cachedHtml != null) {
                myWebView.loadDataWithBaseURL(urlToLoad, cachedHtml, "text/html", "UTF-8", null)
            } else {
                // Fallback to regular URL loading if no cached content
                myWebView.loadUrl(urlToLoad)
            }
        } else {
            myWebView.loadUrl(urlToLoad)
        }
    }

    companion object {
        const val EXTRA_URL_TO_USE = "extra_url_to_use"
        const val PREWARM = "should_prewarm"
        const val URL_PRECONNECT = "should_preconnect"
    }
}