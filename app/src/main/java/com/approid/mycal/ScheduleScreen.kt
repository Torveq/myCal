@file:OptIn(ExperimentalFoundationApi::class)

package com.approid.mycal

import android.content.ClipData
import android.content.ClipDescription
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.input.pointer.pointerInput
import java.util.UUID


// Data class to represent a single schedule event
data class ScheduleEvent(
    val id: String = UUID.randomUUID().toString(), // Unique ID for each event
    var title: String,
    var startTime: String,
    var endTime: String,
    var day: DayOfWeek,
    var location: String? = null, // Optional location
    var notes: String? = null // Optional notes
)

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

// ViewModel to hold and manage the schedule state
class ScheduleViewModel : ViewModel() {
    // A list of events, wrapped in mutableStateListOf to trigger recomposition on changes
    val events = mutableStateListOf<ScheduleEvent>()

    // Map to group events by day for easier display
    val eventsByDay: Map<DayOfWeek, List<ScheduleEvent>>
        get() = events.groupBy { it.day }

    fun addEvent(event: ScheduleEvent) {
        events.add(event)
    }

    fun deleteEvent(eventId: String) {
        events.removeAll { it.id == eventId }
    }

    fun updateEvent(updatedEvent: ScheduleEvent) {
        val index = events.indexOfFirst { it.id == updatedEvent.id }
        if (index != -1) {
            events[index] = updatedEvent
        }
    }

    fun moveEvent(eventId: String, newDay: DayOfWeek) {
        val eventIndex = events.indexOfFirst { it.id == eventId }
        if (eventIndex != -1) {
            val event = events[eventIndex]
            events[eventIndex] = event.copy(day = newDay)
        }
    }

    init {populateSampleData()}
    // Function to add some sample data for preview
    private fun populateSampleData() {
        if (events.isEmpty()) { // Only add if empty to avoid duplication on recomposition
            addEvent(ScheduleEvent(title = "Team Meeting", startTime = "10:00 AM", endTime = "11:00AM", location = "Room A", day = DayOfWeek.MONDAY))
            addEvent(ScheduleEvent(title = "Lunch with Client", startTime = "1:00 PM", endTime = "2:00PM", location = "Cafe Central", day = DayOfWeek.MONDAY))
            addEvent(ScheduleEvent(title = "Project Sync", startTime = "3:00 PM", endTime = "4:00PM", location = "Online", day = DayOfWeek.WEDNESDAY))
            addEvent(ScheduleEvent(title = "Gym Session", startTime = "6:00 PM", endTime = "7:00PM", location = "City Fitness", day = DayOfWeek.THURSDAY))
            addEvent(ScheduleEvent(title = "Weekend Prep", startTime = "4:00 PM", endTime = "5:00PM", location = "Home", day = DayOfWeek.FRIDAY))
            addEvent(ScheduleEvent(title = "Grocery Shopping", startTime = "11:00 AM", endTime = "12:00PM", location = "Supermarket", day = DayOfWeek.SATURDAY))
        }
    }
}

// CHANGE: Create a parent composable to own and manage the drag-and-drop state.
// This is crucial for coordinating the drag source and drop targets.
@Composable
fun WeeklyScheduleScreen(scheduleViewModel: ScheduleViewModel = viewModel()) {
    // CHANGE: State to track the ID of the event currently being dragged.
    // When an event is dragged, its ID is stored here. When the drag ends, it's set back to null.
    // This state is what drives the AnimatedVisibility.
    var draggedEventId by remember { mutableStateOf<String?>(null) }
    val days = DayOfWeek.values()

    Column {
        DayHeaderRow(days)
        LazyVerticalGrid(
            columns = GridCells.Fixed(days.size),
        ) {
            days.forEach { day ->
                item(span = { GridItemSpan(1) }) {
                    DayColumn(
                        day = day,
                        events = scheduleViewModel.eventsByDay[day] ?: emptyList(),
                        viewModel = scheduleViewModel,
                        // CHANGE: Pass the dragged item ID and the callbacks down to the children.
                        draggedEventId = draggedEventId,
                        onDragStart = { eventId -> draggedEventId = eventId },
                        onDragEnd = { draggedEventId = null }
                    )
                }
            }
        }
    }
}

