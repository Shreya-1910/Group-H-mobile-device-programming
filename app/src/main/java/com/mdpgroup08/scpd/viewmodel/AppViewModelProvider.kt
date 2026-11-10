package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mdpgroup08.scpd.ScpdApplication

/**
 * One factory that creates every ViewModel with its repositories.
 * In a screen: val vm: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)
 *
 * Everyone: add one initializer line here for each new ViewModel, e.g.
 *   initializer { MarketplaceViewModel(scpdApplication().container.eventRepository) }
 */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { LoginViewModel(scpdApplication().container.authRepository) }
        initializer { SessionViewModel(scpdApplication().container.authRepository) }
    }
}

/** Gets our ScpdApplication inside an initializer. */
fun CreationExtras.scpdApplication(): ScpdApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ScpdApplication
