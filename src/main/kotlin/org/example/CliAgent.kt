package org.example

import kotlin.random.Random

/**
 * Agent class for processing API health check requests
 * Orchestrates parsing, validation, and execution of API health checks
 */
class CliAgent {
    private val apiValidator = ApiValidator()
    private val apiHealthChecker = ApiHealthChecker()
    private val syntheticTool = SyntheticTool()

    /**
     * Process a user request to check the health of an API endpoint
     *
     * @param request The user request string
     * @return A response message to display to the user
     */
    fun processRequest(request: String): String {
        // Parse the input request
        val parsedRequest = RequestParser.parse(request)

        if (parsedRequest == null) {
            return "Invalid request format. Expected: \"Determine the status of <URL> [with query parameters <params>] [with headers <headers>] [and body <body>]\"."
        }

        // Validate the request against OpenAPI specifications
        val endpoint = apiValidator.findMatchingEndpoint(parsedRequest.url, parsedRequest.method)

        if (endpoint == null) {
            return "Invalid request: The endpoint or request parameters do not match our OpenAPI specification."
        }

        // Validate request parameters against the OpenAPI spec
        val validationResult = apiValidator.validateRequest(
            endpointMatch = endpoint,
            params = parsedRequest.queryParams,
            requestBody = parsedRequest.body
        )

        if (!validationResult.isValid) {
            return "Invalid request: The endpoint or request parameters do not match our OpenAPI specification."
        }

        // Decide which tool to use for checking health
        return if (shouldUseSyntheticTool()) {
            // Use synthetic tool
            val status = syntheticTool.checkHealth(parsedRequest)
            formatResponse(parsedRequest.url, status)
        } else {
            // Use real HTTP client
            try {
                val status = apiHealthChecker.checkHealth(
                    url = parsedRequest.url,
                    method = parsedRequest.method,
                    params = parsedRequest.queryParams,
                    body = parsedRequest.body,
                    headers = parsedRequest.headers
                )
                formatResponse(parsedRequest.url, status)
            } catch (e: Exception) {
                "Error: Unable to reach the specified URL."
            }
        }
    }

    /**
     * Formats the API health status response according to the required output format
     *
     * @param url The URL that was checked
     * @param status The API health status
     * @return A formatted response string
     */
    private fun formatResponse(url: String, status: ApiHealthStatus): String {
        return if (status.statusCode == -1) {
            "Error: Unable to reach the specified URL."
        } else {
            "The HTTP status of $url is ${status.statusCode}."
        }
    }

    /**
     * Decides whether to use the synthetic tool instead of real HTTP requests
     * This can be based on various conditions like specific domains, testing mode, etc.
     *
     * @return True if the synthetic tool should be used, false otherwise
     */
    private fun shouldUseSyntheticTool(): Boolean {
        return false  // Temporary, think of better way how to determine when to use synthetic tool
    }

    /**
     * Cleanup resources when the agent is no longer needed
     */
    fun close() {
        apiHealthChecker.close()
    }
}