package com.aqi.weather.data.remote.dto

data class LocationResponse(
    val city: String,
    val continent: String,
    val continentCode: String,
    val countryCode: String,
    val countryName: String,
    val latitude: Double,
    val locality: String,
    val localityInfo: LocalityInfo,
    val localityLanguageRequested: String,
    val longitude: Double,
    val plusCode: String,
    val postcode: String,
    val principalSubdivision: String,
    val principalSubdivisionCode: String
)

data class LocalityInfo(
    val administrative: List<Administrative>,
    val informative: List<Informative>
)

data class Administrative(
    val adminLevel: Int,
    val description: String,
    val geonameId: Int,
    val isoCode: String,
    val isoName: String,
    val name: String,
    val order: Int,
    val wikidataId: String
)

data class Informative(
    val description: String,
    val geonameId: Int,
    val isoCode: String,
    val isoName: String,
    val name: String,
    val order: Int,
    val wikidataId: String
)