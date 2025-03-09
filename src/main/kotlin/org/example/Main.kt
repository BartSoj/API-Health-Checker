package org.example

/**
 * Main entry point for the API Health Checker application.
 * This file demonstrates how to use the ApiValidator to validate API requests
 * against OpenAPI specifications and the ApiHealthChecker to check API health.
 */
fun main() {
    println("===== API VALIDATION EXAMPLE =====")
    val apiValidator = ApiValidator()

    val validationUrl = "https://api.spotify.com/v1/albums/4aawyAB9vmqN3uQ7FjRGTy/tracks"
    val method = "GET"
    val endpoint = apiValidator.findMatchingEndpoint(validationUrl, method)

    if (endpoint != null) {
        apiValidator.displayEndpointMatch(endpoint)

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
    } else {
        println("No matching endpoint found for $method $validationUrl")
    }

    println("\n===== API HEALTH CHECKING EXAMPLE =====")
    val healthChecker = ApiHealthChecker()

    try {
        val postUrl = "https://jsonplaceholder.typicode.com/posts"
        println("\nChecking POST endpoint: $postUrl")
        val postBody = """
            {
                "title": "foo",
                "body": "bar",
                "userId": 1
            }
        """.trimIndent()

        val postHealthStatus = healthChecker.checkHealth(
            url = postUrl, method = "POST", body = postBody, headers = mapOf("Content-Type" to "application/json")
        )

        println("Health Status:")
        println("  Status Code: ${postHealthStatus.statusCode}")
        println("  Response Time: ${postHealthStatus.responseTime}ms")
        println("  Healthy: ${postHealthStatus.healthy}")
        if (postHealthStatus.errorMessage != null) {
            println("  Error: ${postHealthStatus.errorMessage}")
        }

    } finally {
        healthChecker.close()
    }
}