package org.example

/**
 * Main orchestration class for the API Health Checker application.
 * Coordinates parsing, validation, and execution of API health checks.
 */
class ApiHealthChecker(
    private val apiValidator: ApiValidator,
    private val httpRequestTester: HttpRequestTester,
    private val syntheticTool: SyntheticTool,
    private val requestParser: RequestParser,
    private val healthCheckMode: HealthCheckMode = HealthCheckMode.HTTP_CLIENT
) {

    /**
     * Process a user request to check the health of an API endpoint
     *
     * @param request The user request string
     * @return A response message to display to the user
     */
    fun processRequest(request: String): String {
        val parsedRequest = requestParser.parse(request)

        if (parsedRequest == null) {
            return "Invalid request format. Expected: \"Determine the status of <URL> using <METHOD> [with query parameters <params>] [with headers <headers>] [and body <body>]\"."
        }

        // Validate the request against OpenAPI specifications
        val endpoint = apiValidator.findMatchingEndpoint(parsedRequest.url, parsedRequest.method)

        if (endpoint == null) {
            return formatEndpointNotFoundError(parsedRequest.url, parsedRequest.method)
        }

        // Validate request parameters against the OpenAPI spec
        val validationResult = apiValidator.validateRequest(
            endpointMatch = endpoint, params = parsedRequest.queryParams, requestBody = parsedRequest.body
        )

        if (!validationResult.isValid) {
            return formatValidationError(validationResult, endpoint)
        }

        // Use the tool specified by the healthCheckMode
        return when (healthCheckMode) {
            HealthCheckMode.SYNTHETIC_TOOL -> {
                // Use synthetic tool
                val status = syntheticTool.checkHealth(parsedRequest)
                formatResponse(parsedRequest.url, status)
            }

            HealthCheckMode.HTTP_CLIENT -> {
                // Use real HTTP client
                try {
                    val status = httpRequestTester.checkHealth(
                        url = parsedRequest.url,
                        method = parsedRequest.method,
                        params = parsedRequest.queryParams,
                        body = parsedRequest.body,
                        headers = parsedRequest.headers
                    )
                    formatResponse(parsedRequest.url, status)
                } catch (e: Exception) {
                    "Error: Unable to reach ${parsedRequest.url}: ${e.message ?: "Unknown error occurred"}"
                }
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
            "Error: Unable to reach $url: ${status.errorMessage ?: "Connection failed"}"
        } else {
            val healthStatus = if (status.healthy) "healthy" else "unhealthy"
            val responseTime = "${status.responseTime}ms"

            "The HTTP status of $url is ${status.statusCode} ($healthStatus, response time: $responseTime)." + if (!status.healthy && status.errorMessage != null) " Error: ${status.errorMessage}" else ""
        }
    }

    /**
     * Formats error message for cases when no matching endpoint is found
     *
     * @param url The URL that was checked
     * @param method The HTTP method that was used
     * @return A formatted error message with detailed information
     */
    private fun formatEndpointNotFoundError(url: String, method: String): String {
        try {
            val urlObj = io.ktor.http.Url(url)
            val host = urlObj.host
            val path = urlObj.encodedPath

            return "No matching endpoint found for $method $path on host $host. " + "Please check that the URL is correct and the API is supported by our OpenAPI specifications."
        } catch (_: Exception) {
            return "Invalid URL format for $url. Please provide a valid URL."
        }
    }

    /**
     * Formats validation error messages based on the validation result
     *
     * @param result The validation result containing errors
     * @param endpoint The matched endpoint information
     * @return A formatted error message with detailed information
     */
    private fun formatValidationError(result: ValidationResult, endpoint: EndpointMatch): String {
        val sb = StringBuilder("Invalid request for ${endpoint.method} ${endpoint.pathPattern}: ")

        if (result.errors.isEmpty()) {
            return sb.append("Unknown validation error.").toString()
        }

        // Group errors by type for better organization
        val errorsByType = result.errors.groupBy { it.type }

        errorsByType.forEach { (type, errors) ->
            when (type) {
                ValidationErrorType.MISSING_REQUIRED_PARAMETER -> {
                    sb.append("Missing required query parameter(s): ")
                    sb.append(errors.mapNotNull { it.field }.joinToString(", "))
                }

                ValidationErrorType.INVALID_PARAMETER_TYPE -> {
                    sb.append("Parameter type error(s): ")
                    errors.forEach { error ->
                        sb.append("${error.field} - ${error.message}. ")
                    }
                }

                ValidationErrorType.MISSING_REQUIRED_BODY -> {
                    sb.append("Request body is required but was not provided. ")
                }

                ValidationErrorType.INVALID_CONTENT_TYPE -> {
                    sb.append("Content type error: ${errors.firstOrNull()?.message ?: "Unsupported content type"}. ")
                }

                ValidationErrorType.INVALID_BODY_STRUCTURE -> {
                    sb.append("Invalid JSON body structure: ${errors.firstOrNull()?.message}. ")
                }

                ValidationErrorType.INVALID_BODY_FIELD_TYPE -> {
                    sb.append("Invalid field type(s) in request body: ")
                    sb.append(errors.mapNotNull { it.field }.joinToString(", "))
                }

                ValidationErrorType.MISSING_REQUIRED_BODY_FIELD -> {
                    sb.append("Missing required field(s) in request body: ")
                    sb.append(errors.mapNotNull { it.field }.joinToString(", "))
                }

                ValidationErrorType.OTHER -> {
                    sb.append(errors.firstOrNull()?.message ?: "Unknown error")
                }
            }
        }

        return sb.toString()
    }


    /**
     * Cleanup resources when the health checker is no longer needed
     */
    fun close() {
        httpRequestTester.close()
    }
}