package org.example

/**
 * Class for parsing user input into a structured ParsedRequest
 */
object RequestParser {
    // Regular expression to match the expected request format
    private val REQUEST_PATTERN = Regex(
        "Determine the status of (\\S+) using (GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD)" +
                "(?:\\s+with\\s+query\\s+parameters\\s+(.+?))?" +
                "(?:\\s+with\\s+headers\\s+(.+?))?" +
                "(?:\\s+and\\s+body\\s+(.+))?",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses a user input string into a structured ParsedRequest
     *
     * @param input The user input string
     * @return A ParsedRequest if parsing was successful, null otherwise
     */
    fun parse(input: String): ParsedRequest? {
        val matchResult = REQUEST_PATTERN.matchEntire(input.trim()) ?: return null

        val url = matchResult.groupValues[1]
        val method = matchResult.groupValues[2].uppercase()
        val queryParamsString = matchResult.groupValues[3]
        val headersString = matchResult.groupValues[4]
        val bodyString = matchResult.groupValues[5]

        // Parse query parameters
        val queryParams = parseKeyValuePairs(queryParamsString)

        // Parse headers
        val headers = parseKeyValuePairs(headersString)

        // Parse body (if provided)
        val body = if (bodyString.isNotBlank()) bodyString.trim() else null

        return ParsedRequest(
            url = url,
            method = method,
            queryParams = queryParams,
            headers = headers,
            body = body
        )
    }

    /**
     * Parses a string of key-value pairs into a map
     *
     * @param input The string to parse
     * @return A map of key-value pairs
     */
    private fun parseKeyValuePairs(input: String): Map<String, String> {
        if (input.isBlank()) return emptyMap()

        return input.split(",\\s*".toRegex())
            .map { it.trim() }
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    val colonParts = pair.split(":", limit = 2)
                    if (colonParts.size == 2) {
                        colonParts[0].trim() to colonParts[1].trim()
                    } else null
                }
            }.toMap()
    }
}