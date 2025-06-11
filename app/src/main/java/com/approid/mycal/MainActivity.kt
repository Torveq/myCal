package com.approid.mycal

// You can comment out unused imports to clean up the file
/*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
*/

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.approid.mycal.ui.theme.MyCalTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyCalTheme {
                // Use a Surface as the main container for your screen.
                // This ensures a proper background and layout behavior.
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Call ONLY the screen you want to test.
                    WeeklyScheduleScreen(scheduleViewModel = viewModel())
                }

                // --- ALL THE CODE BELOW IS COMMENTED OUT FOR THE TEST ---
                /*
                var imageUri by remember { mutableStateOf<Uri?>(null) }
                var generatedText by remember { mutableStateOf<String?>(null) }
                val scannerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = {
                        if(it.resultCode == RESULT_OK) {
                            val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                            imageUri = result?.pages?.firstOrNull()?.imageUri

                            // Save the JPEG to filesDir
                            result?.pages?.firstOrNull()?.imageUri?.let { uri ->
                                val inputStream = contentResolver.openInputStream(uri)
                                val outputFile = File(filesDir, "scan.jpeg")
                                val outputStream = FileOutputStream(outputFile)
                                //val imageBitMap = fileToBitmap(outputFile)
                                inputStream?.use { input ->
                                    outputStream.use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                // Execute the suspend function after saving the image
                                lifecycleScope.launch {
                                    val apiCallResponse =
                                        textGenMultimodalOneImagePrompt(context = this@MainActivity)
                                    generatedText = apiCallResponse
                                }
                            }
                        }
                    }
                )

                Column (
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    generatedText?.let { text ->
                        Text(
                            text = text
                        )
                    }

                    Button(onClick = {
                        scanner.getStartScanIntent(this@MainActivity)
                            .addOnSuccessListener {
                                scannerLauncher.launch(IntentSenderRequest.Builder(it).build())
                            }
                            .addOnFailureListener {
                                Toast.makeText(applicationContext, it.message, Toast.LENGTH_LONG).show()
                            }

                    }) {
                        Text(text = "\n\n\nScan Document\n\n\n")
                    }
                }
                */
            }
        }
    }
}

// You can also comment out helper composables that are not being used in the test.
/*
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyCalTheme {
        Greeting("Android")
    }
}


suspend fun textGenMultimodalOneImagePrompt(context: Context): String {
    val generativeModel =
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMAPI
        )

    val filePath = context.filesDir.path + "/scan.jpeg"
    val imageFile = File(filePath)
    val image: Bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

    val inputContent = content {
        image(image)
        text("You are a calendar assistant AI...") // Shortened for brevity
    }

    val response = generativeModel.generateContent(inputContent)

    return response.text.toString()
}

@Composable
fun displayText(response: String) {
    print(response)

    Text(
        text = response,
        modifier = Modifier.fillMaxWidth()
    )
}
*/