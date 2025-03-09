package org.example

import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.oas.models.OpenAPI
import java.io.File
import io.ktor.http.Url

fun parseOpenApi(filePath: String): OpenAPI {
    val openApiJson = File(filePath).readText()
    val parseOptions = ParseOptions()
    parseOptions.isResolve = true
    return OpenAPIV3Parser().readContents(openApiJson, null, parseOptions).openAPI
}

data class EndpointMatch(
    val url: String,
    val method: String,
    val pathPattern: String,
    val operation: io.swagger.v3.oas.models.Operation
)

fun findMatchingEndpoint(url: String, method: String): EndpointMatch? {
    val openApiFiles = listOf(
        "api_specs/calendar_openapi.json",
        "api_specs/discord_openapi.json",
        "api_specs/docs_openapi.json",
        "api_specs/drive_openapi.json",
        "api_specs/gmail_openapi.json",
        "api_specs/people_openapi.json",
        "api_specs/script_openapi.json",
        "api_specs/sheets_openapi.json",
        "api_specs/slides_openapi.json",
        "api_specs/spotify_openapi.json",
        "api_specs/tasks_openapi.json",
        "api_specs/wolfram_openapi.json"
    )

    val requestUrl = Url(url)
    val requestDomain = requestUrl.host
    val requestPath = requestUrl.encodedPath
    val httpMethod = method.lowercase()

    // Find specs with matching domains in server URLs
    val matchingSpecs = mutableListOf<OpenAPI>()

    openApiFiles.forEach { filePath ->
        try {
            val openApiSpec = parseOpenApi(filePath)
            val serverUrls = openApiSpec.servers?.mapNotNull { server ->
                try {
                    Url(server.url).host
                } catch (_: Exception) {
                    null
                }
            } ?: emptyList()

            if (serverUrls.any { it == requestDomain }) {
                matchingSpecs.add(openApiSpec)
            }
        } catch (e: Exception) {
            println("Error parsing $filePath: ${e.message}")
        }
    }

    if (matchingSpecs.isEmpty()) {
        println("No OpenAPI specs found for url: $url")
        return null
    }

    // Find matching endpoint in the specs with matching domains
    matchingSpecs.forEach { spec ->
        // Extract the server path to remove it from the request path
        val serverPath = spec.servers?.firstOrNull()?.url?.let {
            try {
                val serverUrl = Url(it)
                serverUrl.encodedPath.removeSuffix("/")
            } catch (_: Exception) {
                ""
            }
        } ?: ""

        // Remove the server path from the request path to get the endpoint
        val apiPath = if (serverPath.isNotEmpty() && requestPath.startsWith(serverPath)) {
            requestPath.substring(serverPath.length)
        } else {
            requestPath
        }

        // Find the matching endpoint in the spec
        spec.paths?.forEach { (pathPattern, pathItem) ->
            if (pathsMatch(apiPath, pathPattern)) {
                val operation = when (httpMethod) {
                    "get" -> pathItem.get
                    "post" -> pathItem.post
                    "put" -> pathItem.put
                    "delete" -> pathItem.delete
                    "patch" -> pathItem.patch
                    "options" -> pathItem.options
                    "head" -> pathItem.head
                    else -> null
                }

                if (operation != null) {
                    return EndpointMatch(
                        url = url,
                        method = httpMethod.uppercase(),
                        pathPattern = pathPattern,
                        operation = operation
                    )
                }
            }
        }
    }

    println("No matching endpoint found for $url with method $method")
    return null
}

private fun pathsMatch(actualPath: String, templatePath: String): Boolean {
    val regexPattern = templatePath
        .replace(".", "\\.")
        .split("/")
        .joinToString("/") { segment ->
            if (segment.startsWith("{") && segment.endsWith("}")) {
                "([^/]+)"
            } else {
                segment
            }
        }

    val regex = "^$regexPattern$".toRegex()
    return regex.matches(actualPath)
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList()
)

data class ValidationError(
    val type: ValidationErrorType,
    val message: String,
    val field: String? = null
)

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
 * Validates if a request conforms to the endpoint specification.
 *
 * @param endpointMatch The matched endpoint specification
 * @param params The query parameters to validate as a map of parameter name to value
 * @param requestBody The request body as a JSON string to validate
 * @return A ValidationResult indicating if the request is valid and any validation errors
 */
