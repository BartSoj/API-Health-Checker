package org.example

/**
 * Determines which tool to use for API health checking
 */
enum class HealthCheckMode {
    HTTP_CLIENT,
    SYNTHETIC_TOOL
}

/**
 * Represents a matched endpoint in an OpenAPI specification.
 *
 * @property url The URL that matched the endpoint
 * @property method The HTTP method of the endpoint
 * @property pathPattern The path pattern from the OpenAPI spec
 * @property operation The operation object from the OpenAPI spec
 */
data class EndpointMatch(
    val url: String,
    val method: String,
    val pathPattern: String,
    val operation: io.swagger.v3.oas.models.Operation
)

/**
 * Represents the result of validating a request against an OpenAPI specification.
 *
 * @property isValid Whether the request is valid
 * @property errors Any validation errors that occurred
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList()
)

/**
 * Represents a validation error.
 *
 * @property type The type of validation error
 * @property field The field that caused the error, if applicable
 */
data class ValidationError(
    val type: ValidationErrorType,
    val field: String? = null
)

/**
 * The types of validation errors that can occur.
 */
enum class ValidationErrorType {
    MISSING_REQUIRED_PARAMETER,
    INVALID_PARAMETER_TYPE,
    MISSING_REQUIRED_BODY,
    INVALID_CONTENT_TYPE,
    INVALID_BODY_STRUCTURE,
    INVALID_BODY_FIELD_TYPE,
    MISSING_REQUIRED_BODY_FIELD,
    OTHER
}

/**
 * Data class representing the health status of an API endpoint
 *
 * @property statusCode The HTTP status code returned by the endpoint, or -1 for errors
 * @property responseTime The time in milliseconds the endpoint took to respond
 * @property healthy Whether the endpoint is considered healthy (based on status code)
 * @property errorMessage An error message if the endpoint is unhealthy, null otherwise
 */
data class ApiHealthStatus(
    val statusCode: Int,
    val responseTime: Long,
    val healthy: Boolean,
    val errorMessage: String?
)

/**
 * Data class representing a parsed API request from user input
 *
 * @property url The URL to send the request to
 * @property method The HTTP method to use
 * @property queryParams Query parameters to include in the request
 * @property headers Headers to include in the request
 * @property body Body to include in the request
 */
data class ParsedRequest(
    val url: String,
    val method: String = "GET",
    val queryParams: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
)