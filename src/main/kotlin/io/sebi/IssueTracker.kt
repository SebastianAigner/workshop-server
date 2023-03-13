package io.sebi

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable

val issueTracker = IssueTracker()

@Serializable
data class Comment(val author: String, val content: String)

@Serializable
enum class IssueStatus {
    OPEN,
    FIXED,
    WONTFIX
}

@Serializable
data class Issue(val id: IssueId, val title: String, val status: IssueStatus)

@JvmInline
@Serializable
value class IssueId(val id: Int)

class IssueTracker {
    val comments = mutableMapOf<IssueId, MutableList<Comment>>(
        IssueId(0) to mutableListOf(
            Comment("sebi_io", "Then what am I looking at? 🤨")
        )
    )

    fun allIssues(): List<Issue> {
        return listOf(
            Issue(
                IssueId(0),
                "Implement issue tracker",
                IssueStatus.WONTFIX,
            )
        )
    }

    fun commentsForId(issueId: IssueId): List<Comment> {
        return comments[issueId] ?: listOf()
    }


    // For now, this just provides an "ad hoc" view of what's going on without any guarantees about completeness of the log you receive
    private val _issueEvents = MutableSharedFlow<IssueEvent>(onBufferOverflow = BufferOverflow.DROP_OLDEST, replay = 1)
    val issueEvents = _issueEvents.asSharedFlow()

    // this function seems very much not thread safe
    fun addComment(issueId: IssueId, comment: Comment) {
        val issue = allIssues().firstOrNull { it.id == issueId } ?: return
        val allComments = comments.getOrPut(issue.id) { mutableListOf() }
        allComments += comment
        comments[issueId] = allComments
        _issueEvents.tryEmit(IssueEvent(IssueEventType.CREATE, issueId, comment))
        println("Added $comment")
    }
}

@Serializable
enum class IssueEventType {
    CREATE
}

@Serializable
class IssueEvent(val type: IssueEventType, val forIssue: IssueId, val comment: Comment)

fun createRandomCommentEvent(): IssueEvent {
    val author = listOf("seb", "marie", "ysl", "fred").random()
    val text = listOf("Wow!", "Great!", "+1", "Why on earth would anyone want this?").random()
    return IssueEvent(IssueEventType.CREATE, issueTracker.allIssues().random().id, Comment(author, text))
}