fun validateRequest(
    endpointMatch: EndpointMatch,
    params: Map<String, String>,
    requestBody: String?
): ValidationResult {
    val errors = mutableListOf<ValidationError>()

    // Validate parameters
    val expectedParameters = endpointMatch.operation.parameters ?: emptyList()
    val queryParameters = expectedParameters.filter { it.`in` == "query" }

    // Check for missing required parameters
    queryParameters.filter { it.required == true }
        .forEach { param ->
            if (!params.containsKey(param.name)) {
                errors.add(
                    ValidationError(
                        ValidationErrorType.MISSING_REQUIRED_PARAMETER,
                        "Missing required query parameter: ${param.name}",
                        param.name
                    )
                )
            }
        }

    // Check parameter types
    params.forEach { (paramName, paramValue) ->
        val paramSpec = queryParameters.find { it.name == paramName }
        if (paramSpec != null) {
            val schema = paramSpec.schema
            if (!validateParameterType(paramValue, schema)) {
                errors.add(
                    ValidationError(
                        ValidationErrorType.INVALID_PARAMETER_TYPE,
                        "Query parameter $paramName has invalid type or format",
                        paramName
                    )
                )
            }
        }
    }

    // Validate request body if needed
    val requestBodySpec = endpointMatch.operation.requestBody
    if (requestBodySpec != null) {
        // Check if request body is required but missing
        if (requestBodySpec.required == true && requestBody.isNullOrBlank()) {
            errors.add(
                ValidationError(
                    ValidationErrorType.MISSING_REQUIRED_BODY,
                    "Request body is required but was not provided"
                )
            )
        } else if (requestBody != null && requestBody.isNotBlank()) {
            // Always use application/json content type
            val mediaTypeSpec = requestBodySpec.content?.get("application/json")

            if (mediaTypeSpec == null) {
                errors.add(
                    ValidationError(
                        ValidationErrorType.INVALID_CONTENT_TYPE,
                        "Unsupported content type: application/json"
                    )
                )
            } else {
                // Parse JSON body
                try {
                    val bodyJson = parseJsonBody(requestBody)
                    errors.addAll(validateBodySchema(bodyJson, mediaTypeSpec.schema))
                } catch (e: Exception) {
                    errors.add(
                        ValidationError(
                            ValidationErrorType.INVALID_BODY_STRUCTURE,
                            "Invalid JSON format: ${e.message}"
                        )
                    )
                }
            }
        }
    } else if (requestBody != null && requestBody.isNotBlank()) {
        errors.add(
            ValidationError(
                ValidationErrorType.OTHER,
                "Request body provided but not expected for this endpoint"
            )
        )
    }

    return ValidationResult(errors.isEmpty(), errors)
}

private fun validateParameterType(value: String, schema: io.swagger.v3.oas.models.media.Schema<*>?): Boolean {
    if (schema == null) return true

    return when (schema.type) {
        "string" -> true  // All values can be strings
        "integer", "number" -> value.toDoubleOrNull() != null
        "boolean" -> value.lowercase() in listOf("true", "false", "1", "0")
        "array" -> {
            try {
                // Simple check for comma-separated values
                val items = value.split(",")
                items.isNotEmpty()
            } catch (_: Exception) {
                false
            }
        }

        else -> true  // Default to true for unknown types
    }
}

private fun parseJsonBody(jsonString: String): Map<String, Any?> {
    // Simple JSON parsing to demonstrate the concept
    // In a real implementation, you would use a proper JSON library
    val result = mutableMapOf<String, Any?>()

    // Mock parse operation - in reality this would use a JSON parser
    if (jsonString.isNotBlank() && jsonString.startsWith("{") && jsonString.endsWith("}")) {
        val content = jsonString.substring(1, jsonString.length - 1).trim()
        content.split(",").forEach { pair ->
            val keyValue = pair.split(":", limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0].trim().removeSurrounding("\"")
                val value = keyValue[1].trim().removeSurrounding("\"")
                result[key] = value
            }
        }
    }

    return result
}

