package org.example

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.anim.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.shutdown.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.render.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the current state of the CLI
 */
enum class State {
    USER_TYPING, AGENT_THINKING, AGENT_RESPONDED
}

/**
 * Represents a message in the conversation history
 */
data class Message(
    val role: String, val content: String
)

/**
 * Main entry point for the API Health Checker application.
 * Provides a CLI interface for checking API health using the ApiHealthCheckerAgent.
 */
fun main() = session {
    val agent = CliAgent()

    try {
        var state by liveVarOf(State.USER_TYPING)
        var userText by liveVarOf("")
        var agentResponse by liveVarOf("")
        val thinkingAnim = textAnimOf(listOf("⠋", "⠙", "⠸", "⠴", "⠦", "⠇"), 150.milliseconds)
        val history = mutableListOf<Message>()

        val userPrompt = "🔍 > "
        val agentPrompt = "🤖 > "

        fun RenderScope.userColor(block: RenderScope.() -> Unit) {
            white(scopedBlock = block)
        }

        fun RenderScope.agentColor(block: RenderScope.() -> Unit) {
            cyan(scopedBlock = block)
        }

        fun RenderScope.infoColor(block: RenderScope.() -> Unit) {
            black(isBright = true, scopedBlock = block)
        }

        section {
            textLine()
            infoColor {
                textLine("API Health Checker - Type a request or 'exit' to quit")
                textLine("Example: \"Determine the status of https://jsonplaceholder.typicode.com/posts\" using GET")
                textLine()
            }

            // Display conversation history
            history.forEach { message ->
                when (message.role) {
                    "user" -> userColor { text(userPrompt); textLine(message.content) }
                    "agent" -> agentColor { text(agentPrompt); textLine(message.content) }
                }
                textLine()
            }

            // Display current state
            when (state) {
                State.USER_TYPING -> {
                    userColor { text(userPrompt); input() }
                }

                State.AGENT_THINKING -> {
                    userColor { text(userPrompt); textLine(userText) }
                    textLine()
                    agentColor { text(agentPrompt); text(thinkingAnim) }
                }

                State.AGENT_RESPONDED -> {
                    userColor { text(userPrompt); textLine(userText) }
                    textLine()
                    agentColor { text(agentPrompt); textLine(agentResponse) }
                    textLine()
                    userColor { text(userPrompt); input() }
                }
            }
        }.run {
            var shouldQuit = false
            addShutdownHook { shouldQuit = true }

            onInputEntered {
                if (input.trim().equals("exit", ignoreCase = true)) {
                    shouldQuit = true
                    return@onInputEntered
                }

                userText = input
                state = State.AGENT_THINKING

                // Process the request in a coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    // Call ApiHealthCheckerAgent to process request
                    agentResponse = agent.processRequest(userText)

                    // Small delay to simulate agent thinking
                    delay(500)

                    // Add to conversation history
                    history.add(Message("user", userText))
                    history.add(Message("agent", agentResponse))

                    // Reset for next interaction
                    userText = ""
                    agentResponse = ""

                    // Move to next state
                    state = State.USER_TYPING

                    // Clear the input field
                    setInput("")
                }
            }

            var lastState = state
            while (!shouldQuit) {
                if (lastState != state) {
                    lastState = state
                }
                delay(Anim.ONE_FRAME_60FPS)
            }
        }
    } finally {
        agent.close()
    }
}