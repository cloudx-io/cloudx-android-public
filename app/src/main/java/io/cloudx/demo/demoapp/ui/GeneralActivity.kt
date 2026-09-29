package io.cloudx.demo.demoapp.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.cloudx.demo.demoapp.DemoLog
import io.cloudx.demo.demoapp.R
import io.cloudx.demo.demoapp.ads.CloudXInit
import io.cloudx.demo.demoapp.ui.screens.AppOpenFragment
import io.cloudx.demo.demoapp.ui.screens.BannerFragment
import io.cloudx.demo.demoapp.ui.screens.InterstitialFragment
import io.cloudx.demo.demoapp.ui.screens.MrecFragment
import io.cloudx.demo.demoapp.ui.screens.NativeFragment
import io.cloudx.demo.demoapp.ui.screens.RewardedFragment
import io.cloudx.sdk.CloudX
import kotlin.reflect.KClass

class GeneralActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var toolbar: Toolbar
    private lateinit var bottomNavBar: BottomNavigationView
    private lateinit var appSubtitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*
         * Called on every create, recreation included: after process death the screen comes back
         * with saved state but an uninitialized SDK.
         */
        CloudXInit.initialize(applicationContext, listener = null)

        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }
        appSubtitle = "Demo App v$versionName"
        supportActionBar?.subtitle = appSubtitle

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            view.setPadding(
                view.paddingLeft,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        bottomNavBar = findViewById(R.id.bottom_nav)
        bottomNavBar.setup()
        /*
         * On recreation the bar restores its own selection and FragmentManager restores the open
         * screen and back stack, so only a fresh start opens Banner.
         */
        if (savedInstanceState == null) {
            bottomNavBar.selectedItemId = R.id.menu_banner
        }

        supportFragmentManager.addOnBackStackChangedListener(::updateToolbarForScreen)
        updateToolbarForScreen()
    }

    /**
     * Programmatically selects a bottom nav tab. Used by [openMoreDestination].
     */
    fun selectBottomNavTab(menuItemId: Int) {
        if (bottomNavBar.selectedItemId == menuItemId) {
            bottomNavBar.menu.findItem(menuItemId)?.let(::navigateToBottomNavItem)
        } else {
            bottomNavBar.selectedItemId = menuItemId
        }
    }

    /**
     * Opens [destination] from the More tab: selects the tab and opens the screen on top of the
     * More list, so Back returns to the list. A screen that is already showing is left as it is.
     */
    fun openMoreDestination(destination: MoreDestination) {
        val screen = createMoreScreen(destination)
        if (screen == null) {
            showMediationDebugger()
            return
        }
        if (supportFragmentManager.isStateSaved) {
            DemoLog.w(TAG, "openMoreDestination($destination) skipped: fragment state already saved")
            return
        }
        val tag = destination.name
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment?.tag == tag) {
            return
        }

        selectBottomNavTab(R.id.menu_more)
        pushScreen(screen.fragmentClass, tag)
    }

    /**
     * Shows the open screen's name and an Up arrow while a More screen is open, and the app name and
     * version otherwise.
     */
    private fun updateToolbarForScreen() {
        val fragmentManager = supportFragmentManager
        val screenTitle = (fragmentManager.backStackEntryCount - 1 downTo 0)
            .firstNotNullOfOrNull { screenTitle(fragmentManager.getBackStackEntryAt(it).name) }
        supportActionBar?.apply {
            title = screenTitle ?: getString(R.string.app_name)
            subtitle = if (screenTitle == null) appSubtitle else null
            setDisplayHomeAsUpEnabled(screenTitle != null)
        }
    }

    private fun screenTitle(backStackEntryName: String?): String? =
        MoreDestination.entries.firstOrNull { it.name == backStackEntryName }?.let { getString(it.titleRes) }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun pushScreen(fragmentClass: KClass<out Fragment>, tag: String) {
        supportFragmentManager.commit {
            setCustomAnimations(
                com.google.android.material.R.anim.abc_fade_in,
                com.google.android.material.R.anim.abc_fade_out,
                com.google.android.material.R.anim.abc_fade_in,
                com.google.android.material.R.anim.abc_fade_out,
            )
            replace(R.id.fragment_container, fragmentClass.java, null, tag)
            addToBackStack(tag)
        }
    }

    private fun BottomNavigationView.setup() {
        with(this) {
            setOnItemSelectedListener {
                navigateToBottomNavItem(it)
                // Respond to all navigation item clicks
                true
            }
            setOnItemReselectedListener(::navigateToBottomNavItem)
        }
    }

    private fun navigateToBottomNavItem(item: MenuItem) {
        val destination = createBottomNavDestination(item.itemId) ?: return
        val tag = item.itemId.toString()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment?.tag == tag) {
            return
        }

        /*
         * A fullscreen ad activity (interstitial/rewarded) in the foreground puts this
         * activity past onSaveInstanceState; popBackStackImmediate/commit would then
         * throw IllegalStateException and crash the app, so skip instead. A user can't
         * navigate a background UI.
         */
        if (supportFragmentManager.isStateSaved) {
            DemoLog.w(TAG, "navigateToBottomNavItem($tag) skipped: fragment state already saved")
            return
        }

        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit {
            setCustomAnimations(
                com.google.android.material.R.anim.abc_fade_in,
                com.google.android.material.R.anim.abc_fade_out
            )
            replace(R.id.fragment_container, destination.fragmentClass.java, null, tag)
        }
    }

    private fun createBottomNavDestination(itemId: Int): Screen? = when (itemId) {
        R.id.menu_banner -> Screen(BannerFragment::class)
        R.id.menu_mrec -> Screen(MrecFragment::class)
        R.id.menu_interstitial -> Screen(InterstitialFragment::class)
        R.id.menu_rewarded -> Screen(RewardedFragment::class)
        R.id.menu_more -> Screen(MoreFragment::class)
        else -> null
    }

    /**
     * The screen [destination] opens, or null for [MoreDestination.MEDIATION_DEBUGGER], which
     * opens the SDK's own debugger activity instead.
     */
    private fun createMoreScreen(destination: MoreDestination): Screen? = when (destination) {
        MoreDestination.APP_OPEN -> Screen(AppOpenFragment::class)
        MoreDestination.NATIVE -> Screen(NativeFragment::class)
        MoreDestination.MEDIATION_DEBUGGER -> null
    }

    private fun showMediationDebugger() {
        if (CloudX.isInitialized()) {
            CloudX.showMediationDebugger(this)
        } else {
            shortSnackbar(bottomNavBar, "CloudX SDK is not initialized yet")
        }
    }

    private data class Screen(val fragmentClass: KClass<out Fragment>)

    private companion object {
        const val TAG = "GeneralActivity"
    }
}
