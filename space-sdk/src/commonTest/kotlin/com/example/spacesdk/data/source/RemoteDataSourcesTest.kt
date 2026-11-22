package com.example.spacesdk.data.source

import com.example.spacesdk.MediaType
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Generated clients (built by the same factories SpaceSdk uses) + mapping, fed
 * with recorded real responses (trimmed). Full URLs are asserted so a wrong
 * host (e.g. the generator's http://localhost default) fails here.
 */
class RemoteDataSourcesTest {

    private val requests = mutableListOf<Url>()

    private fun engine(body: String, status: HttpStatusCode = HttpStatusCode.OK) = MockEngine { request ->
        requests += request.url
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    @Test
    fun apodLatestIsMappedToPlainText() = runTest {
        val source = GeneratedApodRemoteDataSource(apodApi(engine("[$APOD_JUNO]")))

        val picture = source.fetchLatest()!!

        assertEquals(
            "https://science.nasa.gov/wp-json/wp/v2/apod-basic?page=1&per_page=1",
            requests.single().toString(),
        )
        assertEquals("2026-11-22", picture.date)
        assertEquals("Swirls and Colors on Jupiter from Juno", picture.title)
        assertEquals("What creates the colors in Jupiter's clouds? No one is sure.", picture.explanation)
        assertEquals("NASA, Juno; Processing & License: Seán Doran", picture.credit)
        assertEquals(MediaType.IMAGE, picture.mediaType)
        assertEquals("https://assets.science.nasa.gov/juno.jpg", picture.imageUrl)
        assertEquals("https://science.nasa.gov/image-article/apod-2026-november-22/", picture.pageUrl)
    }

    @Test
    fun apodByDateUsesDateCode() = runTest {
        val source = GeneratedApodRemoteDataSource(apodApi(engine(APOD_JUNO)))

        source.fetchByDate(LocalDate(2026, 11, 22))

        assertEquals("https://science.nasa.gov/wp-json/wp/v2/apod-basic/261122", requests.single().toString())
    }

    @Test
    fun apodMissingDateIsNull() = runTest {
        val source = GeneratedApodRemoteDataSource(apodApi(engine(WP_ERROR, HttpStatusCode.BadRequest)))

        assertNull(source.fetchByDate(LocalDate(2099, 1, 1)))
    }

    @Test
    fun apodServerErrorThrows() = runTest {
        val source = GeneratedApodRemoteDataSource(apodApi(engine("oops", HttpStatusCode.ServiceUnavailable)))

        val error = assertFailsWith<SpaceApiException> { source.fetchLatest() }
        assertEquals("NASA APOD returned HTTP 503", error.message)
    }

    @Test
    fun newsArticlesAreMapped() = runTest {
        val source = GeneratedNewsRemoteDataSource(spaceflightNewsApi(engine(NEWS_PAGE)))

        val articles = source.fetchLatest(limit = 1)

        assertEquals("https://api.spaceflightnewsapi.net/v4/articles/?limit=1", requests.single().toString())
        with(articles.single()) {
            assertEquals(40930, id)
            assertEquals("Spaceflight Now", newsSite)
            assertEquals("2026-11-21T16:50:51Z", publishedAt)
            assertEquals("SpaceX & the NRO", title)
            assertNull(imageUrl, "blank image_url becomes null")
        }
    }

    private companion object {
        const val APOD_JUNO = """
        {"date":"2026-11-22","post_id":1514357,"title":"Swirls and Colors on Jupiter from Juno",
         "permalink":"https://science.nasa.gov/image-article/apod-2026-november-22/","media_type":"image",
         "explanation":"<strong>Explanation:</strong> What creates the colors in Jupiter's clouds? No one is sure.",
         "credit":"<a href=\"https://www.nasa.gov/\">NASA</a>, <a href=\"x\">Juno</a>; <em>Processing &amp; <a href=\"y\">License</a>:</em> Seán Doran",
         "copyright":"NASA","alt":"","url":"https://science.nasa.gov/image-article/apod-2026-november-22/",
         "hdurl":"https://assets.science.nasa.gov/juno.jpg","basic_html":"<!doctype html>","basic_html_url":"https://x"}
        """

        const val WP_ERROR = """{"code":"rest_invalid_param","message":"Invalid parameter(s): date","data":{"status":400}}"""

        const val NEWS_PAGE = """
        {"count":36980,"next":null,"previous":null,"results":[
         {"id":40930,"title":"SpaceX &amp; the NRO","authors":[{"name":"Will Robinson-Smith","socials":null}],
          "url":"https://spaceflightnow.com/x/","image_url":"","news_site":"Spaceflight Now",
          "summary":"The NROL-136 mission...","published_at":"2026-11-21T16:50:51Z",
          "updated_at":"2026-11-21T17:01:12.011033Z","featured":false,"launches":[],"events":[]}]}
        """
    }
}
