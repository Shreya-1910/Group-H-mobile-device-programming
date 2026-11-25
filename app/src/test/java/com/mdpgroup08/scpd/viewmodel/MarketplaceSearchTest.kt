package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.network.FakeNetworkMonitor
import com.mdpgroup08.scpd.data.repository.FakeEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset

/** Marketplace search and filters (R2-S2). */
@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceSearchTest {

    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val utc = ZoneOffset.UTC

    // Thu 1 Jan 2026, 08:00 UTC
    private val fixedNow = 1_767_225_600_000L + 8 * hour

    private fun item(
        id: String, title: String = "Event $id", club: String = "Computing Club",
        category: String = "Technology", location: String = "Lab 1", description: String = "",
        startsIn: Long = 3 * day, points: Int = 5, availability: Availability = Availability.OPEN
    ) = MarketplaceItem(
        event = Event(
            id = id, title = title, clubName = club, category = category, location = location,
            description = description, startTime = fixedNow + startsIn, endTime = fixedNow + startsIn + 2 * hour
        ),
        points = points, placesLeft = 10, availability = availability
    )

    private fun ids(items: List<MarketplaceItem>, filters: MarketplaceFilters) =
        MarketplaceSearch.apply(items, filters, fixedNow, utc).map { it.event.id }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // ---------- Keyword search ----------

    @Test
    fun `keyword matches the title, ignoring case`() {
        val items = listOf(item("a", title = "Career Talk: AI"), item("b", title = "Beach Clean-up"))
        assertEquals(listOf("a"), ids(items, MarketplaceFilters(query = "career")))
    }

    @Test
    fun `keyword matches the club and the venue`() {
        val items = listOf(item("a", club = "Green Society"), item("b", location = "Port Dickson"))
        assertEquals(listOf("a"), ids(items, MarketplaceFilters(query = "green")))
        assertEquals(listOf("b"), ids(items, MarketplaceFilters(query = "dickson")))
    }

    @Test
    fun `every word must match`() {
        val items = listOf(item("a", title = "AI Career Talk"), item("b", title = "AI Workshop"))
        assertEquals(listOf("a"), ids(items, MarketplaceFilters(query = "ai talk")))
    }

    @Test
    fun `blank search matches everything`() {
        val items = listOf(item("a"), item("b"))
        assertEquals(listOf("a", "b"), ids(items, MarketplaceFilters(query = "   ")))
    }

    // ---------- Filters ----------

    @Test
    fun `category filter, with blank category counted as Other`() {
        val items = listOf(item("a", category = "Sports"), item("b", category = ""))
        assertEquals(listOf("a"), ids(items, MarketplaceFilters(category = "Sports")))
        assertEquals(listOf("b"), ids(items, MarketplaceFilters(category = "Other")))
    }

    @Test
    fun `club filter`() {
        val items = listOf(item("a", club = "Sports Club"), item("b", club = "Green Society"))
        assertEquals(listOf("b"), ids(items, MarketplaceFilters(club = "Green Society")))
    }

    @Test
    fun `minimum points uses the item's approved points`() {
        val items = listOf(item("low", points = 3), item("high", points = 8))
        assertEquals(listOf("high"), ids(items, MarketplaceFilters(minPoints = 5)))
    }

    @Test
    fun `available only hides full and closed events`() {
        val items = listOf(
            item("open"), item("full", availability = Availability.FULL),
            item("closed", availability = Availability.CLOSED)
        )
        assertEquals(listOf("open"), ids(items, MarketplaceFilters(availableOnly = true)))
    }

    @Test
    fun `today means the same calendar day`() {
        val items = listOf(item("today", startsIn = 2 * hour), item("tomorrow", startsIn = 20 * hour))
        assertEquals(listOf("today"), ids(items, MarketplaceFilters(date = DateFilter.TODAY)))
    }

    @Test
    fun `next 7 days excludes events further away`() {
        val items = listOf(item("soon", startsIn = 6 * day), item("later", startsIn = 10 * day))
        assertEquals(listOf("soon"), ids(items, MarketplaceFilters(date = DateFilter.NEXT_7_DAYS)))
    }

    @Test
    fun `keyword and filters combine`() {
        val items = listOf(
            item("a", title = "AI Talk", category = "Career"),
            item("b", title = "AI Workshop", category = "Technology")
        )
        assertEquals(listOf("b"), ids(items, MarketplaceFilters(query = "ai", category = "Technology")))
    }

    @Test
    fun `active count includes the search text`() {
        val f = MarketplaceFilters(query = "ai", category = "Career", availableOnly = true)
        assertEquals(3, f.activeCount)
        assertFalse(MarketplaceFilters().isActive)
    }

    // ---------- In the ViewModel ----------

    private val now = System.currentTimeMillis()

    private fun event(id: String, title: String, club: String) = Event(
        id = id, title = title, clubName = club, category = "Technology", status = EventStatus.APPROVED,
        startTime = now + 3 * day, endTime = now + 3 * day + 2 * hour,
        requestedPoints = 5, approvedPoints = 5, capacity = 50, bookedCount = 10
    )

    private fun viewModel() = MarketplaceViewModel(
        FakeEventRepository(
            listOf(event("a", "Hackathon", "Computing Club"), event("b", "Beach Clean-up", "Green Society"))
        ),
        FakeNetworkMonitor(), clock = { now }
    )

    private fun MarketplaceViewModel.loaded() = uiState.value.list as MarketplaceList.Loaded

    @Test
    fun `no match keeps the total so the screen can suggest clearing filters`() {
        val vm = viewModel()
        vm.onQueryChange("zzz")
        assertTrue(vm.loaded().items.isEmpty())
        assertEquals(2, vm.loaded().total)
    }

    @Test
    fun `clear filters shows every event again`() {
        val vm = viewModel()
        vm.onQueryChange("hack")
        vm.setAvailableOnly(true)
        assertEquals(1, vm.loaded().items.size)

        vm.clearFilters()
        assertEquals(2, vm.loaded().items.size)
        assertFalse(vm.uiState.value.filters.isActive)
    }

    @Test
    fun `club filter options come from the loaded events, sorted`() {
        assertEquals(listOf("Computing Club", "Green Society"), viewModel().uiState.value.clubs)
    }
}