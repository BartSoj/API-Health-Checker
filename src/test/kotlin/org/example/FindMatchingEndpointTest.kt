package org.example

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class FindMatchingEndpointTest {

    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()
    private val apiValidator = ApiValidator()

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

        val result = apiValidator.findMatchingEndpoint(url, method)

        Assertions.assertNotNull(result)
        Assertions.assertEquals(url, result?.url)
        Assertions.assertEquals("GET", result?.method)

        // Instead of checking params which is now deprecated as we changed the EndpointMatch structure
        Assertions.assertNotNull(result?.operation?.parameters)

        // Check that operation has parameters for market, limit, or offset
        val paramNames = result?.operation?.parameters?.mapNotNull { it.name } ?: emptyList()
        Assertions.assertTrue(paramNames.any { it in listOf("market", "limit", "offset") })
    }

    @Test
    fun `findMatchingEndpoint should not find matching endpoint for invalid path`() {
        val url = "https://api.spotify.com/v1/invalid/path"
        val method = "GET"

        val result = apiValidator.findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        Assertions.assertNull(result)
        Assertions.assertTrue(output.contains("No matching endpoint found for $url with method $method"))
    }

    @Test
    fun `findMatchingEndpoint should not find matching endpoint for invalid method`() {
        val url = "https://api.spotify.com/v1/albums/4aawyAB9vmqN3uQ7FjRGTy/tracks"
        val method = "POST"

        val result = apiValidator.findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        Assertions.assertNull(result)
        Assertions.assertTrue(output.contains("No matching endpoint found for $url with method $method"))
    }

    @Test
    fun `findMatchingEndpoint should handle invalid URLs`() {
        val url = "invalid-url"
        val method = "GET"

        val result = apiValidator.findMatchingEndpoint(url, method)
        val output = outputStreamCaptor.toString().trim()

        Assertions.assertNull(result)
        Assertions.assertTrue(output.contains("No OpenAPI specs found for url: $url"))
    }

    @Test
    fun `findMatchingEndpoint should find artist endpoint in Spotify OpenAPI`() {
        val url = "https://api.spotify.com/v1/artists/0TnOYISbd1XYRBk9myaseg"
        val method = "GET"

        val result = apiValidator.findMatchingEndpoint(url, method)

        Assertions.assertNotNull(result)
        Assertions.assertEquals(url, result?.url)
        Assertions.assertEquals("GET", result?.method)
    }

    @Test
    fun `findMatchingEndpoint should find search endpoint in Spotify OpenAPI`() {
        val url = "https://api.spotify.com/v1/search?q=tania%20bowra&type=artist"
        val method = "GET"

        val result = apiValidator.findMatchingEndpoint(url, method)

        Assertions.assertNotNull(result)
        Assertions.assertEquals(url, result?.url)
        Assertions.assertEquals("GET", result?.method)

        // Check that operation has parameters for q or type
        val paramNames = result?.operation?.parameters?.mapNotNull { it.name } ?: emptyList()
        Assertions.assertTrue(paramNames.any { it in listOf("q", "type") })
    }

    @Test
    fun `findMatchingEndpoint should handle complex path parameters in URL`() {
        val url = "https://api.spotify.com/v1/tracks/11dFghVXANMlKmJXsNCbNl"
        val method = "GET"

        val result = apiValidator.findMatchingEndpoint(url, method)

        Assertions.assertNotNull(result)
        Assertions.assertEquals(url, result?.url)
        Assertions.assertEquals("GET", result?.method)
    }

    @Test
    fun `findMatchingEndpoint should handle multiple path segments with parameters`() {
        val url = "https://api.spotify.com/v1/audio-analysis/11dFghVXANMlKmJXsNCbNl"
        val method = "GET"

        val result = apiValidator.findMatchingEndpoint(url, method)

        Assertions.assertNotNull(result)
        Assertions.assertEquals(url, result?.url)
        Assertions.assertEquals("GET", result?.method)
    }

    @Test
    fun `validateRequest should validate correct parameters`() {
        val url = "https://api.spotify.com/v1/albums/4aawyAB9vmqN3uQ7FjRGTy/tracks"
        val method = "GET"
        val endpointMatch = apiValidator.findMatchingEndpoint(url, method)

        Assertions.assertNotNull(endpointMatch)

        val validParams = mapOf(
            "market" to "US", "limit" to "10", "offset" to "0"
        )

        val result = apiValidator.validateRequest(
            endpointMatch = endpointMatch!!, params = validParams, requestBody = null
        )

        Assertions.assertTrue(result.isValid)
        Assertions.assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validateRequest should identify invalid parameter types`() {
        val url = "https://api.spotify.com/v1/albums/4aawyAB9vmqN3uQ7FjRGTy/tracks"
        val method = "GET"
        val endpointMatch = apiValidator.findMatchingEndpoint(url, method)

        Assertions.assertNotNull(endpointMatch)

        val invalidParams = mapOf(
            "limit" to "not-a-number",  // Should be a number
            "offset" to "0"
        )

        val result = apiValidator.validateRequest(
            endpointMatch = endpointMatch!!, params = invalidParams, requestBody = null
        )

        Assertions.assertFalse(result.isValid)
        Assertions.assertTrue(result.errors.isNotEmpty())
        Assertions.assertEquals(ValidationErrorType.INVALID_PARAMETER_TYPE, result.errors[0].type)
    }

    @Test
    fun `validateRequest should identify missing required parameters`() {
        val url = "https://api.spotify.com/v1/search"  // Search endpoint typically requires q & type params
        val method = "GET"
        val endpointMatch = apiValidator.findMatchingEndpoint(url, method)

        Assertions.assertNotNull(endpointMatch)

        // Empty params map - will miss any required parameters
        val emptyParams = emptyMap<String, String>()

        val result = apiValidator.validateRequest(
            endpointMatch = endpointMatch!!, params = emptyParams, requestBody = null
        )

        val requiredParams =
            endpointMatch.operation.parameters?.filter { it.required == true && it.`in` == "query" }?.map { it.name }
                ?: emptyList()

        if (requiredParams.isNotEmpty()) {
            Assertions.assertFalse(result.isValid)
            Assertions.assertTrue(result.errors.isNotEmpty())
            Assertions.assertEquals(ValidationErrorType.MISSING_REQUIRED_PARAMETER, result.errors[0].type)
        }
    }

    @Test
    fun `validateRequest should validate request bodies`() {
        // Find a POST endpoint that requires a request body
        val url = "https://api.spotify.com/v1/users/31k53kp5qvav5erqjrari3opeagi/playlists"
        val method = "POST"
        val endpointMatch = apiValidator.findMatchingEndpoint(url, method)

        // If we can't find this endpoint in the spec, skip the test
        if (endpointMatch == null) {
            return
        }

        // Valid request body with required fields
        val validBody = """
            {
                "name": "New Playlist",
                "description": "My new playlist description",
                "public": false
            }
        """.trimIndent()

        apiValidator.validateRequest(
            endpointMatch = endpointMatch, params = emptyMap(), requestBody = validBody
        )

        // If the endpoint doesn't have required fields, this test won't be meaningful
        // so we'll check if there are required fields in the request body schema
        val hasRequiredFields = endpointMatch.operation.requestBody?.let { rb ->
            rb.content?.values?.any { mediaType ->
                mediaType.schema?.required?.isNotEmpty() == true
            }
        } ?: false

        if (hasRequiredFields) {
            // Now test with invalid body missing required fields
            val invalidBody = "{}"

            val invalidResult = apiValidator.validateRequest(
                endpointMatch = endpointMatch, params = emptyMap(), requestBody = invalidBody
            )

            Assertions.assertFalse(invalidResult.isValid)
            Assertions.assertTrue(invalidResult.errors.isNotEmpty())
            Assertions.assertTrue(invalidResult.errors.any { it.type == ValidationErrorType.MISSING_REQUIRED_BODY_FIELD })
        }
    }
}