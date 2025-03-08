package org.example

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class FindMatchingEndpointTest {

    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    @BeforeEach
    fun setUp() {
        outputStreamCaptor.reset()
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @AfterEach
    fun tearDown() {
        System.setOut(standardOut)
//        println("Captured output:\n${outputStreamCaptor.toString().trim()}")  // Uncomment to see captured output
    }

    @Test
    fun `findMatchingEndpoint should find album tracks endpoint in Spotify OpenAPI`() {
        val url = "https://api.spotify.com/v1/albums/4aawyAB9vmqN3uQ7FjRGTy/tracks"
        val method = "GET"

        findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        assertTrue(output.contains("Match found in: api_specs/spotify_openapi.json"))
        assertTrue(output.contains("Path: /albums/{id}/tracks"))
        assertTrue(output.contains("Method: GET"))
        assertTrue(output.contains("Operation ID: get-an-albums-tracks"))
        assertTrue(output.contains("Query Parameters:"))
        assertTrue(output.contains("market") || output.contains("limit") || output.contains("offset"))
    }

    @Test
    fun `findMatchingEndpoint should not find matching endpoint for invalid path`() {
        val url = "https://api.spotify.com/v1/invalid/path"
        val method = "GET"

        findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        assertTrue(output.contains("No matching endpoint found for $url with method $method"))
    }

    @Test
    fun `findMatchingEndpoint should not find matching endpoint for invalid method`() {
        val url = "https://api.spotify.com/v1/albums/4aawyAB9vmqN3uQ7FjRGTy/tracks"
        val method = "POST"

        findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        assertTrue(output.contains("No matching endpoint found for $url with method $method"))
    }

    @Test
    fun `findMatchingEndpoint should handle invalid URLs`() {
        val url = "invalid-url"
        val method = "GET"

        findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        assertTrue(output.contains("No OpenAPI specs found for url: $url"))
    }

    @Test
    fun `findMatchingEndpoint should find artist endpoint in Spotify OpenAPI`() {
        val url = "https://api.spotify.com/v1/artists/0TnOYISbd1XYRBk9myaseg"
        val method = "GET"

        findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        assertTrue(output.contains("Match found in: api_specs/spotify_openapi.json"))
        assertTrue(output.contains("Path: /artists/{id}"))
        assertTrue(output.contains("Method: GET"))
    }

    @Test
    fun `findMatchingEndpoint should find search endpoint in Spotify OpenAPI`() {
        val url = "https://api.spotify.com/v1/search?q=tania%20bowra&type=artist"
        val method = "GET"

        findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        assertTrue(output.contains("Match found in: api_specs/spotify_openapi.json"))
        assertTrue(output.contains("Path: /search"))
        assertTrue(output.contains("Method: GET"))
    }

    @Test
    fun `findMatchingEndpoint should handle complex path parameters in URL`() {
        val url = "https://api.spotify.com/v1/tracks/11dFghVXANMlKmJXsNCbNl"
        val method = "GET"

        findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        assertTrue(output.contains("Match found in: api_specs/spotify_openapi.json"))
        assertTrue(output.contains("Path: /tracks/{id}"))
        assertTrue(output.contains("Method: GET"))
    }

    @Test
    fun `findMatchingEndpoint should handle multiple path segments with parameters`() {
        val url = "https://api.spotify.com/v1/audio-analysis/11dFghVXANMlKmJXsNCbNl"
        val method = "GET"

        findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        assertTrue(output.contains("Match found in: api_specs/spotify_openapi.json"))
        assertTrue(output.contains("Path: /audio-analysis/{id}"))
        assertTrue(output.contains("Method: GET"))
    }
}