@Composable
fun DayHeaderRow(days: Array<DayOfWeek>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { day ->
            Text(
                text = day.name.take(3), // Display "MON", "TUE", etc.
                modifier = Modifier.padding(8.dp).weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DayColumn(
    day: DayOfWeek,
    events: List<ScheduleEvent>,
    viewModel: ScheduleViewModel,
    // CHANGE: Accept the state and callbacks from the parent.
    draggedEventId: String?,
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit
) {
    // CHANGE: State to provide visual feedback when an item is being dragged over this column.
    var isDropTarget by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(4.dp)
            .border(2.dp, if (isDropTarget) MaterialTheme.colorScheme.primary else Color.LightGray)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.toAndroidDragEvent().clipDescription?.hasMimeType("application/vnd.mycal.event") == true
                },
                target = remember {
                    object: DragAndDropTarget {
                        // CHANGE: onEntered is called when the dragged item first enters the column's bounds.
                        override fun onEntered(event: DragAndDropEvent) {
                            super.onEntered(event)
                            // We set the state to true to trigger the border color change.
                            isDropTarget = true
                        }

                        // CHANGE: onExited is called when the dragged item leaves the column's bounds.
                        override fun onExited(event: DragAndDropEvent) {
                            super.onExited(event)
                            // We reset the state to remove the highlight.
                            isDropTarget = false
                        }

                        // CHANGE: onDrop is called when the user releases the item over this column.
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            // The data we sent is in the ClipData. We extract the item (the event ID).
                            val draggedId = event.toAndroidDragEvent().clipData.getItemAt(0).text.toString()

                            // If we successfully get the ID, we tell the ViewModel to move the event.
                            if (draggedId.isNotEmpty()) {
                                viewModel.moveEvent(draggedId, day)
                            }
                            // Reset the highlight, as the drop is complete.
                            isDropTarget = false
                            // Return true to indicate that the drop was successfully handled.
                            return true
                        }
                    }
                }
            )
    ) {
        // Display events for this day
        events.forEach { event ->
            // CHANGE: The EventItem is wrapped in AnimatedVisibility.
            // It is only 'visible' if its ID does not match the ID of the item currently being dragged.
            // This makes the original item disappear while its "ghost" is being dragged.
            AnimatedVisibility(
                visible = draggedEventId != event.id,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EventItem(
                    event = event,
                    viewModel = viewModel,
                    // CHANGE: Pass the callbacks down to the EventItem.
                    onDragStart = onDragStart,
                    onDragEnd = onDragEnd
                )
            }
        }
        // You might want a minimum height for empty day columns or a placeholder
        if (events.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) // Placeholder for empty space
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventItem(
    event: ScheduleEvent,
    viewModel: ScheduleViewModel,
    // CHANGE: Accept callbacks to notify the parent about drag state changes.
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .dragAndDropSource {
                // We use a pointerInput scope implicitly(we are in dndsource scope which is in pointerInput scope) to detect the long press gesture that will start the drag.
                detectTapGestures(
                    onLongPress = {
                        // CHANGE: When a long press is detected, we build and start the data transfer.
                        // The try/finally block is crucial. The 'finally' block ensures that
                        // onDragEnd() is always called, even if the drag is cancelled,
                        // making sure the original item reappears correctly.
                        try {
                            // First, we notify the parent that a drag has started.
                            // This sets the 'draggedEventId' and hides this original item via AnimatedVisibility.
                            onDragStart(event.id)

                            // Then, we start the actual system-level drag operation.
                            startTransfer(
                                DragAndDropTransferData(
                                    // We send the event's ID as plain text.
                                    // We also give it a custom MIME type so our drop target can identify it.
                                    clipData = ClipData(
                                        ClipDescription("Schedule Event", arrayOf("application/vnd.mycal.event")),
                                        ClipData.Item(event.id)
                                    ),
                                    // The system automatically creates a "drag shadow" (a semi-transparent
                                    // image of the composable being dragged), so we don't need to draw it manually.
                                )
                            )
                        } finally {
                            // This block executes when the drag is over (dropped or cancelled).
                            // We notify the parent to reset the state, making the original item visible again.
                            onDragEnd()
                        }
                    }
                )
            }
            .combinedClickable(
                onClick = { /* Maybe open event details */ },
                onLongClick = { showOptionsMenu = true } // Show options on long press
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = event.title, fontWeight = FontWeight.Bold)
            Text(text = "${event.startTime} - ${event.endTime}")
            event.location?.let { Text(text = "Location: $it") }

            // Options Menu (DropdownMenu)
            DropdownMenu(
                expanded = showOptionsMenu,
                onDismissRequest = { showOptionsMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showOptionsMenu = false
                        // TODO: Navigate to an edit screen or show an edit dialog
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        viewModel.deleteEvent(event.id)
                        showOptionsMenu = false
                    }
                )
            }
        }
    }
}










































