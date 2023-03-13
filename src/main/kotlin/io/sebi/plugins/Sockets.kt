package io.sebi.plugins

import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.sebi.createRandomCommentEvent
import io.sebi.issueTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Duration

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    routing {
        webSocket("/issueEvents") { // websocketSession
            launch {
                // TODO: Move this outside the individual client, otherwise everyone spams :)
                while (true) {
                    delay(1000)
                    val randomCommentEvent = createRandomCommentEvent()
                    issueTracker.addComment(randomCommentEvent.forIssue, randomCommentEvent.comment)
                }
            }
            issueTracker.issueEvents.onEach {
                sendSerialized(it)
            }.collect()
        }
    }
}
