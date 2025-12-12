package com.example.benchmark

import android.content.Context
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import junit.framework.TestCase.fail
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * This is an example startup benchmark.
 *
 * It navigates to the device's home screen, and launches the default activity.
 *
 * Before running this benchmark:
 * 1) switch your app's active build variant in the Studio (affects Studio runs only)
 * 2) add `<profileable android:shell="true" />` to your app's manifest, within the `<application>` tag
 *
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance.
 */

const val DEFAULT_ITERATIONS = 72 + 72
const val DEFAULT_WAIT_SESSION = 3000L
const val DEFAULT_WAIT_URL_WARM = 1500L
const val DEFAULT_WAIT_URL_LOAD = 15000L
const val DEFAULT_WAIT_WRITE_TO_FILE = 1000L
const val DNS_CACHE_WAIT_TIME = 3600_000L // 60 minutes to ensure DNS cache expires
const val PACKAGE_NAME = "com.example.browser_load_example"
var urlsToLoad = listOf(
    "https://www.google.com",
    "https://www.example.com",
    "https://www.wikipedia.org",
    "https://www.github.com",
    "https://www.stackoverflow.com",
    "https://www.forhers.com/lp/wl-start-hers-glp1-injections-wegovy-pricing",
    "https://www.hims.com/lp/hair-start-comparison-dlp",
    "https://www.alchemer.com/software/",
    "https://www.writ.so/",
    "https://www.grammarly.com/",
    "https://chatgpt.com/",
    "https://contrave.com/",
    "https://www.americanexpress.com/",
    "https://lp.spinfinite.com/",
    "https://www.interactivebrokers.com/",
    "https://www.nerdwallet.com/",
    "https://www.hitchswitch.com/",
    "https://www.mazdausa.com/vehicles/cx-90",
    "https://www.bloomberg.com/subscriptions/uspaid",
    "https://redis.io/cloud",
    "https://www.slack.com",
    "https://www.dropbox.com",
    "https://www.spotify.com",
    "https://www.netflix.com",
    "https://www.amazon.com",
    "https://www.facebook.com",
    "https://www.twitter.com",
    "https://www.instagram.com",
    "https://www.espn.com",
    "https://www.baidu.com",
    "https://www.nike.com",
    "https://www.adidas.com",
    "https://www.puma.com",
    "https://www.underarmour.com",
    "https://www.reebok.com",
    "https://www.newbalance.com",
    "https://www.asics.com",
    "https://www.converse.com",
    "https://www.vans.com",
    "https://www.fila.com",
    "https://www.keds.com",
    "https://www.timberland.com",
    "https://www.skechers.com",
    "https://www.dcshoes.com",
    "https://www.etnies.com",
    "https://www.osiris.com",
    "https://www.famousfootwear.com",
    "https://www.finishline.com",
    "https://www.hibbett.com",
    "https://www.sportsauthority.com",
    "https://www.dickssportinggoods.com",
    "https://www.academy.com",
    "https://www.cabelas.com",
    "https://www.basspro.com",
    "https://www.rei.com",
    "https://www.backcountry.com",
    "https://www.mountainhardwear.com",
    "https://www.columbia.com",
    "https://www.patagonia.com",
    "https://www.theNorthFace.com",
    "https://www.marmot.com",
    "https://www.eddiebauer.com",
    "https://www.nba.com",
    "https://www.nhl.com",
    "https://www.mlb.com",
    "https://www.nfl.com",
    "https://www.wwe.com",
    "https://www.bleacherreport.com",
    "https://www.sportsillustrated.com",
    "https://www.foxsports.com",
    "https://www.cbssports.com",
    "https://www.nbcsports.com",
    "https://www.primevideo.com",
    "https://www.prada.com",
    "https://www.gucci.com",
    "https://www.louisvuitton.com",
    "https://www.chanel.com",
    "https://www.dior.com",
    "https://www.burberry.com",
)
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ExampleStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    lateinit var instrumentationContext: Context

    @Test
    fun c_webview_load_test() {
        var fileName = "webview"
        var testURLs = (urlsToLoad + urlsToLoad).toMutableList()
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = DEFAULT_ITERATIONS,
            startupMode = StartupMode.COLD,
            setupBlock = {
                instrumentationContext = InstrumentationRegistry.getInstrumentation().context
                pressHome()
            }
        ) {
            val intent = Intent("$PACKAGE_NAME.MAIN_ACTIVITY")
            if (testURLs.isNotEmpty()) {
                intent.putExtra("extra_url_to_use", testURLs.removeAt(0))
            }
            startActivityAndWait(intent)
            Thread.sleep(DEFAULT_WAIT_URL_WARM)
            clickOnId("open_web_view")
            Thread.sleep(DEFAULT_WAIT_URL_LOAD) // Wait for the web view to load
            saveFile(fileName)
            Thread.sleep(DEFAULT_WAIT_WRITE_TO_FILE) // Wait for the file to be saved
        }
    }

    @Test
    fun a_webview_preconnect_load_test() {
        var fileName = "webview_preconnect"
        var testURLs = (urlsToLoad + urlsToLoad).toMutableList()
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = DEFAULT_ITERATIONS,
            startupMode = StartupMode.COLD,
            setupBlock = {
                instrumentationContext = InstrumentationRegistry.getInstrumentation().context
                pressHome()
            }
        ) {
            val intent = Intent("$PACKAGE_NAME.MAIN_ACTIVITY")
            if (testURLs.isNotEmpty()) {
                intent.putExtra("extra_url_to_use", testURLs.removeAt(0))
            }
            intent.putExtra("prewarm_url", true)
            startActivityAndWait(intent)
            Thread.sleep(DEFAULT_WAIT_URL_WARM)
            clickOnId("open_web_view")
            Thread.sleep(DEFAULT_WAIT_URL_LOAD) // Wait for the web view to load
            saveFile(fileName)
            Thread.sleep(DEFAULT_WAIT_WRITE_TO_FILE) // Wait for the file to be saved
        }

        // Wait for DNS cache to expire before next test
        Thread.sleep(DNS_CACHE_WAIT_TIME)
    }

    @Test
    fun b_webview_preconnect_with_cct_load_test() {
        var fileName = "webview_preconnect_with_cct"
        var testURLs = (urlsToLoad + urlsToLoad).toMutableList()
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = DEFAULT_ITERATIONS,
            startupMode = StartupMode.COLD,
            setupBlock = {
                instrumentationContext = InstrumentationRegistry.getInstrumentation().context
                pressHome()
            }
        ) {
            val intent = Intent("$PACKAGE_NAME.MAIN_ACTIVITY")
            if (testURLs.isNotEmpty()) {
                intent.putExtra("extra_url_to_use", testURLs.removeAt(0))
            }
            intent.putExtra("cct_prewarm_url", true)
            startActivityAndWait(intent)
            Thread.sleep(DEFAULT_WAIT_URL_WARM)
            clickOnId("open_web_view")
            Thread.sleep(DEFAULT_WAIT_URL_LOAD) // Wait for the web view to load
            saveFile(fileName)
            Thread.sleep(DEFAULT_WAIT_WRITE_TO_FILE) // Wait for the file to be saved
        }

        // Wait for DNS cache to expire before next test
        Thread.sleep(DNS_CACHE_WAIT_TIME)
    }

    @Test
    fun d_webview_preconnect_with_okhttp_load_test() {
        var fileName = "webview_preconnect_with_okhttp"
        var testURLs = (urlsToLoad + urlsToLoad).toMutableList()
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = DEFAULT_ITERATIONS,
            startupMode = StartupMode.COLD,
            setupBlock = {
                instrumentationContext = InstrumentationRegistry.getInstrumentation().context
                pressHome()
            }
        ) {
            val intent = Intent("$PACKAGE_NAME.MAIN_ACTIVITY")
            if (testURLs.isNotEmpty()) {
                intent.putExtra("extra_url_to_use", testURLs.removeAt(0))
            }
            intent.putExtra("okhttp_prewarm_url", true)
            startActivityAndWait(intent)
            Thread.sleep(DEFAULT_WAIT_URL_WARM)
            clickOnId("open_web_view")
            Thread.sleep(DEFAULT_WAIT_URL_LOAD) // Wait for the web view to load
            saveFile(fileName)
            Thread.sleep(DEFAULT_WAIT_WRITE_TO_FILE) // Wait for the file to be saved
        }

        // Wait for DNS cache to expire before next test
        Thread.sleep(DNS_CACHE_WAIT_TIME)
    }

    private fun MacrobenchmarkScope.saveFile(fileName: String) {
        val intent = Intent("$PACKAGE_NAME.PRINT_FILE")
        intent.putExtra("extra_log_to_file_name", fileName)
        startActivityAndWait(intent)
    }

    private fun MacrobenchmarkScope.clickOnId(resourceId: String) {
        val selector = By.res(PACKAGE_NAME, resourceId)
        if (!device.wait(Until.hasObject(selector), 2_500)) {
            fail("Did not find object with id $resourceId")
        }

        device
            .findObject(selector)
            .click()
        // Chill to ensure we capture the end of the click span in the trace.
        Thread.sleep(100)
    }

}