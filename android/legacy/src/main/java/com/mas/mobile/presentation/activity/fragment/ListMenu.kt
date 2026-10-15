package com.mas.mobile.presentation.activity.fragment

import android.view.MenuItem

interface ListMenu<T> {
    fun onRowMenuSelected(menuItem: MenuItem, item: T): Boolean
}