1. Technology & Environment

    - Language: Kotlin.
    - Interface: Command-line (CLI) application.
    - Runtime: Continuous loop accepting input until the user types a termination command (e.g., “exit”).

2. Input Format

    - Accepted Request Pattern:
      `Determine the HTTP status of <URL> with query parameters <query> with headers <headers> and body <body>`

    - Mandatory:
    - URL: A valid HTTP/HTTPS URL.
    - Optional:
    - Query Parameters: Key-value pairs appended to the URL (e.g., ?key1=value1&key2=value2).
    - Headers: Provided as one or more key: value pairs.
    - Body: A JSON-formatted string (if applicable).

    - Validation:
    - The agent must check that the input strictly matches this format.
    - If not, it must respond with an error message indicating the correct format.

3. OpenAPI Specification Validation

    - Specification File:
    - The agent must load a predefined OpenAPI JSON file at startup.
    - Validation Process:
    - URL Path: The provided URL’s path must exist in the OpenAPI spec.
    - HTTP Method: The request must use an allowed method (e.g., HEAD, GET, or POST) as defined in the spec.
    - Parameters & Headers:
    - All required query parameters and headers defined for the endpoint must be present.
    - The values must conform to the expected format or type.
    - Request Body:
    - If a body is provided, it must match the schema defined in the OpenAPI spec.
    - Invalid Request Handling:
    - If the input does not match the specification, output:

   Invalid request: The endpoint or request parameters do not match our OpenAPI specification.

4. HTTP Request Execution

    - Tool Options (Internal Tools):
      The agent may have multiple internal tools for handling valid requests:
    - Real HTTP Request Tool:
    - Construct the HTTP request using the parsed URL, query parameters, headers, and body.
    - Set appropriate timeouts (e.g., connection and read timeouts of 5000ms).
    - Use an HTTP client (e.g., HttpURLConnection in Java) to send the request.
    - Capture the HTTP response status code.
    - Synthetic Response Tool (Optional):
    - Generate a synthetic status code or response for demonstration purposes.
    - Tool selection can be random or based on specific conditions.
    - Decision Logic:
    - The agent must decide which tool to use while keeping the input request type fixed.

5. Output Format

    - Success Response:
      If the request is valid and executed:

   The HTTP status of <URL> is <status code>.

    - Error Response:
    - For validation errors (not matching OpenAPI spec):

   Invalid request: The endpoint or request parameters do not match our OpenAPI specification.

    - For network or execution errors:

   Error: Unable to reach the specified URL.

6. User Interaction & Error Handling

    - Continuous Input:
    - The agent should repeatedly prompt the user for input.
    - Graceful Error Handling:
    - Provide clear error messages if the input format is incorrect or if the request fails.
    - Termination:
    - Allow the user to exit the application with a specific command (e.g., “exit”).

7. Code Quality & Structure

    - Modularity:
    - Separate components for input processing, OpenAPI specification validation, HTTP request execution, and output
      generation.
    - Documentation:
    - Include clear inline comments and documentation for each module.