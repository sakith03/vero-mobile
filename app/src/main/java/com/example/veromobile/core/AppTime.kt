package com.example.veromobile.core

import java.time.LocalDate
import java.time.ZoneId

object AppTime {
    val zone: ZoneId = ZoneId.of("Asia/Colombo")
    fun today(): LocalDate = LocalDate.now(zone)
}