/* Sample schedule data (same as your JSON)
val sampleScheduleData = listOf(
    ScheduleEvent(title="Mathematics", "09:00", "10:00", "Monday"),
    ScheduleEvent("English", "10:00", "11:00", "Monday"),
    ScheduleEvent("Break", "11:00", "11:30", "Monday"),
    ScheduleEvent("Science", "11:30", "12:30", "Monday"),
    ScheduleEvent("Lunch", "12:30", "13:30", "Monday"),
    ScheduleEvent("History", "13:30", "14:30", "Monday"),
    ScheduleEvent("Geography", "14:30", "15:30", "Monday"),
    ScheduleEvent("English", "09:00", "10:00", "Tuesday"),
    ScheduleEvent("Science", "10:00", "11:00", "Tuesday"),
    ScheduleEvent("Break", "11:00", "11:30", "Tuesday"),
    ScheduleEvent("Mathematics", "11:30", "12:30", "Tuesday"),
    ScheduleEvent("Lunch", "12:30", "13:30", "Tuesday"),
    ScheduleEvent("Art", "13:30", "14:30", "Tuesday"),
    ScheduleEvent("Music", "14:30", "15:30", "Tuesday"),
    ScheduleEvent("Science", "09:00", "10:00", "Wednesday"),
    ScheduleEvent("Mathematics", "10:00", "11:00", "Wednesday"),
    ScheduleEvent("Break", "11:00", "11:30", "Wednesday"),
    ScheduleEvent("English", "11:30", "12:30", "Wednesday"),
    ScheduleEvent("Lunch", "12:30", "13:30", "Wednesday"),
    ScheduleEvent("P.E.", "13:30", "14:30", "Wednesday"),
    ScheduleEvent("I.T.", "14:30", "15:30", "Wednesday"),
    ScheduleEvent("History", "09:00", "10:00", "Thursday"),
    ScheduleEvent("Geography", "10:00", "11:00", "Thursday"),
    ScheduleEvent("Break", "11:00", "11:30", "Thursday"),
    ScheduleEvent("Mathematics", "11:30", "12:30", "Thursday"),
    ScheduleEvent("Lunch", "12:30", "13:30", "Thursday"),
    ScheduleEvent("Science", "13:30", "14:30", "Thursday"),
    ScheduleEvent("English", "14:30", "15:30", "Thursday"),
    ScheduleEvent("P.E.", "09:00", "10:00", "Friday"),
    ScheduleEvent("English", "10:00", "11:00", "Friday"),
    ScheduleEvent("Break", "11:00", "11:30", "Friday"),
    ScheduleEvent("Science", "11:30", "12:30", "Friday"),
    ScheduleEvent("Lunch", "12:30", "13:30", "Friday"),
    ScheduleEvent("Mathematics", "13:30", "14:30", "Friday"),
    ScheduleEvent("French", "14:30", "15:30", "Friday")
)




@Composable
fun ScheduleScreen(scheduleData: List<ScheduleEvent>) {
    // Define the days of the week in order
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    // Get unique time slots and sort them
    val timeSlots = scheduleData
        .map { "${it.startTime} - ${it.endTime}" }
        .distinct()
        .sortedWith(compareBy { it.split(" - ")[0] }) // Sort by start time

    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = "School Timetable",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        // Header Row for Days
        Row(Modifier.fillMaxWidth()) {
            // Empty cell for time column alignment
            Box(modifier = Modifier.weight(0.2f).border(1.dp, Color.LightGray).padding(4.dp)) {
                Text(text = "Time", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
            }
            // Day headers
            daysOfWeek.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f) // Each day column takes equal space
                        .border(1.dp, Color.LightGray)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = day, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center, fontSize = 11.sp)
                }
            }
        }

        // Schedule Rows (Time slots and events)
        LazyColumn {
            items(timeSlots) { timeSlot ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Time Slot Cell
                    Box(
                        modifier = Modifier
                            .weight(0.2f) // Corresponds to the "Time" header's weight
                            .height(IntrinsicSize.Min) // Ensure height matches content or other cells
                            .border(1.dp, Color.LightGray)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeSlot,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Event Cells for each day in this time slot
                    daysOfWeek.forEach { day ->
                        val event = scheduleData.find { it.day == day && "${it.startTime} - ${it.endTime}" == timeSlot }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp) // Fixed height for event cells
                                .border(1.dp, Color.LightGray)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = event?.title ?: "", // Display event title or empty string
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                lineHeight = 12.sp // Helps with multi-line text in small boxes
                            )
                        }
                    }
                }
            }
        }
    }
}

// Basic Theme definition (You might have this in Theme.kt)
@Composable
fun WeeklySchedulerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme( // or darkColorScheme()
            primary = Color(0xFF6200EE),
            primaryContainer = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6),
            secondaryContainer = Color(0xFF018786),
            // Add other colors as needed
        ),
        typography = Typography(), // Default typography
        content = content
    )
}

@Preview(showBackground = true, widthDp = 380, heightDp = 600)
@Composable
fun ScheduleScreenPreview() {
    WeeklySchedulerTheme {
        ScheduleScreen(scheduleData = sampleScheduleData)
    }
}
*/