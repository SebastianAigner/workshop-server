package io.sebi

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

    val comments = mapOf<IssueId, List<Comment>>(
        IssueId(0) to listOf(
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