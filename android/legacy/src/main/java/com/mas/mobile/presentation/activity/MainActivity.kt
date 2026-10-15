package com.mas.mobile.presentation.activity

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.mas.mobile.R
import com.mas.mobile.appComponent
import com.mas.mobile.domain.message.MessageRepository
import com.mas.mobile.domain.settings.SettingsService
import com.mas.mobile.service.AppUpdateCheckWorker
import com.mas.mobile.service.DateListener
import com.mas.mobile.service.ScheduledSpendingWorker
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class MainActivity : AppCompatActivity() {
    private val dateListener = DateListener()
    private lateinit var appBarConfig : AppBarConfiguration

    @Inject
    lateinit var messageRepository: MessageRepository
    @Inject
    lateinit var settingsService: SettingsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.appComponent.injectMainActivity(this)

        checkPrivacyPolicy()

        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.toolbar))

        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_view) as NavHostFragment).navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph).also {
            it.setStartDestination(R.id.nav_expenditure_list)
        }
        navController.graph = graph

        val rootFragments = setOf(
            R.id.nav_expenditure_list,
            R.id.nav_spending_list,
            R.id.nav_message_list_fragment ,
            R.id.nav_menu
        )

        setupActionBar(navController, rootFragments)
        setupBottomNavMenu(navController, rootFragments)
        setupHamburgerNavMenu(navController)

        this.applicationContext.registerReceiver(dateListener, IntentFilter(Intent.ACTION_TIME_TICK))

        startWorkManagers()
    }

    override fun onDestroy() {
        this.applicationContext.unregisterReceiver(dateListener)
        super.onDestroy()
    }

    private fun checkPrivacyPolicy() {
        if (settingsService.needToShowPolicy())  {
            Intent(this, PolicyActivity::class.java).also { startActivity(it) }
        }
    }

    private fun setupActionBar(navController: NavController, rootFragments: Set<Int>) {
        val drawerLayout : DrawerLayout? = findViewById(R.id.drawer_layout)
        appBarConfig = AppBarConfiguration(rootFragments, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfig)
    }

    private fun setupHamburgerNavMenu(navController: NavController) {
       val sideNavView = findViewById<NavigationView>(R.id.nav_view)
       sideNavView?.setupWithNavController(navController)
    }

    private fun setupBottomNavMenu(navController: NavController, rootFragments: Set<Int>) {
        val bottomNavMenu = findViewById<BottomNavigationView>(R.id.nav_bottom_view) ?: return

        with(bottomNavMenu) {
            setupWithNavController(navController)

            // Do not keep stack for every tab
            setOnItemReselectedListener {
                navController.popBackStack(it.itemId, inclusive = false)
            }
            setOnItemSelectedListener {
                NavigationUI.onNavDestinationSelected(it, navController, false)
                true
            }
            navController.addOnDestinationChangedListener { _, destination, _ ->
                visibility = if (destination.id in rootFragments) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            messageRepository.countUnreadLive(LocalDate.now().minusDays(30)).observeForever {
                val badge = getOrCreateBadge(R.id.nav_message_list_fragment)
                if (it > 0) {
                    badge.number = it
                    badge.isVisible = true
                } else {
                    badge.isVisible = false
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val retValue = super.onCreateOptionsMenu(menu)

        //menuInflater.inflate(R.menu.actions_menu, menu)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        if (navigationView == null) {
            menuInflater.inflate(R.menu.bottom_menu, menu)
            return true
        }

        return retValue
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(findNavController(R.id.nav_host_view))
                || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_view).navigateUp(appBarConfig)
    }

    private fun startWorkManagers() {
        val updateCheckRequest = PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(2, TimeUnit.HOURS).build()
        WorkManager.getInstance(this.applicationContext).enqueueUniquePeriodicWork(
            "periodicUpdateCheck",
            ExistingPeriodicWorkPolicy.REPLACE,
            updateCheckRequest
        )

        val scheduledSpendingRequest = PeriodicWorkRequestBuilder<ScheduledSpendingWorker>(2, TimeUnit.HOURS).build()
        WorkManager.getInstance(this.applicationContext).enqueueUniquePeriodicWork(
            "scheduledSpending",
            ExistingPeriodicWorkPolicy.REPLACE,
            scheduledSpendingRequest
        )
    }
}
