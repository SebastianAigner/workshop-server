package io.sebi.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sebi.IssueId
import io.sebi.issueTracker
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        route("issue") {
            // Endpoint to simulate that parallel requests are faster than sequential
            get("{id}/comments") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val intId = id.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                delay(500)
                call.respond(issueTracker.commentsForId(IssueId(intId)))
            }
        }
        // Endpoint to simulate single slow request
        route("issues") {
            get {
                delay(2.seconds)
                call.respond(issueTracker.allIssues())
            }
        }
    }
}
