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

/**
 * Bookings stored in Firestore at bookings/{eventId}_{studentId} (R2-S4).
 * The document ID allows only one booking per student per event.
 */
class FirebaseBookingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : BookingRepository {

    /**
     * One transaction: check the event is approved and has a place, then add the booking and
     * increase bookedCount together, so capacity is never exceeded even if two students book
     * the last place at the same moment (R2-S4, R2-S6).
     */
    override suspend fun book(eventId: String) {
        val uid = auth.currentUser?.uid ?: throw BookingException.NotLoggedIn
        val eventRef = db.collection("events").document(eventId)
        val bookingRef = db.collection("bookings").document("${eventId}_$uid")

        try {
            db.runTransaction { tx ->
                val event = tx.get(eventRef).toObject(Event::class.java)
                    ?: throw BookingException.EventNotFound
                val existing = tx.get(bookingRef).toObject(Booking::class.java)

                if (event.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
                // A cancelled booking can be booked again; only a confirmed one blocks it
                if (existing?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
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
        } catch (e: Exception) {
            // Firestore may wrap our exception, so unwrap it for the ViewModel
            throw (e as? BookingException) ?: (e.cause as? BookingException) ?: e
        }
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

    /** The signed-in student's confirmed bookings, used for clash checks (R2-S7). */
    override fun getMyBookings(): Flow<List<Booking>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = db.collection("bookings")
            .whereEqualTo("studentId", uid)
            .whereEqualTo("status", BookingStatus.CONFIRMED.name)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(Booking::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }
}