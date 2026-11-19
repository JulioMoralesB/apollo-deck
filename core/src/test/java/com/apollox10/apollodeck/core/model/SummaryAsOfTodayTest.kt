package com.apollox10.apollodeck.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SummaryAsOfTodayTest {

    private val today = LocalDate.parse("2026-11-18")
    private val now = Instant.parse("2026-11-18T16:00:00Z")

    private fun caduTrack(raw: String) =
        (parseServiceSummary(raw, today = today, now = now) as ServiceSummary.CaduTrack).data

    @Test
    fun `next items whose date passed move to expired, counts follow`() {
        val data = caduTrack(
            """{"expired": 1, "expired_products": [{"name": "Leche", "expires_at": "2026-11-10"}],
               "expiring_soon": 3, "next": [{"name": "Cebolla", "expires_at": "2026-11-17"},
                                            {"name": "Papa", "expires_at": "2026-11-17"}]}""",
        )
        assertEquals(3, data.expired)
        assertEquals(listOf("Leche", "Cebolla", "Papa"), data.expiredProducts.map { it.name })
        assertEquals(1, data.expiringSoon)
        assertEquals(emptyList<String>(), data.next.map { it.name })
    }

    @Test
    fun `an item expiring today is not expired yet`() {
        val data = caduTrack(
            """{"expired": 0, "expired_products": [], "expiring_soon": 1,
               "next": [{"name": "Tortilla", "expires_at": "2026-11-18"}]}""",
        )
        assertEquals(0, data.expired)
        assertEquals(listOf("Tortilla"), data.next.map { it.name })
    }

    @Test
    fun `expiring soon count never goes negative`() {
        val data = caduTrack(
            """{"expired": 0, "expired_products": [], "expiring_soon": 0,
               "next": [{"name": "Queso", "expires_at": "2026-11-01"}]}""",
        )
        assertEquals(0, data.expiringSoon)
        assertEquals(1, data.expired)
    }

    @Test
    fun `ended free game promotions drop off, open-ended ones stay`() {
        val data = (parseServiceSummary(
            """{"service": "free-games-notifier", "active_promotions": [
                 {"title": "Ended", "store": "epic", "end_date": "2026-11-18T15:00:00.000Z"},
                 {"title": "Running", "store": "epic", "end_date": "2026-11-19T16:00:00.000Z"},
                 {"title": "No end", "store": "steam", "end_date": ""}]}""",
            today = today,
            now = now,
        ) as ServiceSummary.FreeGames).data
        assertEquals(listOf("Running", "No end"), data.activePromotions.map { it.title })
    }

    @Test
    fun `fetched_at is read back, and data age shows only after an hour`() {
        val raw = """{"expired": 0, "expired_products": [], "expiring_soon": 0, "next": [],
                      "_fetched_at": "2026-11-17T18:30:00.180762-06:00"}"""
        val fetchedAt = summaryFetchedAt(raw)
        assertEquals(Instant.parse("2026-11-18T00:30:00.180762Z"), fetchedAt)
        assertEquals("Updated 15 h ago", formatDataAge(fetchedAt, now))
        assertNull(formatDataAge(now.minusSeconds(30 * 60), now))
        assertEquals("Updated 2 d ago", formatDataAge(now.minusSeconds(2 * 24 * 3600 + 60), now))
    }
}
