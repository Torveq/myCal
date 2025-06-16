@file:OptIn(ExperimentalFoundationApi::class)

package com.approid.mycal

import android.content.ClipData
import android.content.ClipDescription
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

// wanted to add a drag and drop kanban style schedule with intra column drag and drop but some logistic issues and cba to reinvent the wheel(how to adjust time when reordered intra column)

// Data class to represent a single schedule event
@Serializable
data class ScheduleEvent(
    val id: String = UUID.randomUUID().toString(), // Unique ID for each event(created by default)
    var title: String,

    @SerialName("start_time")
    var startTime: String,
    @SerialName("end_time")
    var endTime: String,

    @Serializable(with = DayOfWeekSerializer::class) // custom serializer for this specific property(casting to enum safety with default MONDAY value)
    var day: DayOfWeek,

    var location: String? = null, // Optional location
    var notes: String? = null // Optional notes
)

fun parseScheduleEvents(jsonString: String): List<ScheduleEvent> {
    val json = Json { ignoreUnknownKeys = true }
    return try {
        json.decodeFromString<List<ScheduleEvent>>(jsonString)
    } catch (e: Exception) {
        println("Error parsing JSON: ${e.message}")
        emptyList()
    }
}

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

/**
 * A custom serializer for the DayOfWeek enum.
 * This object provides custom logic for converting a string to a DayOfWeek enum,
 * including a fallback mechanism for invalid values.
 */
object DayOfWeekSerializer : KSerializer<DayOfWeek> {
    // Describes the type being serialized (a String in this case) to the library.
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DayOfWeek", PrimitiveKind.STRING)

    /**
     * This function is called when converting an enum object TO a JSON string.
     * It simply takes the enum's name and encodes it as a string.
     */
    override fun serialize(encoder: Encoder, value: DayOfWeek) {
        encoder.encodeString(value.name)
    }

    /**
     * This function is called when converting a JSON string TO an enum object.
     * This is where we implement the custom fallback logic.
     */
    override fun deserialize(decoder: Decoder): DayOfWeek {
        val dayString = decoder.decodeString()
        return try {
            // We try to convert the string to an enum, converting it to uppercase
            // to be more flexible (e.g., "tuesday" will work).
            DayOfWeek.valueOf(dayString.uppercase())
        } catch (e: IllegalArgumentException) {
            // If valueOf() fails because the string is not a valid enum name,
            // we catch the exception and return our desired default value.
            println("Warning: Invalid day '$dayString' found in JSON. Defaulting to MONDAY.")
            DayOfWeek.MONDAY
        }
    }
}


// ViewModel to hold and manage the schedule state
class ScheduleViewModel : ViewModel() {
    // A list of events, wrapped in mutableStateOf to trigger recomposition on changes
    var events by mutableStateOf<List<ScheduleEvent>>(emptyList())
        private set  //makes the setter of this private

    // Map to group events by day for easier display
    val eventsByDay: Map<DayOfWeek, List<ScheduleEvent>>
        get() = events.groupBy { it.day }.mapValues { (_, dayEvents) ->
            // Events within each day are now sorted by their start time.
            dayEvents.sortedBy { parseTime(it.startTime) }   //TODO: use smth similar to comparator in java for other sort order if start time is the same(end time comparison)
        }

    fun addEvent(event: ScheduleEvent) {
        events = events + event
    }

    fun deleteEvent(eventId: String) {
        events = events.filterNot { it.id == eventId }
    }

    fun updateEvent(updatedEvent: ScheduleEvent) {
        events = events.map { if (it.id == updatedEvent.id) updatedEvent else it }
    }

    // This function now handles moving an event to a specific day
    fun moveEvent(draggedId: String, targetDay: DayOfWeek, targetIndex: Int) {
        val currentList = events.toMutableList()

        // Find the event being dragged and remove it from its original position.
        val draggedEvent = currentList.find { it.id == draggedId } ?: return
        currentList.remove(draggedEvent)

        // All events for the target day, in their current order.
        val targetDayEvents = currentList.filter { it.day == targetDay }.toMutableList()
        // Ensure the target index is within the valid bounds.
        val insertionIndex = targetIndex.coerceIn(0, targetDayEvents.size)
        // Add the dragged event to its new position in the target day's list.
        targetDayEvents.add(insertionIndex, draggedEvent.copy(day = targetDay))

        // Get all events from other days.
        val otherEvents = currentList.filter { it.day != targetDay }

        // Reconstruct the master list and update the state.
        // We sort by day enum order to maintain the column structure.
        events = (otherEvents + targetDayEvents).sortedBy { it.day.ordinal }
    }

