package org.example

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
 * @property message A human-readable error message
 * @property field The field that caused the error, if applicable
 */
data class ValidationError(
    val type: ValidationErrorType,
    val message: String,
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