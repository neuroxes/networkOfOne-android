package com.example.networkofone.mvvm.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserModel(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val userType: UserType = UserType.SCHOOL
) {
    // 3. Empty constructor for Firebase deserialization
    constructor() : this("", "", "", "", UserType.SCHOOL)
}
enum class UserType{
    SCHOOL,
    REFEREE,
    ADMIN
}
