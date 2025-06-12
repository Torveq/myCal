package com.approid.mycal

//package com.approid.mycal.ui.scanner

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// holds the state and logic.
class ScannerViewModel(private val scheduleViewModel: ScheduleViewModel) : ViewModel() {

    // --- State Variables ---
    var isLoading by mutableStateOf(false)
        private set

    var generatedText by mutableStateOf<String?>(null)
        private set

    var showInfoPopup by mutableStateOf(false)
        private set

    var scanSuccess by mutableStateOf(false)
        private set

    /**
     * The main function to process the scanned image.
     */
    fun processScannedImage(context: Context) {
        viewModelScope.launch {
            isLoading = true
            resetState() // Clear previous results before starting

            try {
                val apiCallResponse = GeminiAPI.getScheduleFromImage(context)

                if (apiCallResponse.contains("<JSON>")) {
                    showInfoPopup = false
                    val json = apiCallResponse.substringAfter("<JSON>").substringBefore("</JSON>")
                    generatedText = json

                    val events = parseScheduleEvents(json)
                    events.forEach { scheduleViewModel.addEvent(it) }
                    scanSuccess = true

                } else if (apiCallResponse.contains("<NA>")) {
                    showInfoPopup = true
                    generatedText = apiCallResponse.substringAfter("<NA>").substringBefore("</NA>").trim()
                } else {
                    showInfoPopup = true
                    generatedText = "An unexpected error occurred. Please try again."
                }
            } catch (e: Exception) {
                generatedText = "Failed to scan: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Resets the state for a new scan or a retry.
     */
    fun resetState() {
        generatedText = null
    }
}

// This is the FACTORY. Its only job is to create the ScannerViewModel.
class ScannerViewModelFactory(private val scheduleViewModel: ScheduleViewModel) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // This check ensures you are creating the correct ViewModel
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // It creates a new ScannerViewModel and passes the scheduleViewModel to it
            return ScannerViewModel(scheduleViewModel) as T
        }
        // This will throw an error if you try to use this factory for a different ViewModel
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



@Composable
fun DocScannerScreen(scannerViewModel: ScannerViewModel, scheduleViewModel: ScheduleViewModel) {  //now just accepts view model as a parameter cause factory responsible for creating it
    val context = LocalContext.current
    //val coroutineScope = rememberCoroutineScope()
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Observe state directly from the ViewModel passed into the function
    val isLoading = scannerViewModel.isLoading
    val generatedText = scannerViewModel.generatedText
    var showInfoPopup = scannerViewModel.showInfoPopup
    var scanSuccess = scannerViewModel.scanSuccess


    // --- Scanner Setup ---
    val options = remember {
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(options) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            imageUri = scanningResult?.pages?.firstOrNull()?.imageUri

            scanningResult?.pages?.firstOrNull()?.imageUri?.let { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputFile = File(context.filesDir, "scan.jpeg")
                val outputStream = FileOutputStream(outputFile)

                inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }

                scannerViewModel.processScannedImage(context)

                /*isLoading = true
                generatedText = null
                coroutineScope.launch {
                    val apiCallResponse = GeminiAPI.getScheduleFromImage(context)
                    isLoading = false

                    // Logic to handle the API response is now directly here
                    if (apiCallResponse.contains("<NA>")) {
                        popupMessage = apiCallResponse.substringAfter("<NA>").substringBefore("</NA>").trim()
                        generatedText = popupMessage
                        showInfoPopup = true
                    } else if (apiCallResponse.contains("<JSON>")) {
                        val extractedJson = apiCallResponse.substringAfter("<JSON>").substringBefore("</JSON>")
                        generatedText = extractedJson // You can now parse this JSON string
                        val listEvents = parseScheduleEvents(generatedText!!)


                    } else {
                        // Handle cases where neither tag is present
                        popupMessage = "An unexpected error occurred. Please try again."
                        showInfoPopup = true
                    }
                }*/
            }
        }
    )
    // A reusable function to start the scan
    val startScan = {
        scanner.getStartScanIntent(context as Activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            }
    }


    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Analyzing image...")
        } else {
            // Display the extracted JSON or a prompt to scan
            Text(
                text = ""  //TODO: Figure out why i get infopopup with unexpectederror catch string if i dont have this eles clause in place
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // The "Scan Schedule" button is now only shown in the initial state,
        // before any scan has been processed. It disappears once pressed.
        if (generatedText == null && !isLoading) {
            Text(
                text = "Hello, welcome to my app, myCal.\n         I hope you find purpose.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { startScan() }) {
                Text(text = "Scan Schedule")
            }
        }
    }

    if (showInfoPopup) {
        if (generatedText != null) {
            InfoPopup(
                onRetry = {
                    showInfoPopup = false
                    startScan()
                },
                message = generatedText
            )
        }
    }
    else if (scanSuccess){
        WeeklyScheduleScreen(scheduleViewModel)
    }
}

/**
 * A pop-up dialog that shows a message, a clickable image icon, and a retry button.
 * The dialog is not dismissible and the text area is scrollable.
 *
 * @param onRetry Lambda for when the user clicks the "Retry Scan" button.
 * @param message The detailed informational message.
 */
@Composable
fun InfoPopup(
    onRetry: () -> Unit,
    message: String
) {
    var showFullScreenImage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageFile = remember { File(context.filesDir, "scan.jpeg") }

    Dialog(onDismissRequest = { /* Do nothing: not dismissible */ }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp)
            ) {
                // Scrollable text area
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false) // Takes available space, but won't grow infinitely
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp) // Padding for the text itself
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom row for controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image thumbnail on the left
                    if (imageFile.exists()) {
                        ClickableImageThumbnail(
                            file = imageFile,
                            onClick = { showFullScreenImage = true }
                        )
                    }

                    // Spacer pushes the button to the right
                    Spacer(modifier = Modifier.weight(1f))

                    // Retry button on the right
                    Button(onClick = onRetry) {
                        Text("Retry Scan")
                    }
                }
            }
        }
    }

    if (showFullScreenImage) {
        FullScreenImageDialog(
            file = imageFile,
            onDismiss = { showFullScreenImage = false }
        )
    }
}

@Composable
private fun ClickableImageThumbnail(file: File, onClick: () -> Unit) {
    AsyncImage(
        model = file,
        contentDescription = "Scanned image thumbnail",
        modifier = Modifier
            .size(48.dp) // Slightly smaller for this layout
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun FullScreenImageDialog(file: File?, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = file,
                contentDescription = "Full-screen scanned image",
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close full-screen image",
                    tint = Color.White
                )
            }
        }
    }
}



