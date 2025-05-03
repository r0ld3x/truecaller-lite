package com.r0ld3x.truecaller

data class ResponseTypes (
    val address: String,
    val birthday: String,
    val gender: String,
    val image: String,
    val name: String,
    val number: String
)
//data class ResponseTypes (
//    val data: List<Datum>,
//    val provider: String,
//    val stats: Stats
//)

data class Datum (
    val id: String,
    val name: String,
    val imID: String,
    val gender: String,
    val score: Double,
    val access: String,
    val enhanced: Boolean,
    val phones: List<Phone>,
    val addresses: List<Address>,
    val internetAddresses: List<InternetAddress>,
    val badges: List<String>,
    val tags: List<Any?>,
    val cacheTTL: Long,
    val sources: List<Any?>,
    val searchWarnings: List<SearchWarning>,
    val surveys: List<Survey>,
    val commentsStats: CommentsStats,
    val manualCallerIDPrompt: Boolean,
    val ns: Long
)

data class Address (
    val address: String,
    val city: String,
    val countryCode: String,
    val timeZone: String,
    val type: String
)

data class CommentsStats (
    val showComments: Boolean
)

data class InternetAddress (
    val id: String,
    val service: String,
    val caption: String,
    val type: String
)

data class Phone (
    val e164Format: String,
    val numberType: String,
    val nationalFormat: String,
    val dialingCode: Long,
    val countryCode: String,
    val carrier: String,
    val type: String
)

data class SearchWarning (
    val id: String,
    val ruleName: String,
    val features: List<Any?>,
    val ruleID: String
)

data class Survey (
    val id: String,
    val frequency: Long,
    val passthroughData: String,
    val perNumberCooldown: Long,
    val dynamicContentAccessKey: String
)

data class Stats (
    val sourceStats: List<Any?>
)