private fun validateBodySchema(
    bodyJson: Map<String, Any?>,
    schema: io.swagger.v3.oas.models.media.Schema<*>?
): List<ValidationError> {
    val errors = mutableListOf<ValidationError>()

    if (schema == null) return errors

    // Validate object schema
    if (schema.type == "object" && schema.properties != null) {
        // Check required properties
        schema.required?.forEach { requiredProp ->
            if (!bodyJson.containsKey(requiredProp)) {
                errors.add(
                    ValidationError(
                        ValidationErrorType.MISSING_REQUIRED_BODY_FIELD,
                        "Missing required field in request body: $requiredProp",
                        requiredProp
                    )
                )
            }
        }

        // Check property types
        bodyJson.forEach { (propName, propValue) ->
            val propSchema = schema.properties[propName] ?: return@forEach

            if (!validatePropertyType(propValue, propSchema)) {
                errors.add(
                    ValidationError(
                        ValidationErrorType.INVALID_BODY_FIELD_TYPE,
                        "Field $propName has invalid type or format",
                        propName
                    )
                )
            }
        }
    }

    return errors
}

private fun validatePropertyType(value: Any?, schema: io.swagger.v3.oas.models.media.Schema<*>): Boolean {
    if (value == null) return true

    return when (schema.type) {
        "string" -> value is String
        "integer" -> value is Int || value is Long || (value is String && value.toIntOrNull() != null)
        "number" -> value is Double || value is Float || (value is String && value.toDoubleOrNull() != null)
        "boolean" -> value is Boolean || (value is String && value.lowercase() in listOf("true", "false"))
        "array" -> value is List<*> || value is Array<*>
        "object" -> value is Map<*, *>
        else -> true
    }
}

fun displayEndpointMatch(endpoint: EndpointMatch) {
    println("URL: ${endpoint.url}")
    println("Method: ${endpoint.method}")
    println("Path Pattern: ${endpoint.pathPattern}")

    println("\nParameters:")
    val parameters = endpoint.operation.parameters ?: emptyList()
    if (parameters.isEmpty()) {
        println("  None")
    } else {
        parameters.forEach { param ->
            println("  - ${param.name} (${param.`in`}, required: ${param.required ?: false}): ${param.description}")
            println("    Type: ${param.schema?.type ?: "unknown"}")
        }
    }

    println("\nRequest Body:")
    val requestBody = endpoint.operation.requestBody
    if (requestBody == null) {
        println("  None")
    } else {
        println("  Required: ${requestBody.required ?: false}")
        println("  Description: ${requestBody.description ?: "No description"}")

        requestBody.content?.forEach { (contentType, mediaType) ->
            println("  Content Type: $contentType")
            println("  Schema Type: ${mediaType.schema?.type ?: "unknown"}")

            val properties = mediaType.schema?.properties
            if (properties != null && properties.isNotEmpty()) {
                println("  Properties:")
                properties.forEach { (propName, propSchema) ->
                    println("    - $propName (${propSchema.type ?: "unknown"}): ${propSchema.description ?: "No description"}")
                }
            }
        }
    }

    println("\n------")
}

fun main() {
    // Example 1: Find and validate an endpoint
    val url = "https://api.spotify.com/v1/albums/4aawyAB9vmqN3uQ7FjRGTy/tracks"
    val method = "GET"
    val endpoint = findMatchingEndpoint(url, method)

    if (endpoint != null) {
        displayEndpointMatch(endpoint)

        // Example validation with valid parameters
        val validParams = mapOf(
            "market" to "US",
            "limit" to "10",
            "offset" to "0"
        )

        val validationResult = validateRequest(
            endpointMatch = endpoint,
            params = validParams,
            requestBody = null
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
        val invalidValidationResult = validateRequest(
            endpointMatch = endpoint,
            params = invalidParams,
            requestBody = null
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

    // Example 2: POST request with body validation
    val postUrl = "https://api.spotify.com/v1/users/31k53kp5qvav5erqjrari3opeagi/playlists"
    val postMethod = "POST"
    val postEndpoint = findMatchingEndpoint(postUrl, postMethod)

    if (postEndpoint != null) {
        println("\n\nTesting POST endpoint:")
        displayEndpointMatch(postEndpoint)

        // Valid request body
        val validBody = """
            {
                "name": "New Playlist",
                "description": "My new playlist description",
                "public": false
            }
        """.trimIndent()

        val postValidationResult = validateRequest(
            endpointMatch = postEndpoint,
            params = emptyMap(),
            requestBody = validBody
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

        val invalidPostValidationResult = validateRequest(
            endpointMatch = postEndpoint,
            params = emptyMap(),
            requestBody = invalidBody
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