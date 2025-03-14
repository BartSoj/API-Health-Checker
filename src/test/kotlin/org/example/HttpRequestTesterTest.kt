package org.example

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class HttpRequestTesterTest {

    private val httpRequestTester = HttpRequestTester()

    @AfterEach
    fun cleanup() {
        httpRequestTester.close()
    }

    @Test
    fun `checkHealth should return healthy status for successful response`() {
        val result = httpRequestTester.checkHealth(
            url = "https://httpbin.org/get", method = "GET"
        )

        assertTrue(result.healthy)
        assertEquals(200, result.statusCode)
        assertTrue(result.responseTime > 0)
        assertNull(result.errorMessage)
    }

    @Test
    fun `checkHealth should return unhealthy status for 404 response`() {
        val result = httpRequestTester.checkHealth(
            url = "https://httpbin.org/status/404", method = "GET"
        )

        assertFalse(result.healthy)
        assertEquals(404, result.statusCode)
        assertTrue(result.responseTime > 0)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `checkHealth should handle POST requests with body`() {
        val body = """{"test": "value"}"""
        val result = httpRequestTester.checkHealth(
            url = "https://httpbin.org/post", method = "POST", body = body
        )

        assertTrue(result.healthy)
        assertEquals(200, result.statusCode)
    }

    @Test
    fun `checkHealth should handle request parameters`() {
        val params = mapOf("param1" to "value1", "param2" to "value2")
        val result = httpRequestTester.checkHealth(
            url = "https://httpbin.org/get", method = "GET", params = params
        )

        assertTrue(result.healthy)
        assertEquals(200, result.statusCode)
    }

    @Test
    fun `checkHealth should handle timeout errors`() {
        val result = httpRequestTester.checkHealth(
            url = "https://httpbin.org/delay/10", method = "GET"
        )

        assertFalse(result.healthy)
        assertEquals(-1, result.statusCode)
        assertNotNull(result.errorMessage)
        assertTrue(
            result.errorMessage.contains("timeout") || result.errorMessage.contains("timed out"),
            "Error message should indicate timeout: ${result.errorMessage}"
        )
    }
}