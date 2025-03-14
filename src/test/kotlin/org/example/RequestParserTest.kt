package org.example

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RequestParserTest {

    @Test
    fun `parse should return null for invalid input`() {
        val input = "This is not a valid request format"
        val result = RequestParser.parse(input)
        assertNull(result)
    }

    @Test
    fun `parse should parse basic request with only URL and method`() {
        val input = "Determine the status of https://api.example.com/endpoint using GET"
        val result = RequestParser.parse(input)

        assertNotNull(result)
        assertEquals("https://api.example.com/endpoint", result?.url)
        assertEquals("GET", result?.method)
        assertTrue(result?.queryParams?.isEmpty() == true)
        assertTrue(result?.headers?.isEmpty() == true)
        assertNull(result?.body)
    }

    @Test
    fun `parse should handle different HTTP methods`() {
        val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")

        methods.forEach { method ->
            val input = "Determine the status of https://api.example.com/endpoint using $method"
            val result = RequestParser.parse(input)

            assertNotNull(result)
            assertEquals(method, result?.method)
        }
    }

    @Test
    fun `parse should handle query parameters`() {
        val input =
            "Determine the status of https://api.example.com/search using GET with query parameters q=test, limit=10"
        val result = RequestParser.parse(input)

        assertNotNull(result)
        assertEquals("https://api.example.com/search", result?.url)
        assertEquals("GET", result?.method)
        assertEquals(mapOf("q" to "test", "limit" to "10"), result?.queryParams)
        assertTrue(result?.headers?.isEmpty() == true)
        assertNull(result?.body)
    }

    @Test
    fun `parse should handle headers`() {
        val input =
            "Determine the status of https://api.example.com/endpoint using GET with headers Authorization=Bearer token, Content-Type=application/json"
        val result = RequestParser.parse(input)

        assertNotNull(result)
        assertEquals("https://api.example.com/endpoint", result?.url)
        assertEquals("GET", result?.method)
        assertTrue(result?.queryParams?.isEmpty() == true)
        assertEquals(
            mapOf(
                "Authorization" to "Bearer token", "Content-Type" to "application/json"
            ), result?.headers
        )
        assertNull(result?.body)
    }

    @Test
    fun `parse should handle request body`() {
        val input = """
            Determine the status of https://api.example.com/users using POST and body {"name":"John","email":"john@example.com"}
        """.trimIndent()
        val result = RequestParser.parse(input)

        assertNotNull(result)
        assertEquals("https://api.example.com/users", result?.url)
        assertEquals("POST", result?.method)
        assertTrue(result?.queryParams?.isEmpty() == true)
        assertTrue(result?.headers?.isEmpty() == true)
        assertEquals("""{"name":"John","email":"john@example.com"}""", result?.body)
    }

    @Test
    fun `parse should handle complete request with all parameters`() {
        val input = """
            Determine the status of https://api.example.com/users using POST 
            with query parameters id=123, version=2 
            with headers Authorization=Bearer token, Content-Type=application/json 
            and body {"name":"John","email":"john@example.com"}
        """.trimIndent()
        val result = RequestParser.parse(input)

        assertNotNull(result)
        assertEquals("https://api.example.com/users", result?.url)
        assertEquals("POST", result?.method)
        assertEquals(mapOf("id" to "123", "version" to "2"), result?.queryParams)
        assertEquals(
            mapOf(
                "Authorization" to "Bearer token", "Content-Type" to "application/json"
            ), result?.headers
        )
        assertEquals("""{"name":"John","email":"john@example.com"}""", result?.body)
    }

    @Test
    fun `parse should handle key-value pairs with colon separator`() {
        val input =
            "Determine the status of https://api.example.com/endpoint using GET with headers Content-Type: application/json, Accept: application/json"
        val result = RequestParser.parse(input)

        assertNotNull(result)
        assertEquals(
            mapOf(
                "Content-Type" to "application/json", "Accept" to "application/json"
            ), result?.headers
        )
    }

    @Test
    fun `parse should be case insensitive for keywords`() {
        val input = "determine THE status OF https://api.example.com/endpoint USING get WITH query PARAMETERS id=123"
        val result = RequestParser.parse(input)

        assertNotNull(result)
        assertEquals("https://api.example.com/endpoint", result?.url)
        assertEquals("GET", result?.method)
        assertEquals(mapOf("id" to "123"), result?.queryParams)
    }
}