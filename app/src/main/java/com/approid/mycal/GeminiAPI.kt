package com.approid.mycal

//package com.approid.mycal.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.approid.mycal.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import java.io.File

/**
 * Encapsulates the logic for interacting with the Gemini API.
 * A singleton cause you won't need multiple instances of it as a class.
 */
object GeminiAPI {

    suspend fun getScheduleFromImage(context: Context): String {
        val generativeModel =
            GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = BuildConfig.GEMAPI
            )

        // Path to the scanned image saved in the app's internal storage
        val filePath = context.filesDir.path + "/scan.jpeg"
        val imageFile = File(filePath)

        if (!imageFile.exists()) {
            return "Error: Scanned image not found."
        }
        val image: Bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

        // think about the ISO 8601 international time convention
        val inputContent = content {
            image(image)
            text("You are a calendar assistant AI that specializes in understanding and extracting structured event data from various weekly schedule formats, even when the structure is unconventional or inconsistent. \n" +
                    "\n" +
                    " The weekly schedule may be represented in different ways. For example, days could be rows or columns. Time slots may appear on the side, top, infused within the event block, or not at all. Some cells may span multiple hours or contain extra notes. Please infer structure where needed. \n" +
                    "\n" +
                    " First, analyze the format of the schedule and briefly explain your reasoning about how the structure is laid out (e.g., which axis represents days vs times, how you inferred the time slots, etc). Then proceed to extract events.  and you must run multiple OCR carefully to get the perfect result we are looking for.\n" +
                    "\n" +
                    "I want everything to match exactly as in the examples if provided, most importantly for day and time, day must be in uppercase completely and time must be in AM/PM(translate from military time if necessary) and one space between the time and AM/PM classification(e.g. '1:00 PM')" +
                    "\n" +
                    " Output the list of events as JSON objects with the following fields: \n" +
                    "\n" +
                    " title \n" +
                    "\n" +
                    " start_time (e.g. '1:00 PM', '12:30 AM') \n" +
                    "\n" +
                    " end_time (e.g. '1:00 PM', '12:30 AM') \n" +
                    "\n" +
                    " day (e.g., 'MONDAY') \n" +
                    "\n" +
                    " location (if present) \n" +
                    "\n" +
                    " notes (optional)”\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "Enclose the JSON objects within the <JSON> </JSON> tags and if you cannot see or discern a form of schedule or timetable then add <NA> </NA> tags and within them include what you can see and if you can tell its something similar to a schedule provide instructions for taking a better image next time - the text in between the <NA> </NA> tags shall not exceed more than 40 words."
            )

        }

        return try {
            val response = generativeModel.generateContent(inputContent)
            android.util.Log.d("API Response", response.text ?: "No response text")
            response.text ?: "Error: Received an empty response from the API."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
