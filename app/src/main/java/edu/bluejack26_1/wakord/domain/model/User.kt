package edu.bluejack26_1.wakord.domain.model

data class User(
    val uid: String,
    val fullName: String,
    val email: String,
    val username: String = "",
    val photoUrl: String? = null,
    val bio: String = "",
    val instruments: List<Instrument> = emptyList(),
    val genres: List<String> = emptyList(),
)
