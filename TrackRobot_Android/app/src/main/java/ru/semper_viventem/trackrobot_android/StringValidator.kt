package ru.semper_viventem.trackrobot_android

import android.util.Patterns

object StringValidator {

    private val IP_REGEX = Patterns.IP_ADDRESS

    fun isValidIp(candidate: String): Boolean = IP_REGEX.matcher(candidate).matches()
}