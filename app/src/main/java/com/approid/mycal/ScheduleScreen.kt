package com.approid.mycal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel


// Data class to represent a single schedule event
data class ScheduleEvent(
    val id: String = java.util.UUID.randomUUID().toString(), // Unique ID for each event
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

    // Function to add some sample data for preview
    fun populateSampleData() {
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

@Composable
fun WeeklyScheduleView(scheduleViewModel: ScheduleViewModel = viewModel()) {
    val days = DayOfWeek.values() // TODO: adjust so that only days with events are in the lists

    Column {
        // Header Row for Days
        DayHeaderRow(days)

        // Grid for Events
        LazyVerticalGrid(
            columns = GridCells.Fixed(days.size), // One column for each day
            // You can also use GridCells.Adaptive(minSize = /* some Dp value */)
            // if you want the number of columns to change based on screen width
        ) {
            // Iterate through each day to create columns
            days.forEach { day ->
                item(span = { GridItemSpan(1) }) { // Each day's column
                    DayColumn(
                        day = day,
                        events = scheduleViewModel.eventsByDay[day] ?: emptyList(),
                        viewModel = scheduleViewModel
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
    viewModel: ScheduleViewModel
    /* Add drag and drop related parameters here */
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(4.dp)
            .border(1.dp, Color.LightGray)
        /* Add dragAndDropTarget modifier here for the column */
    ) {
        // Display events for this day
        events.forEach { event ->
            EventItem(event = event, viewModel = viewModel /*, add dragAndDropSource modifier here */)
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
    viewModel: ScheduleViewModel
    /* Add dragAndDropSource modifier here */
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            /* Add .dragAndDropSource modifier here for the event item */
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