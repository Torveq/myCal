package com.approid.mycal

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
//TODO: fix the .ics format thingy debug in log cause parse error when exporting
/**
 * Converts a list of ScheduleEvent objects into an iCalendar (.ics) file string.
 *
 * @param events The list of schedule events to convert.
 * @param prodId A unique identifier for the product that created the calendar.
 * @return A string containing the calendar data in .ics format.
 */
fun createIcsFromSchedule(events: List<ScheduleEvent>, prodId: String = "-//YourApp//YourApp//EN"): String {
    val icsBuilder = StringBuilder()
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // --- Calendar Header ---
    icsBuilder.appendLine("BEGIN:VCALENDAR")
    icsBuilder.appendLine("VERSION:2.0")
    icsBuilder.appendLine("PRODID:$prodId")
    icsBuilder.appendLine("CALSCALE:GREGORIAN")

    val today = LocalDate.now()

    // --- Calendar Events ---
    for (event in events) {
        try {
            // Find the date of the next occurrence of the event's day of the week
            val eventDate = today.with(TemporalAdjusters.nextOrSame(event.day.toJavaDayOfWeek))
            val startTime = LocalTime.parse(event.startTime, timeFormatter)
            val endTime = LocalTime.parse(event.endTime, timeFormatter)

            val startDateTime = eventDate.atTime(startTime)
            val endDateTime = eventDate.atTime(endTime)

            // Get current time for DTSTAMP (when the event was created in the calendar)
            val creationTimestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))

            icsBuilder.appendLine("BEGIN:VEVENT")
            icsBuilder.appendLine("UID:${event.id}")
            icsBuilder.appendLine("DTSTAMP:$creationTimestamp")
            icsBuilder.appendLine("DTSTART:${startDateTime.format(dateTimeFormatter)}")
            icsBuilder.appendLine("DTEND:${endDateTime.format(dateTimeFormatter)}")

            // Add a weekly recurrence rule
            icsBuilder.appendLine("RRULE:FREQ=WEEKLY")

            icsBuilder.appendLine("SUMMARY:${event.title}")
            event.location?.let { icsBuilder.appendLine("LOCATION:$it") }
            event.notes?.let { icsBuilder.appendLine("DESCRIPTION:$it") }
            icsBuilder.appendLine("END:VEVENT")

        } catch (e: Exception) {
            // Log or handle events with malformed time strings
            System.err.println("Could not process event '${event.title}' due to an error: ${e.message}")
        }
    }

    // --- Calendar Footer ---
    icsBuilder.appendLine("END:VCALENDAR")

    return icsBuilder.toString()
}

fun shareIcsFile(context: Context, icsContent: String) {
    try {
        // 1. Get the cache directory
        val cachePath = File(context.cacheDir, "ics_files/")
        cachePath.mkdirs() // Create the directory if it doesn't exist

        // 2. Create the file and write the .ics content to it
        val file = File(cachePath, "my_schedule.ics")
        file.writeText(icsContent)

        // 3. Get the content URI for the file using the FileProvider
        //    The authority MUST match what you put in AndroidManifest.xml
        val contentUri = FileProvider.getUriForFile(
            context,
            "com.approid.mycal.fileprovider",
            file
        )

        if (contentUri != null) {
            // 4. Create an Intent to view the file
            val shareIntent = Intent(Intent.ACTION_VIEW).apply {
                // Set the data and the MIME type for .ics files
                setDataAndType(contentUri, "text/calendar")
                // Grant temporary read permission to the app that handles the intent
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 5. Launch the intent chooser
            val chooser = Intent.createChooser(shareIntent, "Open with")
            context.startActivity(chooser)
        }

    } catch (e: IOException) {
        // Handle file I/O errors
        e.printStackTrace()
    }
}