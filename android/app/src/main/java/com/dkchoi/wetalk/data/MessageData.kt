package com.dkchoi.wetalk.data

import java.util.*

data class MessageData(
    val type: MessageType,
    val name: String,
    val id: String,
    val content: String,
    val sendTime: Long,
    val roomName: String,
    val roomTitle: String,
    val roomId: String
)
