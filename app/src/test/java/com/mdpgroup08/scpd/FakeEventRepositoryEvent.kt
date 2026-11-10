package com.mdpgroup08.scpd



import com.mdpgroup08.scpd.data.repo.FakeEventRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeEventRepositoryTest {

    @Test
    fun returnsFourEvents() {
        assertEquals(4, FakeEventRepository.getEvents().size)
    }

    @Test
    fun atLeastOneEventIsFull() {
        assertTrue(FakeEventRepository.getEvents().any { it.seatsLeft == 0 })
    }
}