package com.dkchoi.wetalk.data

data class MessageData(
    val type: MessageType,
    val name: String,
    val content: String,
    val sendTime: Long,
    var roomName: String
)
