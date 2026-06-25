package com.gntr.framework.calendar

import com.gntr.domain.calendar.ICalendarSyncService
import com.gntr.domain.calendar.SyncError
import com.gntr.domain.calendar.SyncFailure
import com.gntr.domain.calendar.SyncResult
import com.gntr.domain.calendar.SyncSuccess
import com.gntr.domain.model.CalendarEvent
import com.gntr.domain.model.CalendarSource
import com.gntr.domain.model.EventTime
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.CalendarList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

class GoogleCalendarServiceImpl @Inject constructor(
    private val transport: HttpTransport,
    private val jsonFactory: JsonFactory
) : ICalendarSyncService {

    override suspend fun fetchAvailableCalendars(accessToken: String): SyncResult<List<CalendarSource>> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleCredential().setAccessToken(accessToken)
            val service = Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName("ProfessionalSleeper")
                .build()

            val remoteSources = mutableListOf<CalendarSource>()
            var pageToken: String? = null

            do {
                val calendarList: CalendarList = service.calendarList().list()
                    .setPageToken(pageToken)
                    .execute()

                val items = calendarList.items.mapNotNull { entry ->
                    if (entry.id == null || entry.summary == null) return@mapNotNull null
                    CalendarSource(
                        id = entry.id,
                        displayName = entry.summaryOverride ?: entry.summary,
                        colorHex = entry.backgroundColor ?: "#5C6370",
                        isEnabled = true
                    )
                }
                remoteSources.addAll(items)
                pageToken = calendarList.nextPageToken
            } while (pageToken != null)

            SyncSuccess(remoteSources)
        } catch (e: Exception) {
            if (e is GoogleJsonResponseException && (e.statusCode == 401 || e.statusCode == 403)) {
                SyncFailure(SyncError.PermanentAuthFailure(e.statusCode, e.message ?: "Revoked"))
            } else {
                SyncFailure(SyncError.Transient(e))
            }
        }
    }

    override suspend fun fetchUpcomingEvents(
        accessToken: String,
        sourceId: String,
        timeMin: Long,
        timeMax: Long
    ): SyncResult<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Initiating Google Calendar sync for sourceId: $sourceId")

            val credential = GoogleCredential().setAccessToken(accessToken)

            val service = Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName("ProfessionalSleeper")
                .build()

            val events = service.events().list(sourceId)
                .setTimeMin(DateTime(timeMin))
                .setTimeMax(DateTime(timeMax))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()

            val mappedEvents = events.items.mapNotNull { event ->
                if (event.id == null) return@mapNotNull null

                val startTime = if (event.start?.date != null) {
                    EventTime.AllDay(LocalDate.parse(event.start.date.toStringRfc3339()))
                } else {
                    EventTime.Exact(event.start.dateTime.value)
                }

                val endTime = if (event.end?.date != null) {
                    EventTime.AllDay(LocalDate.parse(event.end.date.toStringRfc3339()))
                } else {
                    EventTime.Exact(event.end.dateTime.value)
                }

                CalendarEvent(
                    id = event.id,
                    sourceId = sourceId,
                    title = event.summary ?: "Busy",
                    startTime = startTime,
                    endTime = endTime
                )
            }

            Timber.i("Calendar sync successful for $sourceId. Parsed ${mappedEvents.size} upcoming events.")
            SyncSuccess(mappedEvents)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch or parse Google Calendar events for $sourceId")
            if (e is GoogleJsonResponseException && (e.statusCode == 401 || e.statusCode == 403)) {
                SyncFailure(SyncError.PermanentAuthFailure(e.statusCode, e.message ?: "Revoked"))
            } else {
                SyncFailure(SyncError.Transient(e))
            }
        }
    }
}