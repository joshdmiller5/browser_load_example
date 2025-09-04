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
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewFeature.PRECONNECT
import com.example.browser_load_example.WebViewActivity.Companion.URL_PRECONNECT


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
        val shouldPreconnect = intent .getBooleanExtra("use_preconnect", false)

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
            } else {
                val intent = Intent(this, WebViewActivity::class.java)
                intent.putExtra(WebViewActivity.EXTRA_URL_TO_USE, url)
                SessionHolder.startTimer()
                startActivity(intent)
            }

        }
    }



    companion object {
        const val EXTRA_URL_TO_USE = "extra_url_to_use"
        const val EXTRA_SHOULD_WARMUP = "extra_should_warmup"
        const val EXTRA_SHOULD_PREWARM_URL = "extra_should_prewarm_url"
    }
}