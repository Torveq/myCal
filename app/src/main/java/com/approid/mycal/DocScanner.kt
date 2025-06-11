package com.approid.mycal

//package com.approid.mycal.ui.scanner
import com.approid.mycal.GeminiAPI

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun DocScannerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var generatedText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Set up the document scanner
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

            // Save the scanned image to internal storage
            scanningResult?.pages?.firstOrNull()?.imageUri?.let { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputFile = File(context.filesDir, "scan.jpeg")
                val outputStream = FileOutputStream(outputFile)

                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                // Call the Gemini API after saving
                isLoading = true
                generatedText = null
                coroutineScope.launch {
                    val apiCallResponse = GeminiAPI.getScheduleFromImage(context)
                    generatedText = apiCallResponse
                    isLoading = false
                }
            }

        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Scanned document",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false) // Allow image to take space but not push content out
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Analyzing image...")
        } else {
            generatedText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            scanner.getStartScanIntent(context as Activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener {
                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                }
        }) {
            Text(text = "Scan Schedule")
        }
    }
}


/**
 * A reusable, static helper composable to display the locally saved "scan.jpeg".
 * This can be used from any other screen in the app long after the scan is complete.
 *
 * @param modifier The modifier to be applied to the image.
 * @param imageFile The File object pointing to the image to display. If null, it defaults
 * to "scan.jpeg" in the app's internal files directory.
 */
@Composable
fun ScannedImageView(
    modifier: Modifier = Modifier,
    imageFile: File? = null
) {
    val context = LocalContext.current
    // If imageFile is null, default to the scan.jpeg in the app's files directory
    val fileToShow = imageFile ?: File(context.filesDir, "scan.jpeg")

    if (fileToShow.exists()) {
        AsyncImage(
            model = fileToShow,
            contentDescription = "Saved scanned document",
            contentScale = ContentScale.FillWidth,
            modifier = modifier.fillMaxWidth()
        )
    }
}
