package com.valerie.yurei.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.valerie.yurei.ui.navigation.RootNav

class GameViewModelFactory(
    private val application: Application,
    private val rootNav: RootNav
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return GameViewModel(application, rootNav) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
