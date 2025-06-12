package com.approid.mycal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.approid.mycal.ui.theme.MyCalTheme
import com.approid.mycal.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import com.approid.mycal.WeeklyScheduleScreen



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Get the ScheduleViewModel instance first.
        val scheduleViewModel: ScheduleViewModel by viewModels()

        // 2. Create the factory for ScannerViewModel, giving it the scheduleViewModel it needs.
        val scannerViewModelFactory = ScannerViewModelFactory(scheduleViewModel)

        // 3. Get the ScannerViewModel using the factory.
        val scannerViewModel: ScannerViewModel by viewModels { scannerViewModelFactory }

        setContent {
            MyCalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // implement navigation library here sometime in the future(screens and that)

                    //val scheduleViewModel: ScheduleViewModel = viewModel()
                    //WeeklyScheduleScreen(scheduleViewModel = scheduleViewModel)

                    //DocScannerScreen()

                    // 4. Pass the already-created ViewModel to your screen.
                    DocScannerScreen(scannerViewModel = scannerViewModel, scheduleViewModel = scheduleViewModel)


                }
            }
        }
    }
}



















/*class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        // doc scanner
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .build()
        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            MyCalTheme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )

                    val actualScheduleViewModel: ScheduleViewModel = viewModel()

                    WeeklyScheduleScreen(
                        scheduleViewModel = actualScheduleViewModel
                    )


                };

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
                            /*imageUris.forEach { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                ) this is for multiple images*/
                            if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            generatedText?.let { text ->
                                // Option 1: Using your displayText Composable
                                // displayText(response = text)

                                // Option 2: Using a standard Text Composable directly
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
        }
    }
}

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
        text("You are a calendar assistant AI that specializes in understanding and extracting structured event data from various weekly schedule formats, even when the structure is unconventional or inconsistent. \n" +
                "\n" +
                " The weekly schedule may be represented in different ways. For example, days could be rows or columns. Time slots may appear on the side, top, infused within the event block, or not at all. Some cells may span multiple hours or contain extra notes. Please infer structure where needed. \n" +
                "\n" +
                " First, analyze the format of the schedule and briefly explain your reasoning about how the structure is laid out (e.g., which axis represents days vs times, how you inferred the time slots, etc). Then proceed to extract events.  and you must run multiple OCR carefully to get the perfect result we are looking for.\n" +
                "\n" +
                " Output the list of events as JSON objects with the following fields: \n" +
                "\n" +
                " title \n" +
                "\n" +
                " start_time (ISO 8601) \n" +
                "\n" +
                " end_time (ISO 8601) \n" +
                "\n" +
                " day (e.g., 'Monday') \n" +
                "\n" +
                " location (if present) \n" +
                "\n" +
                " notes (optional)”\n" +
                "\n" +
                "\n" +
                "\n" +
                "Enclose the JSON objects within the <JSON> </JSON> tags and if you cannot see or discern a form of schedule or timetable then add <NA> </NA> tags and within them include what you can see and if you can tell its something similar to a schedule provide instructions for taking a better image next time.")
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
}*/



