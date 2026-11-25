package com.rukavina.gymbuddy.feedback

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Pings the owner's phone through ntfy.sh when new feedback arrives.
 * The push says only "New feedback": the user's text never leaves our
 * server (privacy policy). Off when the topic is blank, which is the
 * default and what tests run with. Fire-and-forget: a failed or slow
 * ping is logged and never affects the request.
 */
@Component
class FeedbackNotifier(
    @Value("\${gymbuddy.feedback.ntfy-topic:}") private val topic: String,
    @Value("\${gymbuddy.feedback.ntfy-url:https://ntfy.sh}") private val baseUrl: String,
) {
    private val log = LoggerFactory.getLogger(FeedbackNotifier::class.java)
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    fun newFeedback() {
        if (topic.isBlank()) return
        val request = HttpRequest.newBuilder(URI.create("${baseUrl.trimEnd('/')}/$topic"))
            .timeout(Duration.ofSeconds(10))
            .header("Title", "Gym Tracker")
            .header("Tags", "speech_balloon")
            .POST(HttpRequest.BodyPublishers.ofString("New feedback"))
            .build()
        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .whenComplete { response, error ->
                when {
                    error != null -> log.warn("Feedback ping failed: {}", error.toString())
                    response.statusCode() !in 200..299 -> log.warn("Feedback ping got HTTP {}", response.statusCode())
                }
            }
    }
}
