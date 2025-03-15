package org.example

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import io.swagger.v3.oas.models.Operation

class ApiHealthCheckerTest {
    private lateinit var apiValidator: ApiValidator
    private lateinit var httpRequestTester: HttpRequestTester
    private lateinit var syntheticTool: SyntheticTool
    private lateinit var requestParser: RequestParser
    private lateinit var apiHealthChecker: ApiHealthChecker

    @BeforeEach
    fun setUp() {
        apiValidator = mock()
        httpRequestTester = mock()
        syntheticTool = mock()
        requestParser = mock()

        apiHealthChecker = ApiHealthChecker(
            apiValidator, httpRequestTester, syntheticTool, requestParser
        )
    }

    @Test
    fun `processRequest should return error message for invalid input`() {
        val request = "Invalid request"
        whenever(requestParser.parse(request)).thenReturn(null)

        val result = apiHealthChecker.processRequest(request)

        assertTrue(result.contains("Invalid request format"))
        verify(requestParser).parse(request)
        verifyNoInteractions(apiValidator)
        verifyNoInteractions(httpRequestTester)
        verifyNoInteractions(syntheticTool)
    }

    @Test
    fun `processRequest should handle endpoint not found`() {
        val request = "Determine the status of https://api.example.com/endpoint using GET"
        val parsedRequest = ParsedRequest(url = "https://api.example.com/endpoint", method = "GET")

        whenever(requestParser.parse(request)).thenReturn(parsedRequest)
        whenever(apiValidator.findMatchingEndpoint(parsedRequest.url, parsedRequest.method)).thenReturn(null)

        val result = apiHealthChecker.processRequest(request)

        assertTrue(result.contains("I couldn't find an API endpoint"))
        verify(requestParser).parse(request)
        verify(apiValidator).findMatchingEndpoint(parsedRequest.url, parsedRequest.method)
        verifyNoInteractions(httpRequestTester)
        verifyNoInteractions(syntheticTool)
    }

    @Test
    fun `processRequest should validate request against OpenAPI spec`() {
        val request = "Determine the status of https://api.example.com/endpoint using GET"
        val parsedRequest = ParsedRequest(
            url = "https://api.example.com/endpoint", method = "GET", queryParams = mapOf("param" to "value")
        )
        val operation = Operation()
        val endpointMatch = EndpointMatch(
            url = parsedRequest.url, method = parsedRequest.method, pathPattern = "/endpoint", operation = operation
        )
        val validationResult = ValidationResult(
            isValid = false, errors = listOf(
                ValidationError(
                    ValidationErrorType.MISSING_REQUIRED_PARAMETER, "requiredParam"
                )
            )
        )

        whenever(requestParser.parse(request)).thenReturn(parsedRequest)
        whenever(apiValidator.findMatchingEndpoint(parsedRequest.url, parsedRequest.method)).thenReturn(endpointMatch)
        whenever(apiValidator.validateRequest(endpointMatch, parsedRequest.queryParams, parsedRequest.body)).thenReturn(
            validationResult
        )

        val result = apiHealthChecker.processRequest(request)

        assertTrue(result.contains("I noticed some issues"))
        verify(requestParser).parse(request)
        verify(apiValidator).findMatchingEndpoint(parsedRequest.url, parsedRequest.method)
        verify(apiValidator).validateRequest(endpointMatch, parsedRequest.queryParams, parsedRequest.body)
        verifyNoInteractions(httpRequestTester)
        verifyNoInteractions(syntheticTool)
    }

    @Test
    fun `processRequest should use HttpRequestTester for valid requests`() {
        val request = "Determine the status of https://api.example.com/endpoint using GET"
        val parsedRequest = ParsedRequest(
            url = "https://api.example.com/endpoint", method = "GET"
        )
        val operation = Operation()
        val endpointMatch = EndpointMatch(
            url = parsedRequest.url, method = parsedRequest.method, pathPattern = "/endpoint", operation = operation
        )
        val validationResult = ValidationResult(isValid = true)
        val healthStatus = ApiHealthStatus(
            statusCode = 200, responseTime = 150, healthy = true, errorMessage = null
        )

        whenever(requestParser.parse(request)).thenReturn(parsedRequest)
        whenever(apiValidator.findMatchingEndpoint(parsedRequest.url, parsedRequest.method)).thenReturn(endpointMatch)
        whenever(apiValidator.validateRequest(endpointMatch, parsedRequest.queryParams, parsedRequest.body)).thenReturn(
            validationResult
        )
        whenever(
            httpRequestTester.checkHealth(
                url = parsedRequest.url,
                method = parsedRequest.method,
                params = parsedRequest.queryParams,
                body = parsedRequest.body,
                headers = parsedRequest.headers
            )
        ).thenReturn(healthStatus)

        val result = apiHealthChecker.processRequest(request)

        assertTrue(result.contains("The API at"))
        assertTrue(result.contains("200"))
        assertTrue(result.contains("responding correctly"))
        verify(requestParser).parse(request)
        verify(apiValidator).findMatchingEndpoint(parsedRequest.url, parsedRequest.method)
        verify(apiValidator).validateRequest(endpointMatch, parsedRequest.queryParams, parsedRequest.body)
        verify(httpRequestTester).checkHealth(
            url = parsedRequest.url,
            method = parsedRequest.method,
            params = parsedRequest.queryParams,
            body = parsedRequest.body,
            headers = parsedRequest.headers
        )
        verifyNoInteractions(syntheticTool)
    }
}