package io.sebi

import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
enum class IssueStatus {
    OPEN,
    FIXED,
    WONTFIX
}

@Serializable
data class Issue(val id: IssueId, val author: String, val title: String, val status: IssueStatus)

@JvmInline
@Serializable
value class IssueId(val id: Int)

fun Application.setupIssueTracker(): IssueTracker {
    val issueTracker = IssueTracker()
    val vUser = VirtualUser()
    vUser.beginPosting(this, issueTracker) // TODO: is there a nicer syntax for this? :)
    launch {
        issueTracker.issueEvents.onEach {
            println(it)
        }.collect()
    }
    return issueTracker
}

class VirtualUser {
    fun createRandomCommentEvent(issueTracker: IssueTracker): AddCommentToIssueEvent {
        val author = listOf("seb", "marie", "ysl", "fred").random()
        val text = listOf("Wow!", "Great!", "+1", "Why on earth would anyone want this?").random()
        return AddCommentToIssueEvent(issueTracker.allIssues().random().id, Comment(author, text))
    }

    fun beginPosting(coroutineScope: CoroutineScope, issueTracker: IssueTracker) {
        coroutineScope.launch {
            while (true) {
                delay(1000)
                val randomCommentEvent = createRandomCommentEvent(issueTracker)
                issueTracker.addComment(randomCommentEvent.forIssue, randomCommentEvent.comment)
            }
        }
    }
}


class IssueTracker {
    private val comments = mutableMapOf<IssueId, MutableList<Comment>>(
        IssueId(0) to mutableListOf(
            Comment("sebi_io", "Then what am I looking at? 🤨")
        )
    )

    private val issues = mutableListOf<Issue>(
        Issue(
            IssueId(0),
            "seb",
            "Implement issue tracker",
            IssueStatus.WONTFIX,
        )
    )

    fun allIssues(): List<Issue> = issues

    fun commentsForId(issueId: IssueId): List<Comment> {
        return comments[issueId] ?: listOf()
    }


    // For now, this just provides an "ad hoc" view of what's going on without any guarantees about completeness of the log you receive
    private val _issueEvents =
        MutableSharedFlow<IssueEvent>(onBufferOverflow = BufferOverflow.DROP_OLDEST, replay = 1)
    val issueEvents = _issueEvents.asSharedFlow()

    // this function seems very much not thread safe
    fun addComment(issueId: IssueId, comment: Comment) {
        val issue = allIssues().firstOrNull { it.id == issueId } ?: return
        val allComments = comments.getOrPut(issue.id) { mutableListOf() }
        allComments += comment
        comments[issueId] = allComments
        _issueEvents.tryEmit(AddCommentToIssueEvent(issueId, comment))
        println("Added $comment")
    }

    fun issueForId(id: IssueId): Issue? {
        return issues.find { it.id == id }
    }

    fun addIssue(author: String, title: String): Issue {
        // TODO: This is very much not thread-safe :)
        val newId = IssueId(allIssues().maxOf { it.id.id } + 1)
        val newIssue = Issue(id = newId, author = author, title = title, status = IssueStatus.OPEN)
        issues.add(newIssue)
        _issueEvents.tryEmit(CreateIssueEvent(newIssue))
        return newIssue
    }
}

@Serializable
sealed class IssueEvent

@Serializable
class AddCommentToIssueEvent(val forIssue: IssueId, val comment: Comment) : IssueEvent()

@Serializable
class CreateIssueEvent(val issue: Issue) : IssueEvent()