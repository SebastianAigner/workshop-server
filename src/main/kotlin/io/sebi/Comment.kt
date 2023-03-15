package io.sebi

import kotlinx.serialization.Serializable

@Serializable
data class Comment(val author: String, val content: String)