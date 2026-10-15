package com.mas.mobile.presentation.activity.fragment

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mas.mobile.R
import com.mas.mobile.presentation.activity.MainActivity
import com.mas.mobile.presentation.viewmodel.validator.Action

abstract class ListFragment: CommonFragment() {

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.standartd_list_menu_add -> {
                val action = resolveAddButtonDestination()
                this.findNavController().navigate(action)
                true
            }
            R.id.standartd_list_menu_settings -> {
                this.findNavController().navigate(R.id.action_to_settings)
                true
            }
            R.id.budget_analytics -> go(R.id.action_to_analytics)
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated(
        message = "The bottom menu hides automatically anyway",
        replaceWith = ReplaceWith("See MainActivity.setupBottomNavMenu()"),
        level = DeprecationLevel.WARNING
    )
    fun hideBottomMenu() {
        updateMenu(false)
    }

    @Deprecated(
        message = "The bottom menu hides automatically anyway",
        replaceWith = ReplaceWith("See MainActivity.setupBottomNavMenu()"),
        level = DeprecationLevel.WARNING
    )
    fun showBottomMenu() {
        updateMenu(true)
    }

    protected fun setTitle(title: (title: String) -> String) {
        (this.requireActivity() as MainActivity).supportActionBar?.let {
            it.title = title(it.title.toString())
        }
    }

    private fun updateMenu(isEnabled: Boolean) {
        this.activity?.let {
            it.findViewById<BottomNavigationView>(R.id.nav_bottom_view)?.menu?.setGroupVisible(0, isEnabled)
        }
    }

    abstract fun resolveAddButtonDestination(): NavDirections
}