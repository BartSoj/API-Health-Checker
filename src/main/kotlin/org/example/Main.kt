package org.example

/**
 * Main entry point for the API Health Checker application.
 * Creates and starts the CLI agent.
 */
fun main() {
    val requestParser = RequestParser()
    val apiValidator = ApiValidator()
    val httpRequestTester = HttpRequestTester()
    val syntheticTool = SyntheticTool()

    val apiHealthChecker = ApiHealthChecker(
        apiValidator, httpRequestTester, syntheticTool, requestParser
    )

    val cliAgent = CliAgent(apiHealthChecker)
    cliAgent.start()
}