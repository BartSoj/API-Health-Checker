1. Technology & Environment

    - Language: Kotlin.
    - Interface: Command-line (CLI) application.
    - Runtime: Continuous loop accepting input until the user types a termination command (e.g., “exit”).

2. Input Format

    - Accepted Request Pattern:
      `Determine the status of <URL> with query parameters <query> with headers <headers> and body <body>`

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

# Example Agent queries

### Correctly Structured Requests

```
"Determine the status of https://api.spotify.com/v1/tracks/3n3Ppam7vgaVa1iaRUc9Lp with query parameters market=US"

"Determine the status of https://api.spotify.com/v1/playlists/3cEYpjA9oz9GiPac4AsH4n with headers Authorization=Bearer abc123 and body { \"name\": \"My Playlist\" }"

"Determine the status of https://api.spotify.com/v1/recommendations with query parameters seed_artists=4NHQUGzhtTLFvgF5SZesLK,seed_genres=pop,seed_tracks=0c6xIDDpzE81m2q797ordA"

"Determine the status of https://tasks.googleapis.com/tasks/v1/users/@me/lists"

"Determine the status of https://tasks.googleapis.com/tasks/v1/users/@me/lists/taskList123"

"Determine the status of https://tasks.googleapis.com/tasks/v1/users/@me/lists with headers Authorization=Bearer abc123 and body { \"title\": \"New Task List\" }"

"Determine the status of https://tasks.googleapis.com/tasks/v1/lists/taskList123/tasks with query parameters showCompleted=true, maxResults=10"

"Determine the status of https://tasks.googleapis.com/tasks/v1/lists/taskList123/tasks with headers Authorization=Bearer abc123 and body { \"title\": \"New Task\", \"notes\": \"Complete this by Monday.\" }"

"Determine the status of https://httpbin.org/status/418"

"Determine the status of https://httpbin.org/status/200"

"Determine the status of https://httpbin.org/status/500"

"Determine the status of https://httpbin.org/delay/10"

"Determine the status of https://httpbin.org/status/404"
```

### Incorrectly Structured Requests

```
"Determine the status of with query parameters market=US"

"Determine the status of https://api.spotify.com/v1/me/player/play"

"Determine the status of https://api.spotify.com/v1/tracks with body instead of query parameters"

"Determine the status of https://api.spotify.com/v1/me with query parameters market=US"

"Determine the status of with query parameters maxResults=10"

"Determine the status of https://tasks.googleapis.com/tasks/v1/lists/taskList123/tasks/task123 with body { \"status\": \"completed\" }"

"Determine the status of https://tasks.googleapis.com/tasks/v1/lists/taskList123/tasks?showCompleted-true"

"Determine the status of https://tasks.googleapis.com/tasks/v1/lists//tasks/task123"

"Determine the status of https://httpbin.org/post"

"Determine the status of https://httpbin.org/invalidendpoint"
```