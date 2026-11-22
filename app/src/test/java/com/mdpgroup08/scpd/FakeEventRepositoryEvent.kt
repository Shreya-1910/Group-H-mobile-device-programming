package com.mdpgroup08.scpd

import com.mdpgroup08.scpd.data.repository.FakeEventRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeEventRepositoryTest {

    @Test
    fun returnsFiveSampleEvents() {
        assertEquals(5, FakeEventRepository.sampleEvents().size)
    }

    @Test
    fun atLeastOneEventIsFull() {
        assertTrue(FakeEventRepository.sampleEvents().any { it.seatsLeft == 0 })
    }
}