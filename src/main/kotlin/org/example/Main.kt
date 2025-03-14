package org.example

/**
 * Main entry point for the API Health Checker application.
 * Creates and starts the CLI agent.
 */
fun main() {
    val apiHealthChecker = ApiHealthChecker()
    val cliAgent = CliAgent(apiHealthChecker)
    cliAgent.start()
}