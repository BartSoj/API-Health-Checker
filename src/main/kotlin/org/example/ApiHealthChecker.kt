package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Class responsible for checking the health of API endpoints
 */
class ApiHealthChecker {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 3_000
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }
    }

    /**
     * Checks the health of an API endpoint
     *
     * @param url The URL of the API endpoint
     * @param method The HTTP method to use (GET, POST, PUT, DELETE)
     * @param params Query parameters as key-value pairs
     * @param body Optional JSON body string for POST/PUT requests
     * @param headers Optional HTTP headers
     * @return ApiHealthStatus containing health information
     */
    fun checkHealth(
        url: String,
        method: String,
        params: Map<String, String> = mapOf(),
        body: String? = null,
        headers: Map<String, String> = mapOf()
    ): ApiHealthStatus = runBlocking {
        val startTime = System.currentTimeMillis()

        try {
            val httpMethod = HttpMethod.parse(method.uppercase())
            val response = withTimeoutOrNull(5.seconds) {
                client.request {
                    this.method = httpMethod
                    this.url {
                        takeFrom(url)
                        params.forEach { (key, value) -> parameters.append(key, value) }
                    }
                    headers.forEach { (key, value) -> header(key, value) }
                    contentType(ContentType.Application.Json)

                    if (body != null) {
                        setBody(body)
                    }
                }
            }

            val endTime = System.currentTimeMillis()
            val responseTime = endTime - startTime

            if (response == null) {
                return@runBlocking ApiHealthStatus(
                    statusCode = -1,
                    responseTime = responseTime,
                    healthy = false,
                    errorMessage = "Request timed out after 5 seconds"
                )
            }

            return@runBlocking ApiHealthStatus(
                statusCode = response.status.value,
                responseTime = responseTime,
                healthy = response.status.isSuccess(),
                errorMessage = if (response.status.isSuccess()) null
                else "Unhealthy status code: ${response.status.value}"
            )
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            ApiHealthStatus(
                statusCode = -1,
                responseTime = endTime - startTime,
                healthy = false,
                errorMessage = "Request failed: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }

    /**
     * Closes the underlying HTTP client
     */
    fun close() {
        client.close()
    }
}