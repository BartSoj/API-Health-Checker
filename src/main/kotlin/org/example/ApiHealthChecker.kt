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
                } catch (_: Exception) {
                    "I'm having difficulty connecting to ${parsedRequest.url}. This could be due to network issues, the server being down, or an incorrect URL."
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
            "I'm having trouble connecting to $url. ${status.errorMessage ?: "The server isn't responding or there might be a network issue"}."
        } else {
            val healthStatus = if (status.healthy) "healthy" else "experiencing issues"
            val responseTime = "${status.responseTime}ms"

            when {
                status.healthy -> "The API at $url is responding correctly with a ${status.statusCode} status code. " + "The response time was $responseTime, which ${if (status.responseTime < 300) "is excellent" else if (status.responseTime < 1000) "is good" else "is a bit slow but acceptable"}."

                status.errorMessage != null -> "I was able to reach $url, but the API is $healthStatus. " + "It returned a ${status.statusCode} status code in $responseTime. " + "The specific error is: ${status.errorMessage}."

                else -> "I reached $url and got a ${status.statusCode} status code in $responseTime. " + "The API appears to be $healthStatus."
            }
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

            return "I couldn't find an API endpoint for $method $path on $host. " + "Please check that the URL is correct and the API is supported by our OpenAPI specifications."
        } catch (_: Exception) {
            return "The URL format '$url' doesn't seem to be valid. A proper URL should look like 'https://example.com/api/resource'."
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
        val sb =
            StringBuilder("I noticed some issues with your request to ${endpoint.pathPattern} using ${endpoint.method}: ")

        if (result.errors.isEmpty()) {
            return sb.append("There appears to be an issue, but I couldn't determine the exact problem.").toString()
        }

        // Group errors by type for better organization
        val errorsByType = result.errors.groupBy { it.type }

        errorsByType.forEach { (type, errors) ->
            when (type) {
                ValidationErrorType.MISSING_REQUIRED_PARAMETER -> {
                    val params = errors.mapNotNull { it.field }.joinToString(", ")
                    sb.append("You need to provide the following required parameter${if (errors.size > 1) "s" else ""}: $params. ")
                }

                ValidationErrorType.INVALID_PARAMETER_TYPE -> {
                    val params = errors.mapNotNull { it.field }.joinToString(", ")
                    sb.append("The value${if (errors.size > 1) "s" else ""} for parameter${if (errors.size > 1) "s" else ""} $params ${if (errors.size > 1) "are" else "is"} not in the correct format. ")
                }

                ValidationErrorType.MISSING_REQUIRED_BODY -> {
                    sb.append("This endpoint requires a request body, but none was provided. ")
                }

                ValidationErrorType.INVALID_CONTENT_TYPE -> {
                    sb.append("The API only accepts application/json content type for this request. ")
                }

                ValidationErrorType.INVALID_BODY_STRUCTURE -> {
                    sb.append("The JSON body you provided isn't properly formatted. Please check for syntax errors. ")
                }

                ValidationErrorType.INVALID_BODY_FIELD_TYPE -> {
                    val fields = errors.mapNotNull { it.field }.joinToString(", ")
                    sb.append("The type${if (errors.size > 1) "s" else ""} of field${if (errors.size > 1) "s" else ""} $fields in your request body ${if (errors.size > 1) "are" else "is"} incorrect. ")
                }

                ValidationErrorType.MISSING_REQUIRED_BODY_FIELD -> {
                    val fields = errors.mapNotNull { it.field }.joinToString(", ")
                    sb.append("Your request body is missing required field${if (errors.size > 1) "s" else ""}: $fields. ")
                }

                ValidationErrorType.OTHER -> {
                    sb.append("There's an issue with your request body - it wasn't expected for this endpoint. ")
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