package org.example

import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.oas.models.OpenAPI
import java.io.File
import io.ktor.http.Url

/**
 * Class responsible for API validation against OpenAPI specifications.
 */
class ApiValidator(
    specsDirectory: String = "api_specs"
) {

    /**
     * List of OpenAPI specification files to validate against.
     */
    private val openApiFiles: List<String> =
        File(specsDirectory).listFiles()?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
            ?.map { it.path }?.sorted() ?: emptyList()

    /**
     * Parses an OpenAPI specification file.
     *
     * @param filePath Path to the OpenAPI specification file
     * @return Parsed OpenAPI object
     */
    fun parseOpenApi(filePath: String): OpenAPI {
        val openApiJson = File(filePath).readText()
        val parseOptions = ParseOptions()
        parseOptions.isResolve = true
        return OpenAPIV3Parser().readContents(openApiJson, null, parseOptions).openAPI
    }

    /**
     * Finds a matching endpoint in the OpenAPI specifications for a given URL and HTTP method.
     *
     * @param url The URL to find a matching endpoint for
     * @param method The HTTP method to match
     * @return An EndpointMatch object if a match is found, null otherwise
     */
    fun findMatchingEndpoint(url: String, method: String): EndpointMatch? {
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
                            url = url, method = httpMethod.uppercase(), pathPattern = pathPattern, operation = operation
                        )
                    }
                }
            }
        }

        println("No matching endpoint found for $url with method $method")
        return null
    }

    /**
     * Checks if a given URL path matches an OpenAPI path template.
     *
     * @param actualPath The actual URL path
     * @param templatePath The OpenAPI path template
     * @return True if the paths match, false otherwise
     */
    private fun pathsMatch(actualPath: String, templatePath: String): Boolean {
        val regexPattern = templatePath.replace(".", "\\.").split("/").joinToString("/") { segment ->
            if (segment.startsWith("{") && segment.endsWith("}")) {
                "([^/]+)"
            } else {
                segment
            }
        }

        val regex = "^$regexPattern$".toRegex()
        return regex.matches(actualPath)
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
        endpointMatch: EndpointMatch, params: Map<String, String>, requestBody: String?
    ): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Validate parameters
        val expectedParameters = endpointMatch.operation.parameters ?: emptyList()
        val queryParameters = expectedParameters.filter { it.`in` == "query" }

        // Check for missing required parameters
        queryParameters.filter { it.required == true }.forEach { param ->
            if (!params.containsKey(param.name)) {
                errors.add(
                    ValidationError(
                        ValidationErrorType.MISSING_REQUIRED_PARAMETER, param.name
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
                            ValidationErrorType.INVALID_PARAMETER_TYPE, paramName
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
                        ValidationErrorType.MISSING_REQUIRED_BODY
                    )
                )
            } else if (requestBody != null && requestBody.isNotBlank()) {
                // Always use application/json content type
                val mediaTypeSpec = requestBodySpec.content?.get("application/json")

                if (mediaTypeSpec == null) {
                    errors.add(
                        ValidationError(
                            ValidationErrorType.INVALID_CONTENT_TYPE
                        )
                    )
                } else {
                    // Parse JSON body
                    try {
                        val bodyJson = parseJsonBody(requestBody)
                        errors.addAll(validateBodySchema(bodyJson, mediaTypeSpec.schema))
                    } catch (_: Exception) {
                        errors.add(
                            ValidationError(
                                ValidationErrorType.INVALID_BODY_STRUCTURE
                            )
                        )
                    }
                }
            }
        } else if (requestBody != null && requestBody.isNotBlank()) {
            errors.add(
                ValidationError(
                    ValidationErrorType.OTHER
                )
            )
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Validates a parameter value against an OpenAPI schema.
     *
     * @param value The parameter value
     * @param schema The OpenAPI schema to validate against
     * @return True if the parameter is valid, false otherwise
     */
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

    /**
     * Parses a JSON string into a map.
     *
     * @param jsonString The JSON string to parse
     * @return A map representing the JSON data
     */
    private fun parseJsonBody(jsonString: String): Map<String, Any?> {
        // Simple JSON parsing to demonstrate the concept
        // In a real implementation, you would use a proper JSON library
        val result = mutableMapOf<String, Any?>()

        // This is a simplified version - in production code you would use a JSON library
        // like Jackson, Gson, or Kotlin serialization
        // For this example, we'll assume that the parsing worked correctly

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

    /**
     * Validates a JSON body against an OpenAPI schema.
     *
     * @param bodyJson The parsed JSON body
     * @param schema The OpenAPI schema to validate against
     * @return A list of validation errors
     */
    private fun validateBodySchema(
        bodyJson: Map<String, Any?>, schema: io.swagger.v3.oas.models.media.Schema<*>?
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
                            ValidationErrorType.MISSING_REQUIRED_BODY_FIELD, requiredProp
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
                            ValidationErrorType.INVALID_BODY_FIELD_TYPE, propName
                        )
                    )
                }
            }
        }

        return errors
    }

    /**
     * Validates a property value against an OpenAPI schema.
     *
     * @param value The property value
     * @param schema The OpenAPI schema to validate against
     * @return True if the property is valid, false otherwise
     */
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

    /**
     * Displays information about a matched endpoint.
     *
     * @param endpoint The matched endpoint
     */
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
}