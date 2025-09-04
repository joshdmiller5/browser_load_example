package com.example.browser_load_example

import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.OutcomeReceiverCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.PrefetchException
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.SpeculativeLoadingParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {
    private var url: String = "https://www.americanexpress.com/"

    @OptIn(Profile.ExperimentalUrlPrefetch::class, Profile.ExperimentalPreconnect::class, Profile.ExperimentalWarmUpRendererProcess::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent.getStringExtra(EXTRA_URL_TO_USE)?.let {
            url = it
        } ?: run {
            Log.d("MainActivity", "No URL provided, using default URL")
        }

        val shouldPrewarm = intent.getBooleanExtra("prewarm_url", false)
        val shouldPreconnect = intent.getBooleanExtra("use_preconnect", false)
        val shouldPrefetchAndCache = intent.getBooleanExtra("prefetch_and_cache", false)


        findViewById<Button>(R.id.open_web_view).setOnClickListener {
            if (shouldPreconnect) {
                val profileStore = ProfileStore.getInstance()
                val profile = profileStore.getOrCreateProfile("TEST_PROFILE_1")
                profile.warmUpRendererProcess()
                profile.prefetchUrlAsync(
                    url,
                    CancellationSignal(),
                    ContextCompat.getMainExecutor(this),
                    SpeculativeLoadingParameters.Builder().setJavaScriptEnabled(true).build(),
                    object : OutcomeReceiverCompat<Void, PrefetchException> {
                        override fun onResult(result: Void?) {
                            val intent = Intent(this@MainActivity, WebViewActivity::class.java)
                            intent.putExtra(WebViewActivity.EXTRA_URL_TO_USE, url)
                            intent.putExtra(WebViewActivity.URL_PRECONNECT, true)
                            SessionHolder.startTimer()
                            startActivity(intent)
                        }

                        override fun onError(error: PrefetchException) {
                            val intent = Intent(this@MainActivity, WebViewActivity::class.java)
                            intent.putExtra(WebViewActivity.EXTRA_URL_TO_USE, url)
                            intent.putExtra(WebViewActivity.URL_PRECONNECT, true)
                            SessionHolder.startTimer()
                            startActivity(intent)
                        }
                    }
                )

            }
            else if (shouldPrewarm) {
                var hasPrewarmed = true
                val webView = WebView(this)
                webView.webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress == 100 && hasPrewarmed) {
                            hasPrewarmed = false
                            val intent = Intent(this@MainActivity, WebViewActivity::class.java)
                            intent.putExtra(WebViewActivity.EXTRA_URL_TO_USE, url)
                            intent.putExtra(WebViewActivity.PREWARM, true)
                            SessionHolder.startTimer()
                            startActivity(intent)
                        }
                    }
                }
                webView.loadUrl(url)
            } else if (shouldPrefetchAndCache) {
                Log.d("MainActivity", "Starting prefetch of: $url")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val html = connection.inputStream.bufferedReader().use { it.readText() }
                            HtmlCache.cacheHtml(url, html)
                            // Use cached content
                            val intent = Intent(this@MainActivity, WebViewActivity::class.java)
                            intent.putExtra(WebViewActivity.EXTRA_URL_TO_USE, url)
                            intent.putExtra("use_cached_content", true)
                            SessionHolder.startTimer()
                            startActivity(intent)
                            Log.d("MainActivity", "Successfully cached ${html.length} characters for $url")
                        } else {
                            Log.e("MainActivity", "Failed to fetch $url, response code: $responseCode")
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error prefetching $url", e)
                    }
                }
            } else {
                val intent = Intent(this, WebViewActivity::class.java)
                intent.putExtra(WebViewActivity.EXTRA_URL_TO_USE, url)
                SessionHolder.startTimer()
                startActivity(intent)
            }

        }
    }

    object HtmlCache {
        private var cachedUrl: String? = null
        private var cachedHtml: String? = null
        
        fun cacheHtml(url: String, html: String) {
            cachedUrl = url
            cachedHtml = html
        }
        
        fun getCachedHtml(url: String): String? {
            return if (cachedUrl == url) cachedHtml else null
        }
        
        fun clearCache() {
            cachedUrl = null
            cachedHtml = null
        }
    }

    companion object {
        const val EXTRA_URL_TO_USE = "extra_url_to_use"
        const val EXTRA_SHOULD_WARMUP = "extra_should_warmup"
        const val EXTRA_SHOULD_PREWARM_URL = "extra_should_prewarm_url"
    }
}