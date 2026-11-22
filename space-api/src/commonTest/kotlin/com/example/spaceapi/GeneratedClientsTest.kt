package com.example.spaceapi

import com.example.spaceapi.apod.apis.ApodApi
import com.example.spaceapi.spaceflightnews.apis.ArticlesApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

/**
 * Runs the OpenAPI-generated clients against recorded real responses through
 * Ktor's MockEngine: offline and deterministic, but proves the generated
 * requests and models match the live APIs.
 */
class GeneratedClientsTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val requests = mutableListOf<Url>()

    private fun engine(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine { request ->
            requests += request.url
            respond(body, status, json)
        }

    @Test
    fun apodListDecodesRealResponse() = runTest {
        val api = ApodApi(httpClientEngine = engine(APOD_LIST_JSON))

        val response = api.listApod(page = 1, perPage = 2, dateFrom = "261121", dateTo = "261122")
        val entries = response.body()

        assertEquals(
            "https://science.nasa.gov/wp-json/wp/v2/apod-basic?page=1&per_page=2&date_from=261121&date_to=261122",
            requests.single().toString(),
        )
        assertEquals(2, entries.size)
        with(entries.first()) {
            assertEquals(LocalDate(2026, 11, 22), date)
            assertEquals(1514357L, postId)
            assertEquals("Swirls and Colors on Jupiter from Juno", title)
            assertEquals("image", mediaType)
        }
        assertEquals("", entries[1].alt, "empty alt text is kept as-is")
    }

    @Test
    fun apodSingleDateUsesPathParameter() = runTest {
        val single = APOD_LIST_JSON.trim().removePrefix("[").substringBefore("\n },") + "\n }"
        val api = ApodApi(httpClientEngine = engine(single))

        val entry = api.getApodByDate("261122").body()

        assertEquals("/wp-json/wp/v2/apod-basic/261122", requests.single().encodedPath)
        assertEquals("Swirls and Colors on Jupiter from Juno", entry.title)
    }

    @Test
    fun apodErrorIsReportedAsUnsuccessful() = runTest {
        val api = ApodApi(httpClientEngine = engine(APOD_ERROR_JSON, HttpStatusCode.BadRequest))

        val response = api.getApodByDate("991399")

        assertFalse(response.success)
        assertEquals(400, response.status)
    }

    @Test
    fun spaceflightNewsArticlesDecodeRealResponse() = runTest {
        val api = ArticlesApi(httpClientEngine = engine(SFN_ARTICLES_JSON))

        val page = api.articlesList(limit = 2).body()

        assertEquals("/v4/articles/", requests.single().encodedPath)
        assertEquals("2", requests.single().parameters["limit"])
        assertEquals(36980, page.count)
        assertNull(page.previous)
        with(page.results.first()) {
            assertEquals(40931, id)
            assertEquals("NASA", newsSite)
            assertEquals(Instant.parse("2026-11-22T05:05:00Z"), publishedAt)
            assertNull(authors.first().socials)
        }
    }
}
