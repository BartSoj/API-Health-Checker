# API Health Checker CLI Agent

A CLI agent for validating and testing API endpoints against OpenAPI specifications, written in Kotlin.

## 🔍 Overview

API Health Checker is a simulated AI agent that operates via command line to validate and test API endpoints. The agent:

1. Accepts specific text-based requests about API endpoint health
2. Validates requests against OpenAPI specifications
3. Selects appropriate tools (HTTP client or synthetic responder) to check endpoint health
4. Returns detailed health status in a conversational format

This agent is designed to process one specific type of request format and cannot handle general queries outside its
domain.

## ✨ Features

- **Specific Request Format**: Only processes "Determine the status of `<URL>` using `<METHOD>`" format requests
- **OpenAPI Validation**: Validates requests against standard OpenAPI specifications
- **Live Health Checking**: Tests API endpoint availability and response times
- **Synthetic Testing**: Option to use synthetic tools instead of real HTTP requests
- **Interactive CLI**: User-friendly command-line interface with animated agent responses
- **Detailed Error Reporting**: Comprehensive validation messages for debugging
- **Multiple API Support**: Pre-loaded specifications for many popular APIs

## 📋 Requirements

- Java 11 or higher
- Kotlin 1.8+
- Gradle 7.0+

## 🚀 Getting Started

### Running from JAR

Download the latest JAR from GitHub releases
https://github.com/BartSoj/API-Health-Checker/releases \
Run the agent:

```bash
java -jar api-health-checker.jar
```

### Usage Examples

The agent only processes requests in the following specific format:

```
Determine the status of <URL> using <METHOD> [with query parameters <params>] [with headers <headers>] [and body <body>]
```

#### Basic GET Request (Successful)

```
🔍 > Determine the status of https://httpbin.org/get using GET

🤖 > The API at https://httpbin.org/get is responding correctly with a 200 status code. The response time was 348ms, which is good.
```

#### GET Request with Parameters

```
🔍 > Determine the status of https://httpbin.org/get using GET with query parameters foo=bar, test=123

🤖 > The API at https://httpbin.org/get is responding correctly with a 200 status code. The response time was 412ms, which is good.
```

#### Status Code Error Example

```
🔍 > Determine the status of https://httpbin.org/status/404 using GET

🤖 > I was able to reach https://httpbin.org/status/404, but the API is experiencing issues. It returned a 404 status code in 356ms. The specific error is: Unhealthy status code: 404.
```

#### Invalid Request Format

```
🔍 > Check if https://httpbin.org/get is working

🤖 > Invalid request format. Expected: "Determine the status of <URL> using <METHOD> [with query parameters <params>] [with headers <headers>] [and body <body>]".
```

#### Invalid Endpoint

```
🔍 > Determine the status of https://httpbin.org/invalid-endpoint using GET

🤖 > I couldn't find an API endpoint for GET /invalid-endpoint on httpbin.org. Please check that the URL is correct and the API is supported by our OpenAPI specifications.
```

#### POST Request with unexpected Body

```
🔍 > Determine the status of https://httpbin.org/post using POST and body {"name": "John Doe", "email": "john@example.com"}

🤖 > I noticed some issues with your request to /post using POST: There's an issue with your request body - it wasn't expected for this endpoint. 
```

#### PUT Request with Headers and unexpected Body

```
🔍 > Determine the status of https://httpbin.org/put using PUT with headers Content-Type=application/json, X-Custom-Header=test and body {"updated": true}

🤖 > I noticed some issues with your request to /put using PUT: There's an issue with your request body - it wasn't expected for this endpoint.
```

### Spotify API Validation Examples

The following examples demonstrate validation against the Spotify API OpenAPI specification:

#### Missing Required Query Parameter

```
🔍 > Determine the status of https://api.spotify.com/v1/search using GET

🤖 > I noticed some issues with your request to /v1/search using GET: You need to provide the following required parameters: q, type.
```

#### Search with Missing Type Parameter

```
🔍 > Determine the status of https://api.spotify.com/v1/search using GET with query parameters q=queen

🤖 > I noticed some issues with your request to /v1/search using GET: You need to provide the following required parameter: type.
```

#### Missing Authorization Header

```
🔍 > Determine the status of https://api.spotify.com/v1/me/playlists using GET

🤖 > I was able to reach https://api.spotify.com/v1/me/playlists, but the API is experiencing issues. It returned a 401 status code in 534ms. The specific error is: Unhealthy status code: 401.
```

#### Missing Required Body for POST Request

```
🔍 > Determine the status of https://api.spotify.com/v1/users/USER_ID/playlists using POST

🤖 > I noticed some issues with your request to /v1/users/{user_id}/playlists using POST: This endpoint requires a request body, but none was provided.
```

#### Invalid Body Structure

```
🔍 > Determine the status of https://api.spotify.com/v1/users/USER_ID/playlists using POST with body {"invalid": "structure"}

🤖 > I noticed some issues with your request to /v1/users/{user_id}/playlists using POST: Your request body is missing required fields: name.
```

## 🔧 How It Works

1. **Input Processing**: The agent parses the specific text input format to extract URL, method, and optional parameters
2. **Tool Selection**: The agent selects either the HTTP Request Tool or Synthetic Response Tool for processing
3. **Request Validation**: Validates the request against OpenAPI specifications for correctness
4. **Request Execution**: The selected tool processes the request and gathers health data
5. **Response Formatting**: Returns detailed health status information in a conversational format

If the input doesn't match the expected format or validation fails, the agent provides specific error messages.

## 📚 Supported API Specifications

The agent includes OpenAPI specifications for:

- Google APIs (Calendar, Drive, Gmail, People, Sheets, Slides, Tasks)
- Discord API
- Spotify API
- Wolfram Alpha API
- HTTP Bin (for testing)

Custom OpenAPI specifications can be added to the `api_specs` directory.

## 🧪 Development

### Building and Testing

```bash
# Build the project
./gradlew build

# Run all tests
./gradlew test

# Run a specific test
./gradlew test --tests "org.example.ApiValidatorTest.testMethodName"

# Run the agent
./gradlew run
```