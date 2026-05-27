package com.example.util

import java.util.Calendar
import kotlin.math.*

object PrayerTimeCalculator {

    data class CityPreset(val name: String, val latitude: Double, val longitude: Double, val defaultMethod: Int)

    val PRESET_CITIES = listOf(
        CityPreset("Makkah (Mecca)", 21.4225, 39.8262, 3), // Umm Al-Qura
        CityPreset("Cairo", 30.0444, 31.2357, 2),        // Egypt Survey
        CityPreset("London", 51.5074, -0.1278, 0),       // MWL
        CityPreset("New York City", 40.7128, -74.0060, 1),// ISNA
        CityPreset("Jakarta", -6.2088, 106.8456, 1),     // ISNA
        CityPreset("Karachi", 24.8607, 67.0011, 4),      // Karachi
        CityPreset("Tehran", 35.6892, 51.3890, 5),       // Tehran
        CityPreset("Kuala Lumpur", 3.1390, 101.6869, 1), // ISNA
        CityPreset("Sydney", -33.8688, 151.2093, 0)      // MWL
    )

    data class MethodConfig(
        val name: String,
        val fajrAngle: Double,
        val ishaUsesAngle: Boolean,
        val ishaAngleOrInterval: Double // angle (deg) or interval (min)
    )

    val CALCULATION_METHODS = listOf(
        MethodConfig("Muslim World League (MWL)", 18.0, true, 17.0),
        MethodConfig("Islamic Society of North America (ISNA)", 15.0, true, 15.0),
        MethodConfig("Egyptian General Authority of Survey", 19.5, true, 17.5),
        MethodConfig("Umm Al-Qura, Makkah", 18.5, false, 90.0), // 90 min after Maghrib
        MethodConfig("University of Islamic Sciences, Karachi", 18.0, true, 18.0),
        MethodConfig("Institute of Geophysics, University of Tehran", 17.7, true, 14.0)
    )

    fun calculatePrayerTimes(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        timezoneOffsetHours: Double,
        methodIndex: Int,
        asrMethod: Int // 1 = Standard (Shafi'i/etc), 2 = Hanafi
    ): Map<String, Calendar> {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val d = getDayOfYear(year, month, day)
        val method = CALCULATION_METHODS.getOrElse(methodIndex) { CALCULATION_METHODS[3] }

        // B parameter for Equation of Time
        val b = 2.0 * Math.PI * (d - 81) / 365.0
        val eot = 9.87 * sin(2.0 * b) - 7.53 * cos(b) - 1.5 * sin(b) // equation of time in minutes

        val declinationDegrees = 23.45 * sin(2.0 * Math.PI * (284 + d) / 365.0)
        val declRad = Math.toRadians(declinationDegrees)
        val latRad = Math.toRadians(latitude)

        // Base Solar Noon in fractional local hours
        val solarNoonFractional = 12.0 + timezoneOffsetHours - (longitude / 15.0) - (eot / 60.0)

        // Helper function for Hour Angle (H)
        fun hourAngle(altitudeDegrees: Double, isBelowHorizon: Boolean): Double {
            val altitudeRad = if (isBelowHorizon) {
                Math.toRadians(-altitudeDegrees)
            } else {
                Math.toRadians(altitudeDegrees)
            }
            val cosH = (sin(altitudeRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
            return when {
                cosH < -1.0 -> 180.0
                cosH > 1.0 -> 0.0
                else -> Math.toDegrees(acos(cosH))
            }
        }

        // Sunrise and Sunset (altitude = 0.833)
        val sunsetHourAngle = hourAngle(0.833, isBelowHorizon = true)
        val sunriseFractional = solarNoonFractional - (sunsetHourAngle / 15.0)
        val sunsetFractional = solarNoonFractional + (sunsetHourAngle / 15.0)

        // Fajr
        val fajrHourAngle = hourAngle(method.fajrAngle, isBelowHorizon = true)
        val fajrFractional = solarNoonFractional - (fajrHourAngle / 15.0)

        // Asr
        val shadowRatio = if (asrMethod == 2) 2.0 else 1.0
        val latMinusDeclRad = abs(latRad - declRad)
        val cotAngle = shadowRatio + tan(latMinusDeclRad)
        val asrAltitudeDegrees = Math.toDegrees(atan(1.0 / cotAngle))
        
        val asrHourAngle = hourAngle(asrAltitudeDegrees, isBelowHorizon = false)
        val asrFractional = solarNoonFractional + (asrHourAngle / 15.0)

        // Maghrib
        val maghribFractional = sunsetFractional

        // Isha
        val ishaFractional = if (method.ishaUsesAngle) {
            val ishaHourAngle = hourAngle(method.ishaAngleOrInterval, isBelowHorizon = true)
            solarNoonFractional + (ishaHourAngle / 15.0)
        } else {
            // Umm Al-Qura / standard intervals (minutes added to maghrib)
            maghribFractional + (method.ishaAngleOrInterval / 60.0)
        }

        fun fractionalToCalendar(fractionalHours: Double): Calendar {
            val resCal = calendar.clone() as Calendar
            
            var hours = fractionalHours
            var dayAdjustment = 0
            while (hours < 0) {
                hours += 24.0
                dayAdjustment -= 1
            }
            while (hours >= 24) {
                hours -= 24.0
                dayAdjustment += 1
            }

            val totalSeconds = (hours * 3600.0).roundToInt()
            val finalHours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            resCal.add(Calendar.DAY_OF_YEAR, dayAdjustment)
            resCal.set(Calendar.HOUR_OF_DAY, finalHours)
            resCal.set(Calendar.MINUTE, minutes)
            resCal.set(Calendar.SECOND, seconds)
            resCal.set(Calendar.MILLISECOND, 0)
            return resCal
        }

        return mapOf(
            "fajr" to fractionalToCalendar(fajrFractional),
            "sunrise" to fractionalToCalendar(sunriseFractional),
            "dhuhr" to fractionalToCalendar(solarNoonFractional),
            "asr" to fractionalToCalendar(asrFractional),
            "maghrib" to fractionalToCalendar(maghribFractional),
            "isha" to fractionalToCalendar(ishaFractional)
        )
    }

    private fun getDayOfYear(year: Int, month: Int, day: Int): Int {
        val daysInMonths = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (isLeapYear(year)) {
            daysInMonths[1] = 29
        }
        var dayCount = day
        for (i in 0 until month - 1) {
            dayCount += daysInMonths[i]
        }
        return dayCount
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
