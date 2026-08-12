package me.ash.reader.infrastructure.rss

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import me.ash.reader.domain.repository.ArticleDao
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import android.util.Log

internal const val enclosureUrlString1: String = "https://example.com/enclosure.jpg"
internal const val enclosureUrlString2: String = "https://github.blog/wp-content/uploads/2024/03/github_copilot_header.png"
internal const val imageUrlString: String = "https://example.com/image.jpg"
internal const val enclosureHtmlCase1: String = """
        <enclosure url="$enclosureUrlString1" type="image/jpeg"/>
        <img src="$imageUrlString"/>
    """
internal const val enclosureHtmlCase2: String = """
        <img src="$imageUrlString"/>
        <enclosure url="$enclosureUrlString1" type="image/jpeg"/>
        <img src="$imageUrlString"/> 
    """
internal const val enclosureHtmlCase3: String = """
        <img src="$imageUrlString"/>
        <enclosure url="$enclosureUrlString2" type="image/png"/>
        <img src="$imageUrlString"/> 
    """
internal const val imageHtmlCase1: String = """
        <img src="$enclosureUrlString1"/>
        <img src="$imageUrlString"/> 
    """
internal const val imageHtmlCase2: String = """
        <img src="$imageUrlString"/> 
        <img src="$enclosureUrlString1"/> 
        <img src="$enclosureUrlString1"/> 
    """

@RunWith(MockitoJUnitRunner::class)
class RssHelperTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockIODispatcher: CoroutineDispatcher

    @Mock
    private lateinit var mockOkHttpClient: OkHttpClient

    @Mock
    private lateinit var mockArticleDao: ArticleDao

    private lateinit var rssHelper: RssHelper

    @Before
    fun setUp() {
        mockContext = mock<Context> { }
        mockIODispatcher = mock<CoroutineDispatcher> {}
        mockOkHttpClient = mock<OkHttpClient> {}
        mockArticleDao = mock<ArticleDao> {}
        rssHelper = RssHelper(mockContext, mockIODispatcher, mockOkHttpClient, mockArticleDao)
    }

    @Test
    fun testFindThumbnail() {
        Assert.assertNull(rssHelper.findThumbnail(""))
        Assert.assertNull(rssHelper.findThumbnail(" "))
        Assert.assertNull(rssHelper.findThumbnail(null))
        Assert.assertEquals(enclosureUrlString1, rssHelper.findThumbnail(enclosureHtmlCase1))
        Assert.assertEquals(enclosureUrlString1, rssHelper.findThumbnail(enclosureHtmlCase2))
        Assert.assertEquals(enclosureUrlString2, rssHelper.findThumbnail(enclosureHtmlCase3))
        Assert.assertEquals(enclosureUrlString1, rssHelper.findThumbnail(imageHtmlCase1))
        Assert.assertEquals(imageUrlString, rssHelper.findThumbnail(imageHtmlCase2))
    }

    @Test
    fun testEnclosureNoFilenameExtension() {
        val case = """
            <enclosure url="$imageUrlString" type="image/jpeg" length="0"/>
        """
        Assert.assertEquals(imageUrlString, rssHelper.findThumbnail(case))
    }

    @Test
    fun testMediaNamespaceThumbnailInRSS20() {
        val case = """
            <enclosure url="$imageUrlString" type="image/jpeg" length="0"/>
        """
        Assert.assertEquals(imageUrlString, rssHelper.findThumbnail(case))
    }

    @Test
    fun testSearchFeedSitemap() = runBlocking {
        val sitemapXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                <url>
                    <loc>https://www.daily-bangladesh.com/bangla/31421</loc>
                    <lastmod>2026-08-12T10:00:00Z</lastmod>
                </url>
            </urlset>
        """.trimIndent()

        val mockCallSitemap = mock<Call>()
        val mockCallOther = mock<Call>()
        val mediaType = "text/xml; charset=utf-8".toMediaType()
        val responseBody = sitemapXml.toResponseBody(mediaType)
        val mockResponse = Response.Builder()
            .request(Request.Builder().url("https://www.daily-bangladesh.com/bangla-sitemap/sitemap-daily-2026-08-12.xml").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody)
            .build()

        whenever(mockOkHttpClient.newCall(any())).thenAnswer { invocation ->
            val req = invocation.getArgument<Request>(0)
            if (req.url.toString().contains("sitemap")) {
                mockCallSitemap
            } else {
                mockCallOther
            }
        }

        whenever(mockCallSitemap.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onResponse(mockCallSitemap, mockResponse)
            null
        }

        val emptyMediaType = "text/html; charset=utf-8".toMediaType()
        val emptyResponseBody = "".toResponseBody(emptyMediaType)
        val mockResponse404 = Response.Builder()
            .request(Request.Builder().url("https://www.daily-bangladesh.com/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Not Found")
            .body(emptyResponseBody)
            .build()

        whenever(mockCallOther.execute()).thenReturn(mockResponse404)

        Mockito.mockStatic(Log::class.java).use { mockedLog ->
            mockedLog.`when`<Boolean> { Log.isLoggable(any(), any()) }.thenReturn(true)

            val testRssHelper = RssHelper(mockContext, Dispatchers.Unconfined, mockOkHttpClient, mockArticleDao)
            val result = testRssHelper.searchFeed("https://www.daily-bangladesh.com/bangla-sitemap/sitemap-daily-2026-08-12.xml")

            Assert.assertNotNull(result)
            Assert.assertEquals("Sitemap: www.daily-bangladesh.com", result.feed.title)
            Assert.assertEquals("https://www.daily-bangladesh.com/bangla-sitemap/sitemap-daily-2026-08-12.xml", result.feed.link)
            Assert.assertNotNull(result.feed.icon)
        }
    }
}
