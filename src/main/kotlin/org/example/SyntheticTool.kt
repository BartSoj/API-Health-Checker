package org.example

import kotlin.random.Random

/**
 * A tool for generating synthetic HTTP responses
 * Used for demonstration or testing purposes
 */
class SyntheticTool {
    
    /**
     * Generates a synthetic health status for the given request
     *
     * @param request The parsed request
     * @return A simulated API health status
     */
    fun checkHealth(request: ParsedRequest): ApiHealthStatus {
        // Simulate response time between 50ms and 500ms
        val responseTime = Random.nextLong(50, 500)
        
        // List of possible status codes to return
        val possibleStatusCodes = listOf(
            200, 201, 202, 204,  // Success codes
            400, 401, 403, 404,  // Client error codes
            500, 502, 503        // Server error codes
        )
        
        // Weighted random selection (70% success, 20% client error, 10% server error)
        val statusCode = when (Random.nextInt(1, 11)) {
            in 1..7 -> possibleStatusCodes[Random.nextInt(0, 4)]    // 70% success
            in 8..9 -> possibleStatusCodes[Random.nextInt(4, 8)]    // 20% client error
            else -> possibleStatusCodes[Random.nextInt(8, 11)]      // 10% server error
        }
        
        // Determine if the status is healthy (2xx codes)
        val healthy = statusCode in 200..299
        
        // Generate an appropriate error message if not healthy
        val errorMessage = if (!healthy) {
            when (statusCode) {
                in 400..499 -> "Client error: ${getErrorMessageForCode(statusCode)}"
                in 500..599 -> "Server error: ${getErrorMessageForCode(statusCode)}"
                else -> "Unknown error with status code $statusCode"
            }
        } else null
        
        return ApiHealthStatus(
            statusCode = statusCode,
            responseTime = responseTime,
            healthy = healthy,
            errorMessage = errorMessage
        )
    }
    
    /**
     * Gets a descriptive error message for common HTTP status codes
     *
     * @param statusCode The HTTP status code
     * @return A descriptive error message
     */
    private fun getErrorMessageForCode(statusCode: Int): String {
        return when (statusCode) {
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            504 -> "Gateway Timeout"
            else -> "HTTP Status $statusCode"
        }
    }
}