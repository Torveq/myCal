package com.approid.mycal
/*
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Represents the different states of the scanner screen UI.
 * This is the single source of truth for what the UI should display.
 */
data class ScannerUiState(
    val isLoading: Boolean = false,
    // The state now holds the final, parsed list of events, not the raw string.
    val successfullyParsedEvents: List<ScheduleEvent>? = null,
    val errorPopupMessage: String? = null
)

/**
 * Manages the state and business logic for the document scanning feature.
 * It is completely decoupled from the UI.
 */
class ScannerViewModel : ViewModel() {

    // Private mutable state flow that only the ViewModel can modify.
    private val _uiState = MutableStateFlow(ScannerUiState())
    // Public, read-only state flow that the UI can observe for changes.
    val uiState = _uiState.asStateFlow()

    /**
     * This is the main function that MainActivity will call.
     * It orchestrates the entire process of saving the image, calling the API,
     * and parsing the result.
     */
    fun processScannedImage(context: Context, uri: Uri) {
        // 1. Set the UI to a loading state.
        _uiState.update { it.copy(isLoading = true, errorPopupMessage = null) }

        // 2. Save the image from the URI to a local file.
        saveUriToFile(context, uri)

        // 3. Launch a coroutine to perform the network request off the main thread.
        viewModelScope.launch {
            val apiCallResponse = GeminiAPI.getScheduleFromImage(context)

            // 4. Handle the API response and update the state accordingly.
            if (apiCallResponse.contains("<JSON>")) {
                val extractedJson = apiCallResponse.substringAfter("<JSON>").substringBefore("</JSON>")
                val parsedEvents = parseScheduleEvents(extractedJson)
                _uiState.update {
                    it.copy(isLoading = false, successfullyParsedEvents = parsedEvents)
                }
            } else {
                val errorMessage = if (apiCallResponse.contains("<NA>")) {
                    apiCallResponse.substringAfter("<NA>").substringBefore("</NA>").trim()
                } else {
                    "An unexpected error occurred. Please try again."
                }
                _uiState.update {
                    it.copy(isLoading = false, errorPopupMessage = errorMessage)
                }
            }
        }
    }

    /**
     * Resets the error message, allowing the UI to dismiss the error pop-up.
     */
    fun dismissError() {
        _uiState.update { it.copy(errorPopupMessage = null) }
    }

    private fun saveUriToFile(context: Context, uri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputFile = File(context.filesDir, "scan.jpeg")
            val outputStream = FileOutputStream(outputFile)
            inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(isLoading = false, errorPopupMessage = "Failed to save image.") }
        }
    }
}
*/