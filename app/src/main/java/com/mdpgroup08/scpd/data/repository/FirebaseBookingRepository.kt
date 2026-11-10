package com.mdpgroup08.scpd.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseBookingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : BookingRepository {

    override suspend fun book(eventId: String) {
        val uid = auth.currentUser?.uid ?: throw BookingException.NotLoggedIn
        val eventRef = db.collection("events").document(eventId)
        val bookingRef = db.collection("bookings").document("${eventId}_$uid")

        db.runTransaction { tx ->
            val event = tx.get(eventRef).toObject(Event::class.java)
                ?: throw BookingException.EventNotFound
            val existing = tx.get(bookingRef)

            if (event.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
            if (existing.exists()) throw BookingException.AlreadyBooked
            if (event.bookedCount >= event.capacity) throw BookingException.EventFull

            tx.update(eventRef, "bookedCount", FieldValue.increment(1))
            tx.set(
                bookingRef,
                Booking(
                    eventId = eventId,
                    studentId = uid,
                    status = BookingStatus.CONFIRMED,
                    createdAt = System.currentTimeMillis()
                )
            )
            null
        }.await()
    }

    override fun getMyBooking(eventId: String): Flow<Booking?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val reg = db.collection("bookings").document("${eventId}_$uid")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObject(Booking::class.java))
            }
        awaitClose { reg.remove() }
    }
}