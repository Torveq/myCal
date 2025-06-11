package com.approid.mycal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// Helper to format LocalTime into a string like "1:30 PM"
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

// Helper to parse a time string like "1:30 PM" into LocalTime
fun parseTime(timeString: String): LocalTime {
    return try {
        LocalTime.parse(timeString, timeFormatter)
    } catch (e: Exception) {
        LocalTime.now() // Fallback
    }
}

/**
 * Main dialog for editing an event.
 */
@Composable
fun EditEventDialog(
    eventToEdit: ScheduleEvent,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(eventToEdit.title) }
    var location by remember { mutableStateOf(eventToEdit.location ?: "") }
    // NOTES CHANGE: Add state for the notes field.
    var notes by remember { mutableStateOf(eventToEdit.notes ?: "") }
    var startTime by remember { mutableStateOf(eventToEdit.startTime) }
    var endTime by remember { mutableStateOf(eventToEdit.endTime) }

    var showTimePickerFor by remember { mutableStateOf<String?>(null) }


    fun adjustTime(timeString: String, amount: Long): String {
        val time = parseTime(timeString)
        return time.plusMinutes(amount).format(timeFormatter)
    }


    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit Event",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // NOTES CHANGE: Add a text field for notes.
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        minLines = 1
                    )
                    //Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeSelector(
                        label = "Start Time",
                        time = startTime,
                        onIncrement = { startTime = adjustTime(startTime, 30) },
                        onDecrement = { startTime = adjustTime(startTime, -30) },
                        onTimeClick = { showTimePickerFor = "Start" },
                        modifier = Modifier.weight(1f)
                    )
                    TimeSelector(
                        label = "End Time",
                        time = endTime,
                        onIncrement = { endTime = adjustTime(endTime, 30) },
                        onDecrement = { endTime = adjustTime(endTime, -30) },
                        onTimeClick = { showTimePickerFor = "End" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updatedEvent = eventToEdit.copy(
                                title = title,
                                location = location,
                                // NOTES CHANGE: Include notes when updating the event.
                                notes = notes,
                                startTime = startTime,
                                endTime = endTime
                            )
                            viewModel.updateEvent(updatedEvent)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    showTimePickerFor?.let { pickerType ->
        val initialTime = if (pickerType == "Start") startTime else endTime
        TimePickerDialog(
            initialTime = parseTime(initialTime),
            onDismiss = { showTimePickerFor = null },
            onTimeSelected = { newTime ->
                if (pickerType == "Start") {
                    startTime = newTime.format(timeFormatter)
                } else {
                    endTime = newTime.format(timeFormatter)
                }
                showTimePickerFor = null
            }
        )
    }
}

@Composable
fun TimeSelector(
    label: String,
    time: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onTimeClick)
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            )
            Column {
                IconButton(onClick = onIncrement, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase time")
                }
                IconButton(onClick = onDecrement, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease time")
                }
            }
        }
    }
}


/**
 * A dialog for picking a specific time with an Apple-style scrolling wheel.
 */
@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit
) {
    val hours = (1..12).toList()
    val minutes = (0..59).toList()
    val amPm = listOf("AM", "PM")

    var selectedHour by remember { mutableStateOf(if (initialTime.hour == 0 || initialTime.hour == 12) 12 else initialTime.hour % 12) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }
    var selectedAmPm by remember { mutableStateOf(if (initialTime.hour < 12) "AM" else "PM") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Time", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(
                    Alignment.CenterHorizontally))
                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Picker
                    ScrollablePicker(items = hours, initialItem = selectedHour, onValueChange = { selectedHour = it })
                    Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
                    // Minute Picker
                    ScrollablePicker(items = minutes, initialItem = selectedMinute, onValueChange = { selectedMinute = it }, format = { "%02d".format(it) })
                    // AM/PM Picker
                    ScrollablePicker(items = amPm, initialItem = selectedAmPm, onValueChange = { selectedAmPm = it }, modifier = Modifier.padding(start = 8.dp))
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val hour24 = when (selectedAmPm) {
                            "PM" -> if (selectedHour == 12) 12 else selectedHour + 12
                            "AM" -> if (selectedHour == 12) 0 else selectedHour
                            else -> 0
                        }
                        onTimeSelected(LocalTime.of(hour24, selectedMinute))
                    }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

/**
 * An iOS-style scrolling picker that highlights and snaps to the central item.
 */
@Composable
fun <T> ScrollablePicker(
    items: List<T>,
    initialItem: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    visibleItemsCount: Int = 3,
    format: (T) -> String = { it.toString() }
) {
    val listState = rememberLazyListState(items.indexOf(initialItem))
    val coroutineScope = rememberCoroutineScope()
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    // This derived state calculates the index of the item that is currently in the center.
    val centralItemIndex by remember {
        derivedStateOf {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset
            (firstVisibleIndex + (firstVisibleOffset / itemHeightPx).toInt()).coerceIn(items.indices)
        }
    }

    // Effect to snap to the nearest item when scrolling stops.
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val targetIndex = centralItemIndex
            // Animate the scroll to the calculated central item.
            listState.animateScrollToItem(targetIndex)
            // Report the new value.
            onValueChange(items[targetIndex])
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .width(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // The highlight box that frames the selected item.
        Box(
            modifier = Modifier
                .height(itemHeight)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(items.size) { index ->
                val item = items[index]

                // Calculate the distance from the center to apply transformations.
                val distanceFromCenter = abs(index - centralItemIndex)
                val scrollOffsetRatio = (listState.firstVisibleItemScrollOffset / itemHeightPx).let { it - it.toInt() }
                val currentItemOffset = abs(index - listState.firstVisibleItemIndex - scrollOffsetRatio)

                // The scale and alpha are interpolated to create a smooth transition.
                val scale by animateFloatAsState(targetValue = if (distanceFromCenter == 0) 1f else 1f - (currentItemOffset * 0.25f).coerceAtMost(0.25f))
                val alpha by animateFloatAsState(targetValue = if (distanceFromCenter == 0) 1f else 1f - (currentItemOffset * 0.5f).coerceAtMost(0.75f))

                Box(modifier = Modifier.height(itemHeight), contentAlignment = Alignment.Center) {
                    Text(
                        text = format(item),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                            .clickable {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                    )
                }
            }
        }
    }
}