    //init {populateSampleData()}
    // Function to add some sample data for preview
    private fun populateSampleData() {
        if (events.isEmpty()) { // Only add if empty to avoid duplication on recomposition
            addEvent(ScheduleEvent(title = "Team Meeting", startTime = "10:00 AM", endTime = "11:00 AM", location = "Room A", day = DayOfWeek.MONDAY))
            addEvent(ScheduleEvent(title = "Lunch with Client", startTime = "1:00 PM", endTime = "2:00 PM", location = "Cafe Central", day = DayOfWeek.MONDAY))
            addEvent(ScheduleEvent(title = "Project Sync", startTime = "3:00 PM", endTime = "4:00 PM", location = "Online", day = DayOfWeek.WEDNESDAY))
            addEvent(ScheduleEvent(title = "Gym Session", startTime = "6:00 PM", endTime = "7:00 PM", location = "City Fitness", day = DayOfWeek.THURSDAY))
            addEvent(ScheduleEvent(title = "Weekend Prep", startTime = "4:00 PM", endTime = "5:00 PM", location = "Home", day = DayOfWeek.FRIDAY))
            addEvent(ScheduleEvent(title = "Grocery Shopping", startTime = "11:00 AM", endTime = "12:00 PM", location = "Supermarket", day = DayOfWeek.SATURDAY))
        }
    }
}

// The following WeeklyScheduleScreen  and DayHeaderRow would replace the uncommented WeeklyScheduleScreen, ScheduleList, ScheduleGrid, DayHeader, and DayHeaderRow to implement original only landscape optimised schedule screen

/*@Composable
fun WeeklyScheduleScreen(scheduleViewModel: ScheduleViewModel = viewModel()) {
    // CHANGE: State to track the ID of the event currently being dragged.
    // When an event is dragged, its ID is stored here. When the drag ends, it's set back to null.
    // This state is what drives the AnimatedVisibility.
    var draggedEventId by remember { mutableStateOf<String?>(null) }
    // When this is not null, the Edit Dialog will be shown.
    var eventToEdit by remember { mutableStateOf<ScheduleEvent?>(null) }
    val days = DayOfWeek.values()

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp), // Padding around the entire schedule
        contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp, // Adds a subtle shadow
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
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
                                onDragEnd = { draggedEventId = null },
                                onEventEdit = { event -> eventToEdit = event }
                            )
                        }
                    }
                }
            }
            // The `let` scope ensures the dialog is only composed when there's an event to edit.
            eventToEdit?.let { event ->
                EditEventDialog(
                    eventToEdit = event,
                    viewModel = scheduleViewModel,
                    onDismiss = { eventToEdit = null } // Set back to null to hide the dialog
                )
            }
        }
    }
}*/

/*@Composable
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
}*/

