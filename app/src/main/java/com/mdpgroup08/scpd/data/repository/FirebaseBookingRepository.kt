package com.mdpgroup08.scpd.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Bookings at bookings/{eventId}_{studentId}; the waitlist is a list of student IDs on the event.
 * Every change runs in a transaction, so bookedCount and the waitlist always stay correct.
 * Firestore requires every read before any write inside a transaction.
 */
class FirebaseBookingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : BookingRepository {

    private val events = db.collection("events")
    private val bookings = db.collection("bookings")

    override fun myId(): String? = auth.currentUser?.uid

    private fun uid(): String = myId() ?: throw BookingException.NotLoggedIn
    private fun bookingRef(eventId: String, studentId: String) = bookings.document("${eventId}_$studentId")

    /** R2-S4, R2-S6: capacity is never exceeded, even if two students book the last place together. */
    override suspend fun book(eventId: String, leaveWaitlistsFor: List<String>) {
        val uid = uid()
        val eventRef = events.document(eventId)
        val myRef = bookingRef(eventId, uid)

        runSafely {
            db.runTransaction { tx ->
                val event = tx.get(eventRef).toObject(Event::class.java) ?: throw BookingException.EventNotFound
                val existing = tx.get(myRef).toObject(Booking::class.java)

                if (event.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
                // A cancelled booking can be booked again; only a confirmed one blocks it
                if (existing?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
                if (event.bookedCount >= event.capacity) throw BookingException.EventFull

                tx.update(eventRef, mapOf<String, Any>(
                    "bookedCount" to FieldValue.increment(1),
                    "waitlist" to FieldValue.arrayRemove(uid)
                ))
                tx.set(myRef, newBooking(eventId, uid))
                // R2-S5 EX1: a student on a waitlist never holds a clashing booking
                leaveWaitlistsFor.forEach { tx.update(events.document(it), "waitlist", FieldValue.arrayRemove(uid)) }
                null
            }.await()
        }
    }

    /** R2-S7: cancel the old booking and book the new event; the old place goes to its waitlist. */
    override suspend fun swap(fromEventId: String, toEventId: String) {
        val uid = uid()
        val fromEventRef = events.document(fromEventId)
        val toEventRef = events.document(toEventId)
        val fromBookingRef = bookingRef(fromEventId, uid)
        val toBookingRef = bookingRef(toEventId, uid)

        runSafely {
            db.runTransaction { tx ->
                // 1. Read everything
                val toEvent = tx.get(toEventRef).toObject(Event::class.java) ?: throw BookingException.EventNotFound
                val fromEvent = tx.get(fromEventRef).toObject(Event::class.java) ?: throw BookingException.EventNotFound
                val fromBooking = tx.get(fromBookingRef).toObject(Booking::class.java)
                val toBooking = tx.get(toBookingRef).toObject(Booking::class.java)
                val next = readNextInLine(tx, fromEvent)

                // 2. Check everything, so a failure changes nothing
                if (fromBooking?.status != BookingStatus.CONFIRMED) throw BookingException.NotBooked
                if (toEvent.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
                if (toBooking?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
                if (toEvent.bookedCount >= toEvent.capacity) throw BookingException.EventFull

                // 3. Write
                tx.update(fromBookingRef, cancelledFields())
                releasePlace(tx, fromEventRef, fromEventId, next)
                tx.update(toEventRef, mapOf<String, Any>(
                    "bookedCount" to FieldValue.increment(1),
                    "waitlist" to FieldValue.arrayRemove(uid)
                ))
                tx.set(toBookingRef, newBooking(toEventId, uid))
                null
            }.await()
        }
    }

    /** R2-S8: cancel a booking; the first student on the waitlist is booked in the same step (R2-S5 AC2). */
    override suspend fun cancel(eventId: String) {
        val uid = uid()
        val eventRef = events.document(eventId)
        val myRef = bookingRef(eventId, uid)

        runSafely {
            db.runTransaction { tx ->
                val event = tx.get(eventRef).toObject(Event::class.java) ?: throw BookingException.EventNotFound
                val mine = tx.get(myRef).toObject(Booking::class.java)
                val next = readNextInLine(tx, event)

                if (mine?.status != BookingStatus.CONFIRMED) throw BookingException.NotBooked

                tx.update(myRef, cancelledFields())
                releasePlace(tx, eventRef, eventId, next)
                null
            }.await()
        }
    }

    /** R2-S5: join the queue of a full event. arrayUnion adds to the end, so first come, first served. */
    override suspend fun joinWaitlist(eventId: String) {
        val uid = uid()
        val eventRef = events.document(eventId)
        val myRef = bookingRef(eventId, uid)

        runSafely {
            db.runTransaction { tx ->
                val event = tx.get(eventRef).toObject(Event::class.java) ?: throw BookingException.EventNotFound
                val mine = tx.get(myRef).toObject(Booking::class.java)

                if (event.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
                if (mine?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
                if (uid in event.waitlist) throw BookingException.AlreadyOnWaitlist
                if (event.bookedCount < event.capacity) throw BookingException.NotFull

                tx.update(eventRef, "waitlist", FieldValue.arrayUnion(uid))
                null
            }.await()
        }
    }

    override suspend fun leaveWaitlist(eventId: String) {
        val uid = uid()
        runSafely { events.document(eventId).update("waitlist", FieldValue.arrayRemove(uid)).await() }
    }

    override fun getMyBooking(eventId: String): Flow<Booking?> = callbackFlow {
        val uid = myId()
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val reg = bookingRef(eventId, uid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(Booking::class.java))
        }
        awaitClose { reg.remove() }
    }

    /** The signed-in student's confirmed bookings, used for clash checks (R2-S7). */
    override fun getMyBookings(): Flow<List<Booking>> = callbackFlow {
        val uid = myId()
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

    // ---------- Waitlist promotion (R2-S5 AC2) ----------

    /** The first student in the queue, read before any write. */
    private data class NextInLine(val studentId: String, val ref: DocumentReference, val alreadyBooked: Boolean)

    private fun readNextInLine(tx: Transaction, event: Event): NextInLine? {
        val next = event.waitlist.firstOrNull() ?: return null
        val ref = bookingRef(event.id, next)
        val booking = tx.get(ref).toObject(Booking::class.java)
        return NextInLine(next, ref, booking?.status == BookingStatus.CONFIRMED)
    }

    /** A place has been freed: give it to the first student in line, or release it if nobody is waiting. */
    private fun releasePlace(tx: Transaction, eventRef: DocumentReference, eventId: String, next: NextInLine?) {
        when {
            next == null -> tx.update(eventRef, "bookedCount", FieldValue.increment(-1))
            next.alreadyBooked -> tx.update(eventRef, mapOf<String, Any>(
                "bookedCount" to FieldValue.increment(-1),
                "waitlist" to FieldValue.arrayRemove(next.studentId)
            ))
            else -> {
                // One student leaves and one joins, so bookedCount stays the same
                tx.set(next.ref, newBooking(eventId, next.studentId, fromWaitlist = true))
                tx.update(eventRef, "waitlist", FieldValue.arrayRemove(next.studentId))
            }
        }
    }

    // ---------- Helpers ----------

    private fun newBooking(eventId: String, studentId: String, fromWaitlist: Boolean = false) = Booking(
        eventId = eventId,
        studentId = studentId,
        status = BookingStatus.CONFIRMED,
        createdAt = System.currentTimeMillis(),
        fromWaitlist = fromWaitlist
    )

    private fun cancelledFields() = mapOf<String, Any>(
        "status" to BookingStatus.CANCELLED.name,
        "cancelledAt" to System.currentTimeMillis()
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