package com.anotherpatkreation.qrwheel

data class Entry(
    val id: Long = 0,
    val name: String,
    val email: String = "",
    val sells: String = "",
    val won: Boolean = false,
    val opt1: String = "",
    val opt2: String = "",
    val opt3: String = "",
    val opt4: String = ""
)
