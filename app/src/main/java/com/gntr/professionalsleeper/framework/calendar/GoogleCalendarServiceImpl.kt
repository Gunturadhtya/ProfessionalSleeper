package com.gntr.professionalsleeper.framework.calendar

import android.content.Context
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.gntr.professionalsleeper.domain.calendar.CalendarEvent
import com.gntr.professionalsleeper.domain.calendar.ICalendarSyncService
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GoogleCalendarServiceImpl(
    private val context: Context
) : ICalendarSyncService {

    private val transport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    override suspend fun fetchUpcomingEvents(
        accessToken: String,
        timeMin: Long,
        timeMax: Long
    ): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Initiating Google Calendar sync using granted access token.")

            val credential = GoogleCredential().setAccessToken(accessToken)

            val service = Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName("ProfessionalSleeper")
                .build()

            val events = service.events().list("primary")
                .setTimeMin(DateTime(timeMin))
                .setTimeMax(DateTime(timeMax))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()

            val mappedEvents = events.items.mapNotNull { event ->
                val start = event.start.dateTime?.value ?: event.start.date?.value ?: return@mapNotNull null
                val end = event.end.dateTime?.value ?: event.end.date?.value ?: return@mapNotNull null

                CalendarEvent(
                    id = event.id,
                    title = event.summary ?: "Busy",
                    startTime = start,
                    endTime = end
                )
            }

            Timber.i("Calendar sync successful. Parsed ${mappedEvents.size} upcoming events.")
            Result.success(mappedEvents)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch or parse Google Calendar events")
            Result.failure(e)
        }
    }

    override fun checkScheduleConflict(napStartTime: Long, napEndTime: Long, events: List<CalendarEvent>): Boolean {
        return events.any { event ->
            napStartTime < event.endTime && napEndTime > event.startTime
        }
    }
}