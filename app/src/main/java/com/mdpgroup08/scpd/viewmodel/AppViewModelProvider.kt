
package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mdpgroup08.scpd.ScpdApplication
import androidx.lifecycle.createSavedStateHandle

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
                c.authRepository,
                c.networkMonitor
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
        initializer {
            val c = scpdApplication().container
            AdminApprovalViewModel(c.eventRepository, c.authRepository, c.networkMonitor)
        }

        initializer {
            val c = scpdApplication().container
            MarketplaceViewModel(c.eventRepository, c.networkMonitor)
        }

        initializer {
            val c = scpdApplication().container
            EventDetailsViewModel(
                createSavedStateHandle(),
                c.eventRepository,
                c.bookingRepository,
                c.networkMonitor
            )
        }

        initializer {
            val c = scpdApplication().container
            StudentRemindersViewModel(c.bookingRepository, c.eventRepository, c.reminderScheduler)
        }

        initializer {
            val c = scpdApplication().container
            StudentEventsViewModel(c.bookingRepository, c.eventRepository, c.networkMonitor)
        }

        initializer {
            val c = scpdApplication().container
            ForYouViewModel(c.eventRepository, c.bookingRepository)
        }

        // ---- Secure check-in (R3, R4) ----
        initializer {
            val c = scpdApplication().container
            ClubCheckInViewModel(c.eventRepository, c.authRepository)
        }

        initializer {
            val c = scpdApplication().container
            ShowCodeViewModel(createSavedStateHandle(), c.eventRepository, c.attendanceRepository, c.authRepository)
        }

        initializer {
            val c = scpdApplication().container
            AttendanceViewModel(createSavedStateHandle(), c.eventRepository, c.attendanceRepository, c.authRepository)
        }

        initializer {
            val c = scpdApplication().container
            StudentCheckInViewModel(
                c.bookingRepository,
                c.eventRepository,
                c.attendanceRepository,
                c.authRepository,
                c.networkMonitor,
                deviceId = { c.deviceHelper.getDeviceId() }
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

