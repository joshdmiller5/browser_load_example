package com.example.browser_load_example

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import androidx.core.os.OutcomeReceiverCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.webkit.PrefetchException
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.SpeculativeLoadingParameters
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewStartUpConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.chromium.net.CronetEngine
import java.net.URL
import kotlin.coroutines.resume
import androidx.core.net.toUri
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps


class MainActivity : AppCompatActivity() {
    private var url: String = "https://www.americanexpress.com/"

    private val dnsOverHttps by lazy {
        val bootstrapClient = OkHttpClient.Builder().build()

        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://dns.google/dns-query".toHttpUrl())
            .build()
    }

    @OptIn(WebViewCompat.ExperimentalAsyncStartUp::class)
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
        val shouldPrewarmWithCct = intent.getBooleanExtra("cct_prewarm_url", false)
        val shouldPrewarmWithOkhttp = intent.getBooleanExtra("okhttp_prewarm_url", false)


        findViewById<Button>(R.id.open_web_view).setOnClickListener {
            lifecycleScope.launch {
                // Add delay to ensure UI is warmed up evenly
                kotlinx.coroutines.delay(1000)

                if (shouldPrewarmWithOkhttp) {
                    // DNS-over-HTTPS preconnect using OkHttp
                    withContext(Dispatchers.IO) {
                        val host = URL(url).host
                        dnsOverHttps.lookup(host)
                    }
                } else if (shouldPrewarmWithCct) {
                    // CCT-based preconnect
                    withContext(Dispatchers.Main) {
                        val cctSession = withTimeoutOrNull(5000) {
                            bindCustomTabsService()
                        }
                        
                        if (cctSession != null) {
                            // Give warmup time to complete
                            kotlinx.coroutines.delay(500)
                            
                            // Trigger preconnect
                            cctSession.mayLaunchUrl(url.toUri(), null, null)
                        }
                    }
                } else if (shouldPrewarm) {
                    // DNS-only preconnect on IO thread
                    withContext(Dispatchers.IO) {
                        val host = URL(url).host
                        java.net.InetAddress.getAllByName(host)

                    }
                }

                kotlinx.coroutines.delay(2000)
                // Switch to main thread to start activity
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@MainActivity, WebViewActivity::class.java)
                    intent.putExtra(WebViewActivity.EXTRA_URL_TO_USE, url)
                    SessionHolder.startTimer()
                    startActivity(intent)
                }
            }
        }
    }

    private suspend fun bindCustomTabsService(): CustomTabsSession? =
        suspendCancellableCoroutine { continuation ->
            var client: CustomTabsClient? = null

            val connection = object : CustomTabsServiceConnection() {
                override fun onCustomTabsServiceConnected(
                    name: ComponentName,
                    customTabsClient: CustomTabsClient
                ) {
                    client = customTabsClient
                    customTabsClient.warmup(0)
                    val session = customTabsClient.newSession(CustomTabsCallback())

                    if (continuation.isActive) {
                        continuation.resume(session)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    client = null
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }

            val bound = CustomTabsClient.bindCustomTabsService(
                this,
                "com.android.chrome",
                connection
            )

            if (!bound && continuation.isActive) {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                try {
                    unbindService(connection)
                } catch (e: Exception) {
                    // Service might not be bound
                }
            }
        }

    companion object {
        const val EXTRA_URL_TO_USE = "extra_url_to_use"
    }
}