package com.bilirkisi.proje.models


data class User(
    val uid: String? = null,
    val username: String? = null,
    val email: String? = null,
    val expertUser: Boolean? = null,   //***
    val token: String? = null,
    val profile_picture_url: String? = null,
    var sentRequests: List<String>? = null,
    var receivedRequests: List<String>? = null,
    var friends: List<String>? = null,
    var bio: String? = null
)