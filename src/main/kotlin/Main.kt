package org.example

import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import java.io.File
import io.ktor.http.Url

fun parseOpenApi(filePath: String): OpenAPI {
    val openApiJson = File(filePath).readText()
    val parseOptions = ParseOptions()
    parseOptions.isResolve = true
    return OpenAPIV3Parser().readContents(openApiJson, null, parseOptions).openAPI
}

fun findMatchingEndpoint(url: String, method: String) {
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
    val matchingSpecs = mutableListOf<Pair<String, OpenAPI>>()

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
                matchingSpecs.add(filePath to openApiSpec)
            }
        } catch (e: Exception) {
            println("Error parsing $filePath: ${e.message}")
        }
    }

    if (matchingSpecs.isEmpty()) {
        println("No OpenAPI specs found for url: $url")
        return
    }

    // Find matching endpoint in the specs with matching domains
    var foundEndpoint = false

    matchingSpecs.forEach { (filePath, spec) ->
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
                    foundEndpoint = true
                    println("Match found in: $filePath")
                    println("Path: $pathPattern")
                    println("Method: ${httpMethod.uppercase()}")
                    displayOperationDetails(operation)
                }
            }
        }
    }

    if (!foundEndpoint) {
        println("No matching endpoint found for $url with method $method")
    }
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

private fun displayOperationDetails(operation: Operation) {
    println("\nOperation ID: ${operation.operationId ?: "Not specified"}")
    println("Description: ${operation.description ?: "Not specified"}")

    println("\nQuery Parameters:")
    val queryParams = operation.parameters?.filter { it.`in` == "query" } ?: emptyList()
    if (queryParams.isEmpty()) {
        println("  None")
    } else {
        queryParams.forEach { param ->
            println("  - ${param.name} (${param.required ?: false}): ${param.description ?: "No description"}")
            println("    Type: ${param.schema?.type ?: "Not specified"}")
        }
    }

    println("\nRequest Body:")
    val requestBody = operation.requestBody
    if (requestBody == null) {
        println("  None")
    } else {
        println("  Required: ${requestBody.required ?: false}")
        println("  Description: ${requestBody.description ?: "No description"}")

        requestBody.content?.forEach { (contentType, mediaType) ->
            println("  Content Type: $contentType")
            val schema = mediaType.schema
            if (schema != null) {
                println("  Schema Type: ${schema.type ?: "Not specified"}")
                println("  Schema Format: ${schema.format ?: "Not specified"}")

                if (schema.type == "object" && schema.properties != null) {
                    println("  Properties:")
                    schema.properties.forEach { (propName, propSchema) ->
                        println("    - $propName (${propSchema.type ?: "unknown type"}): ${propSchema.description ?: "No description"}")
                    }
                }
            }
        }
    }

    println("\n------")
}

fun main() {
    findMatchingEndpoint("https://api.spotify.com/v1/albums/4aawyAB9vmqN3uQ7FjRGTy/tracks", "GET")
}