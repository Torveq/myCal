package com.approid.mycal.scanner
/*
import android.app.Activity
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
import coil.compose.AsyncImage
import com.approid.mycal.ScannerUiState
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

/**
 * A "dumb" UI component for the document scanning screen.
 * It displays the state provided by the ViewModel and calls callbacks on user actions.
 *
 * @param uiState The current state of the UI to display.
 * @param onImageScanned Callback invoked when the scanner successfully returns an image URI.
 * @param onDismissError Callback invoked when the error pop-up is dismissed.
 */
@Composable
fun DocScannerScreen(
    uiState: ScannerUiState,
    onImageScanned: (Uri) -> Unit,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            ?.pages
            ?.firstOrNull()
            ?.imageUri
            ?.let { uri ->
                // The screen's only job is to report the result back up.
                onImageScanned(uri)
            }
    }

    // making its dependency clear and removing the unsafe cast from its definition.
    val startScan = { activity: Activity ->
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { scannerLauncher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener { Toast.makeText(context, "Scan failed: ${it.message}", Toast.LENGTH_SHORT).show()}
    }

    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Analyzing image...")
        } else {
            // The UI is now driven entirely by the state object
            // The check for success/error now happens in MainActivity
            Text(
                text = "Scan a document to import your schedule.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { startScan(context as Activity) }) {
                Text(text = "Scan Schedule")
            }
        }
    }

    // Show the pop-up if there's an error message in the state
    uiState.errorPopupMessage?.let { message ->
        InfoPopup(
            onRetry = {
                onDismissError() // First, clear the error state
                startScan(context as Activity)      // Then, try scanning again
            },
            message = message
        )
    }
}


@Composable
fun InfoPopup(onRetry: () -> Unit, message: String) {
    var showFullScreenImage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageFile = remember { File(context.filesDir, "scan.jpeg") }

    Dialog(onDismissRequest = { /* Do nothing: not dismissible */ }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (imageFile.exists()) {
                        ClickableImageThumbnail(
                            file = imageFile,
                            onClick = { showFullScreenImage = true }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
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
            .size(48.dp)
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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
*/