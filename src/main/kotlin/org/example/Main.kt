package org.example

/**
 * Main entry point for the API Health Checker application.
 * This file demonstrates how to use the ApiValidator to validate API requests
 * against OpenAPI specifications.
 */
fun main() {
    val apiValidator = ApiValidator()

    // Example 1: Find and validate a GET endpoint
    val url = "https://api.spotify.com/v1/albums/4aawyAB9vmqN3uQ7FjRGTy/tracks"
    val method = "GET"
    val endpoint = apiValidator.findMatchingEndpoint(url, method)

    if (endpoint != null) {
        apiValidator.displayEndpointMatch(endpoint)

        // Example validation with valid parameters
        val validParams = mapOf(
            "market" to "US", "limit" to "10", "offset" to "0"
        )

        val validationResult = apiValidator.validateRequest(
            endpointMatch = endpoint, params = validParams, requestBody = null
        )

        println("\nValidation Result:")
        println("Valid: ${validationResult.isValid}")

        if (!validationResult.isValid) {
            println("Validation Errors:")
            validationResult.errors.forEach { error ->
                println("- [${error.type}] ${error.message}")
            }
        }

        // Example with invalid parameters (missing required parameter if any)
        println("\nValidation with Missing Parameters:")
        val invalidParams = mapOf<String, String>()
        val invalidValidationResult = apiValidator.validateRequest(
            endpointMatch = endpoint, params = invalidParams, requestBody = null
        )

        println("Valid: ${invalidValidationResult.isValid}")
        if (!invalidValidationResult.isValid) {
            println("Validation Errors:")
            invalidValidationResult.errors.forEach { error ->
                println("- [${error.type}] ${error.message}")
            }
        }
    } else {
        println("No endpoint found")
    }

    // Example 2: Find and validate a POST endpoint with request body
    val postUrl = "https://api.spotify.com/v1/users/31k53kp5qvav5erqjrari3opeagi/playlists"
    val postMethod = "POST"
    val postEndpoint = apiValidator.findMatchingEndpoint(postUrl, postMethod)

    if (postEndpoint != null) {
        println("\n\nTesting POST endpoint:")
        apiValidator.displayEndpointMatch(postEndpoint)

        // Valid request body
        val validBody = """
            {
                "name": "New Playlist",
                "description": "My new playlist description",
                "public": false
            }
        """.trimIndent()

        val postValidationResult = apiValidator.validateRequest(
            endpointMatch = postEndpoint, params = emptyMap(), requestBody = validBody
        )

        println("\nValidation Result:")
        println("Valid: ${postValidationResult.isValid}")

        if (!postValidationResult.isValid) {
            println("Validation Errors:")
            postValidationResult.errors.forEach { error ->
                println("- [${error.type}] ${error.message}")
            }
        }

        // Invalid request body (missing required field)
        val invalidBody = """
            {
                "description": "Missing name field"
            }
        """.trimIndent()

        val invalidPostValidationResult = apiValidator.validateRequest(
            endpointMatch = postEndpoint, params = emptyMap(), requestBody = invalidBody
        )

        println("\nValidation with Invalid Body:")
        println("Valid: ${invalidPostValidationResult.isValid}")

        if (!invalidPostValidationResult.isValid) {
            println("Validation Errors:")
            invalidPostValidationResult.errors.forEach { error ->
                println("- [${error.type}] ${error.message}")
            }
        }
    }
}