@Composable
fun WeeklyScheduleScreen(scheduleViewModel: ScheduleViewModel = viewModel()) {
    var draggedEventId by remember { mutableStateOf<String?>(null) }
    var eventToEdit by remember { mutableStateOf<ScheduleEvent?>(null) }

    var showFullScreenImage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imgFile = remember { File(context.filesDir, "scan.jpeg") }

    // This is the root Box
    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .fillMaxHeight(0.82f)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isPortrait = maxWidth < 600.dp // A common breakpoint for phones

            // Conditionally display List or Grid based on orientation ---
            if (isPortrait) {
                ScheduleList(
                    scheduleViewModel = scheduleViewModel,
                    draggedEventId = draggedEventId,
                    onDragStart = { draggedEventId = it },
                    onDragEnd = { draggedEventId = null },
                    onEventEdit = { eventToEdit = it }
                )
            } else {
                ScheduleGrid(
                    scheduleViewModel = scheduleViewModel,
                    draggedEventId = draggedEventId,
                    onDragStart = { draggedEventId = it },
                    onDragEnd = { draggedEventId = null },
                    onEventEdit = { eventToEdit = it }
                )
            }

            eventToEdit?.let { event ->
                EditEventDialog(
                    eventToEdit = event,
                    viewModel = scheduleViewModel,
                    onDismiss = { eventToEdit = null },
                    isPortrait = isPortrait
                )
            }


            // A Row to hold the thumbnail and FAB at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter) // Aligns the whole row to the bottom
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, // Pushes children to the ends
                verticalAlignment = Alignment.CenterVertically // Aligns items vertically in the middle
            ) {
                // 1. Thumbnail on the left
                if (imgFile.exists()) {
                    ClickableImageThumbnail(
                        file = imgFile,
                        onClick = { showFullScreenImage = true }
                    )
                } else {
                    // Spacer to hold the space if the image doesn't exist,
                    // keeping the FAB on the right.
                    Text(text = "File exists: ${imgFile.exists()}", color = Color.Red)
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // 2. FAB on the right
                FloatingActionButton(
                    onClick = {
                        // Create a new blank event to signal "add mode"
                        val now = LocalTime.now()
                        eventToEdit = ScheduleEvent(
                            title = "",
                            startTime = now.format(timeFormatter),
                            endTime = now.plusHours(1).format(timeFormatter),
                            day = DayOfWeek.values()[java.time.LocalDate.now().dayOfWeek.value - 1] // Default to today
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Event")
                }
            }
        }

        Button(
            onClick = {},
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(horizontal = 15.dp, vertical = 15.dp),
            content = { Text("Convert") }
        )
    }
    if (showFullScreenImage) {
        FullScreenImageDialog(
            file = imgFile,
            onDismiss = { showFullScreenImage = false }
        )
    }
}

// A Composable for the Portrait (List) layout ---
@Composable
fun ScheduleList(
    scheduleViewModel: ScheduleViewModel,
    draggedEventId: String?,
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit,
    onEventEdit: (ScheduleEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val eventsByDay = scheduleViewModel.eventsByDay
        DayOfWeek.values().forEach { day ->
            val eventsForDay = eventsByDay[day]
            if (!eventsForDay.isNullOrEmpty()) {
                // Sticky header for the day
                stickyHeader {
                    DayHeader(day = day)
                }
                // Items for that day
                items(eventsForDay, key = { it.id }) { event ->
                    AnimatedVisibility(
                        visible = draggedEventId != event.id,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        EventItem(
                            event = event,
                            viewModel = scheduleViewModel,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onEdit = onEventEdit,
                            index = -1, // Placeholder
                            isPortrait = true
                        )
                    }
                }
            }
        }
    }
}

// A Composable for the Landscape (Grid) layout ---
@Composable
fun ScheduleGrid(
    scheduleViewModel: ScheduleViewModel,
    draggedEventId: String?,
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit,
    onEventEdit: (ScheduleEvent) -> Unit
) {
    val days = DayOfWeek.values()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
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
                                draggedEventId = draggedEventId,
                                onDragStart = onDragStart,
                                onDragEnd = onDragEnd,
                                onEventEdit = onEventEdit
                            )
                        }
                    }
                }
            }
        }
    }
}

