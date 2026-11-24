
package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mdpgroup08.scpd.ScpdApplication

/**
 * Creates every ViewModel with its repositories.
 */
object AppViewModelProvider {

    val Factory: ViewModelProvider.Factory = viewModelFactory {

        initializer {
            LoginViewModel(
                scpdApplication().container.authRepository
            )
        }

        initializer {
            RegisterViewModel(
                scpdApplication().container.authRepository
            )
        }

        initializer {
            SessionViewModel(
                scpdApplication().container.authRepository
            )
        }

        initializer {
            val c = scpdApplication().container

            CreateEventViewModel(
                c.eventRepository,
                c.authRepository,
                c.networkMonitor
            )
        }

        initializer {
            val c = scpdApplication().container

            MyEventsViewModel(
                c.eventRepository,
                c.authRepository
            )
        }

        initializer {
            val c = scpdApplication().container

            AdminViewModel(
                c.adminRepository,
                c.authRepository
            )
        }

        initializer {
            val c = scpdApplication().container

            ApprovalQueueViewModel(
                c.eventRepository,
                c.authRepository,
                c.networkMonitor
            )
        }

        // Offline check-in ViewModel
        initializer {
            val c = scpdApplication().container

            CheckInViewModel(
                c.checkInRepository
            )
        }
    }
}

/**
 * Gets our ScpdApplication inside an initializer.
 */
fun CreationExtras.scpdApplication(): ScpdApplication =
    this[
        ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
    ] as ScpdApplication

