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

    private val events = db.collection("events")
    private val bookings = db.collection("bookings")

    /**
     * One transaction: check the event is approved and has a place, then add the booking and
     * increase bookedCount together, so capacity is never exceeded even if two students book
     * the last place at the same moment (R2-S4, R2-S6).
     */
    override suspend fun book(eventId: String) {
        val uid = auth.currentUser?.uid ?: throw BookingException.NotLoggedIn
        val eventRef = events.document(eventId)
        val bookingRef = bookings.document("${eventId}_$uid")

        runSafely {
            db.runTransaction { tx ->
                val event = tx.get(eventRef).toObject(Event::class.java)
                    ?: throw BookingException.EventNotFound
                val existing = tx.get(bookingRef).toObject(Booking::class.java)

                if (event.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
                // A cancelled booking can be booked again; only a confirmed one blocks it
                if (existing?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
                if (event.bookedCount >= event.capacity) throw BookingException.EventFull

                tx.update(eventRef, "bookedCount", FieldValue.increment(1))
                tx.set(bookingRef, newBooking(eventId, uid))
                null
            }.await()
        }
    }

    /**
     * R2-S7: one transaction that cancels the old booking and books the new event.
     * Firestore requires every read before any write, so all four documents are read first.
     */
    override suspend fun swap(fromEventId: String, toEventId: String) {
        val uid = auth.currentUser?.uid ?: throw BookingException.NotLoggedIn
        val fromEventRef = events.document(fromEventId)
        val toEventRef = events.document(toEventId)
        val fromBookingRef = bookings.document("${fromEventId}_$uid")
        val toBookingRef = bookings.document("${toEventId}_$uid")

        runSafely {
            db.runTransaction { tx ->
                // 1. Read everything
                val toEvent = tx.get(toEventRef).toObject(Event::class.java)
                    ?: throw BookingException.EventNotFound
                tx.get(fromEventRef).toObject(Event::class.java) ?: throw BookingException.EventNotFound
                val fromBooking = tx.get(fromBookingRef).toObject(Booking::class.java)
                val toBooking = tx.get(toBookingRef).toObject(Booking::class.java)

                // 2. Check everything, so a failure changes nothing
                if (fromBooking?.status != BookingStatus.CONFIRMED) throw BookingException.NotBooked
                if (toEvent.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
                if (toBooking?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
                if (toEvent.bookedCount >= toEvent.capacity) throw BookingException.EventFull

                // 3. Write: release the old place, take the new one
                val now = System.currentTimeMillis()
                tx.update(fromBookingRef, mapOf("status" to BookingStatus.CANCELLED.name, "cancelledAt" to now))
                tx.update(fromEventRef, "bookedCount", FieldValue.increment(-1))
                tx.update(toEventRef, "bookedCount", FieldValue.increment(1))
                tx.set(toBookingRef, newBooking(toEventId, uid))
                null
            }.await()
        }
    }

    override fun getMyBooking(eventId: String): Flow<Booking?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val reg = bookings.document("${eventId}_$uid")
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
        val reg = bookings
            .whereEqualTo("studentId", uid)
            .whereEqualTo("status", BookingStatus.CONFIRMED.name)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(Booking::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    private fun newBooking(eventId: String, uid: String) = Booking(
        eventId = eventId,
        studentId = uid,
        status = BookingStatus.CONFIRMED,
        createdAt = System.currentTimeMillis()
    )

    /** Firestore may wrap our exception inside its own, so unwrap it for the ViewModel. */
    private suspend fun runSafely(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            throw (e as? BookingException) ?: (e.cause as? BookingException) ?: e
        }
    }
}