// A specific header for the List view with a background
@Composable
fun DayHeader(day: DayOfWeek) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp // Adds a subtle lift
    ) {
        Text(
            text = day.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}


@Composable
fun DayHeaderRow(days: Array<DayOfWeek>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { day ->
            Text(
                text = day.name.take(3),
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
fun DayColumn(
    day: DayOfWeek,
    events: List<ScheduleEvent>,
    viewModel: ScheduleViewModel,
    // Accept the state and callbacks from the parent.
    draggedEventId: String?,
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit,
    onEventEdit: (ScheduleEvent) -> Unit
) {
    // CHANGE: State to provide visual feedback when an item is being dragged over this column.
    var isDropTarget by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            // EMPTY COLUMN FIX: The .defaultMinSize modifier ensures that the Column has a
            // minimum height even when it contains no events
            .defaultMinSize(minHeight = 150.dp)
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
                                // REORDERING CHANGE: When dropping on the column (not an item),
                                // move the event to the end of this day's list.
                                viewModel.moveEvent(
                                    draggedId = draggedId,
                                    targetDay = day,
                                    targetIndex = events.size
                                )
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
        events.forEachIndexed { index, event ->
            // STALE STATE FIX: We wrap the item in a `key` composable.
            // This explicitly tells Compose that the content inside is tied to the unique
            // `event.id`. This prevents the "stale lambda" bug where a reused composable
            // might reference old data in its gesture listeners.
            key(event.id) {
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
                        index = index,
                        // CHANGE: Pass the callbacks down to the EventItem.
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onEdit = onEventEdit,
                        isPortrait = false
                    )
                }
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
    index: Int, // REORDERING CHANGE: Accept the item's index in the list.
    // CHANGE: Accept callbacks to notify the parent about drag state changes.
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit,
    onEdit: (ScheduleEvent) -> Unit,
    isPortrait: Boolean
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    // RIPPLE EFFECT: 1. Create a MutableInteractionSource.
    // This object will allow us to programmatically control the visual state (like pressed) of the Card.
    val interactionSource = remember { MutableInteractionSource() }
    // REORDERING CHANGE: State to show visual feedback when another item is dragged over this one.
    var isBeingDraggedOver by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = { showOptionsMenu = true }
            )
            // RIPPLE EFFECT: 2. Apply the .indication modifier.
            // This modifier doesn't detect gestures itself; it just *draws* the ripple
            // whenever the `interactionSource` receives a press event.
            .indication(interactionSource = interactionSource, indication = rememberRipple())
            .dragAndDropSource {
                // We use a pointerInput scope implicitly(we are in dndsource scope which is in pointerInput scope) to detect the long press gesture that will start the drag.
                if (!isPortrait) {
                    detectTapGestures(
                        // RIPPLE EFFECT: 3. Implement the onPress block.
                        onPress = { offset ->
                            // As soon as a finger touches down, create a 'Press' interaction.
                            val press = PressInteraction.Press(offset)
                            // Emit the press to our interactionSource, which tells the .indication modifier to draw the ripple.
                            interactionSource.emit(press)

                            try {
                                // This waits for the user to lift their finger or for the gesture to be cancelled.
                                awaitRelease()
                            } finally {
                                // Once the press is over, emit a 'Release' event to remove the ripple.
                                interactionSource.emit(PressInteraction.Release(press))
                            }
                        },
                        // A simple, single tap will now trigger the options menu.
                        onTap = {
                            showOptionsMenu = true
                        },
                        onLongPress = {
                            android.util.Log.d(
                                "DragDropDebug",
                                "Long Press DETECTED on item: ${event.title}"
                            )
                            // CHANGE: When a long press is detected, we build and start the data transfer.
                            // The try/finally block is crucial. The 'finally' block ensures that
                            // onDragEnd() is always called, even if the drag is cancelled,
                            // making sure the original item reappears correctly.
                            try {
                                // First, we notify the parent that a drag has started.
                                // This sets the 'draggedEventId' and hides this original item via AnimatedVisibility.
                                onDragStart(event.id)

                                android.util.Log.d(
                                    "DragDropDebug",
                                    "onDragStart CALLED for item: ${event.title}"
                                )

                                // Then, we start the actual system-level drag operation.
                                startTransfer(
                                    DragAndDropTransferData(
                                        // We send the event's ID as plain text.
                                        // We also give it a custom MIME type so our drop target can identify it.
                                        clipData = ClipData(
                                            ClipDescription(
                                                "Schedule Event",
                                                arrayOf("application/vnd.mycal.event")
                                            ),
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
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp) // Modern, roundy corners for the event item.
    ) {
        Column(modifier = Modifier
            .padding(12.dp)
            .defaultMinSize(minHeight = 130.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = event.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${event.startTime} - ${event.endTime}", style = MaterialTheme.typography.bodyLarge)
            event.location?.let {
                if (it.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "At: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Options Menu (DropdownMenu)
            DropdownMenu(
                expanded = showOptionsMenu,
                onDismissRequest = { showOptionsMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        onEdit(event)
                        showOptionsMenu = false
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