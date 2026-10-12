package com.i.miniread.network

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MinifluxApi.kt 中数据类的 Gson 反序列化纯 JVM 测试。
 *
 * 覆盖 @SerializedName 映射（snake_case -> camelCase）、不可变 + 可空字段的缺省/null 容错。
 * 不使用任何 android.* / Robolectric。
 */
class GsonParsingTest {

    private val gson = Gson()

    @Test
    fun parseEntry_mapsSnakeCaseFields() {
        val json = """
            {
              "id": 42,
              "title": "Hello",
              "url": "https://example.com/a",
              "content": "<p>hi</p>",
              "published_at": "2024-01-01T00:00:00Z",
              "status": "unread",
              "feed_id": 7,
              "starred": true,
              "feed": {
                "id": 7,
                "title": "Feed Title",
                "site_url": "https://site",
                "feed_url": "https://feed",
                "category": { "id": 1, "title": "Cat" },
                "disabled": false,
                "unread_count": 5
              }
            }
        """.trimIndent()

        val entry = gson.fromJson(json, Entry::class.java)

        assertEquals(42, entry.id)
        assertEquals(7, entry.feed_id)
        assertEquals("2024-01-01T00:00:00Z", entry.published_at)
        assertTrue(entry.starred)
        assertEquals("Hello", entry.title)
        assertEquals(7, entry.feed.id)
        assertEquals(5, entry.feed.unreadCount)
    }

    @Test
    fun parseFeed_mapsUnreadCountFromSnakeCase() {
        val json = """
            {
              "id": 7,
              "title": "Feed Title",
              "site_url": "https://site",
              "feed_url": "https://feed",
              "category": { "id": 1, "title": "Cat" },
              "disabled": false,
              "unread_count": 5
            }
        """.trimIndent()

        val feed = gson.fromJson(json, Feed::class.java)

        assertEquals(7, feed.id)
        assertEquals("Feed Title", feed.title)
        assertEquals(5, feed.unreadCount)
        assertEquals(1, feed.category.id)
        assertEquals("Cat", feed.category.title)
        assertFalse(feed.disabled)
    }

    @Test
    fun parseFeed_camelCaseUnreadCountIsIgnored() {
        // 已用 @SerializedName("unread_count")，故 "unreadCount" 不会映射到字段
        val json = """
            {
              "id": 1,
              "title": "F",
              "site_url": "",
              "feed_url": "",
              "category": null,
              "disabled": false,
              "unreadCount": 5
            }
        """.trimIndent()

        val feed = gson.fromJson(json, Feed::class.java)

        assertEquals(0, feed.unreadCount)
    }

    @Test
    fun parseCategory_mapsFeedCountAndTotalUnread() {
        val json = """{ "id": 1, "title": "Cat", "feed_count": 3, "total_unread": 9 }"""

        val category = gson.fromJson(json, Category::class.java)

        assertEquals(1, category.id)
        assertEquals("Cat", category.title)
        assertEquals(3, category.feedCount)
        assertEquals(9, category.unreadCount)
    }

    @Test
    fun parseCategory_missingCountsDefaultToZero() {
        val json = """{ "id": 2, "title": "Only" }"""

        val category = gson.fromJson(json, Category::class.java)

        assertEquals(0, category.feedCount)
        assertEquals(0, category.unreadCount)
    }

    @Test
    fun parseEntriesResponse_mapsTotalAndEntries() {
        val json = """
            {
              "total": 2,
              "entries": [
                { "id": 1, "feed_id": 10, "starred": false },
                { "id": 2, "feed_id": 11, "starred": true }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, EntriesResponse::class.java)

        assertEquals(2, response.total)
        assertEquals(2, response.entries.size)
        assertEquals(1, response.entries[0].id)
        assertEquals(11, response.entries[1].feed_id)
        assertTrue(response.entries[1].starred)
    }

    @Test
    fun parseFeedCountersResponse_mapsStringIntMaps() {
        val json = """{ "reads": { "1": 10, "2": 0 }, "unreads": { "1": 2, "3": 7 } }"""

        val response = gson.fromJson(json, MinifluxApi.FeedCountersResponse::class.java)

        assertEquals(10, response.reads["1"]!!.toInt())
        assertEquals(2, response.unreads["1"]!!.toInt())
        assertEquals(7, response.unreads["3"]!!.toInt())
    }

    @Test
    fun parseEntry_missingOptionalFields_tolerated() {
        val json = """{ "id": 1, "feed_id": 2, "starred": false }"""

        val entry = gson.fromJson(json, Entry::class.java)

        assertEquals(1, entry.id)
        assertEquals(2, entry.feed_id)
        assertFalse(entry.starred)
        assertNull(entry.title)
        assertNull(entry.url)
        assertNull(entry.content)
        assertNull(entry.published_at)
        assertNull(entry.status)
        assertNull(entry.feed)
    }

    @Test
    fun parseFeed_missingCountsDefaultToZeroAndNullCategory() {
        val json = """{ "id": 9, "title": "F", "site_url": "", "feed_url": "", "disabled": true }"""

        val feed = gson.fromJson(json, Feed::class.java)

        assertEquals(0, feed.unreadCount)
        assertNull(feed.category)
        assertTrue(feed.disabled)
    }

    @Test
    fun parseEntriesResponse_missingEntriesTolerated() {
        val json = """{ "total": 0 }"""

        val response = gson.fromJson(json, EntriesResponse::class.java)

        assertEquals(0, response.total)
        assertNull(response.entries)
    }

    @Test
    fun parseFeedCountersResponse_nullMapsTolerated() {
        val json = """{ "reads": null, "unreads": null }"""

        val response = gson.fromJson(json, MinifluxApi.FeedCountersResponse::class.java)

        assertNull(response.reads)
        assertNull(response.unreads)
    }
}
