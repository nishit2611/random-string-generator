package com.example.stringgen.data.model

data class RandomStringData(
    val value: String,
    val length: Int,
    val created: String,
    val id: Long = System.currentTimeMillis()
)

data class RandomStringResponse(
    val randomText: RandomStringData
)