package com.aqi.weather.auth

data class UserModel(
    var userType: String? = null,
    var id: String? = null,
    var name: String? = null,
    var email: String? = null,
    var pass: String? = null,
    var gender: String? = null,
    var dob: String? = null,
    var phone: String? = null
)