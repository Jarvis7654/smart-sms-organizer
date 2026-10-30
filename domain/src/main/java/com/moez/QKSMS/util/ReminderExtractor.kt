/*
 * Copyright (C) 2026 Smart SMS Organizer contributors
 *
 * This file is part of Smart SMS Organizer, based on QUIK/QKSMS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.util

import dev.octoshrimpy.quik.model.Reminder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * Extracts SMS Organizer style reminders (bills due, journeys, appointments, orders...)
 * from message text. Purely heuristic and fully on-device: the parser looks for a known
 * event type, then tries to find an amount and a date/time to attach to it.
 */
object ReminderExtractor {

    data class Extracted(
        val type: String,
        val title: String,
        val subtitle: String,
        val amount: String,
        val dueDate: Long,
    )

    // --- type detection ---------------------------------------------------------------

    private val TRAVEL_TRAIN = Pattern.compile(
        "(?i)\\b(pnr|train|irctc|rail|berth|coach|seat no|wagon)\\b")
    private val TRAVEL_FLIGHT = Pattern.compile(
        "(?i)\\b(flight|airline|boarding pass|departure gate|pnr.*(flight)|indigo|vistara|air india|spicejet|goair|akasa)\\b")
    private val TRAVEL_BUS = Pattern.compile(
        "(?i)\\b(bus|redbus|volvo|boarding point|sleeper)\\b")
    private val MOVIE = Pattern.compile(
        "(?i)\\b(movie|cinema|bookmyshow|pvr|INOX|seats? (are|is)|screen \\d|show ?time|matinee)\\b")
    private val APPOINTMENT = Pattern.compile(
        "(?i)\\b(appointment|doctor|clinic|hospital|consultation|dental|opd|visit is scheduled)\\b")
    private val BILL = Pattern.compile(
        "(?i)\\b(bill|invoice|statement|recharge|prepaid|postpaid|broadband|electricity|gas|water bill|dth|emi|instal?lment|premium|minimum due|total (amount )?due|payment due|due date|due on|overdue)\\b")
    private val ORDER = Pattern.compile(
        "(?i)\\b(order|shipment|dispatched|shipped|out for delivery|expected delivery|arriving (on|today|tomorrow)|delivery (date|by|expected))\\b")
    private val EVENT = Pattern.compile(
        "(?i)\\b(event|invitation|invite you|rsvp|seminar|webinar|meetup|meeting is|conference)\\b")

    // --- amounts ----------------------------------------------------------------------

    private val AMOUNT = Pattern.compile(
        "(?i)(?:rs\\.?|inr|₹|usd ?|\\$|€)\\s?[0-9]{1,3}(?:[, ][0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]{1,3}(?:,[0-9]{2,3})+(?:\\.[0-9]{2})?\\s?(?:rs\\.?|inr|₹)")

    // --- dates ------------------------------------------------------------------------

    private val MONTHS = mapOf(
        "jan" to 0, "feb" to 1, "mar" to 2, "apr" to 3, "may" to 4, "jun" to 5,
        "jul" to 6, "aug" to 7, "sep" to 8, "sept" to 8, "oct" to 9, "nov" to 10, "dec" to 11)

    private val DATE_DMY_TEXT = Pattern.compile(
        "(?i)\\b(\\d{1,2})(?:st|nd|rd|th)?[ -](jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*(?:[ -,]+(\\d{4}))?\\b")
    private val DATE_MDY_TEXT = Pattern.compile(
        "(?i)\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*[ .-]+(\\d{1,2})(?:st|nd|rd|th)?(?:[ ,]+(\\d{4}))?\\b")
    private val DATE_NUMERIC = Pattern.compile(
        "(?i)\\b(\\d{1,2})[/\\-.](\\d{1,2})(?:[/\\-.](\\d{2,4}))?\\b")
    private val DATE_WORDS = Pattern.compile(
        "(?i)\\b(today|tonight|tomorrow|day after tomorrow)\\b")
    private val TIME = Pattern.compile(
        "(?i)\\b(\\d{1,2})[:.](\\d{2})\\s?(am|pm)?\\b|(?:\\bat |on )?(\\d{1,2})\\s?(am|pm)\\b")

    // --- due date context -------------------------------------------------------------

    private val DUE_CONTEXT = Pattern.compile(
        "(?i)(?:due (?:on|by|date)|payment (?:due|by)|last date|pay by|before|valid (?:till|until)|on |at |departure|departs|boarding|show ?time|arriving|check ?in)")

    /** Represents the message text [body] into a reminder, or null when nothing sensible is found. */
    fun extract(body: String, address: String, now: Long): Extracted? {
        if (body.length < 12) return null
        val lower = body.lowercase(Locale.getDefault())

        val type = detectType(lower) ?: return null

        val amount = AMOUNT.matcher(body).let { m ->
            if (m.find()) m.group().trim() else ""
        }

        val dueDate = parseDueDate(body, now) ?: return when (type) {
            // travel/movie/appointment without a date aren't useful as reminders
            Reminder.TYPE_TRAIN, Reminder.TYPE_FLIGHT, Reminder.TYPE_BUS,
            Reminder.TYPE_MOVIE, Reminder.TYPE_APPOINTMENT -> null
            // bills still show even without a parsed date (amount + sender are useful)
            else -> Extracted(type, titleFor(type), address, amount, 0L)
        }

        // only future-facing reminders are useful
        val earliest = now - 24 * 60 * 60 * 1000L
        if (dueDate < earliest) return null
        // ignore absurd dates (> 2 years out): usually parse errors
        if (dueDate > now + 730L * 24 * 60 * 60 * 1000L) return null

        return Extracted(type, titleFor(type), address, amount, dueDate)
    }

    private fun detectType(lower: String): String? = when {
        TRAVEL_TRAIN.matcher(lower).find() -> Reminder.TYPE_TRAIN
        TRAVEL_FLIGHT.matcher(lower).find() -> Reminder.TYPE_FLIGHT
        TRAVEL_BUS.matcher(lower).find() -> Reminder.TYPE_BUS
        MOVIE.matcher(lower).find() -> Reminder.TYPE_MOVIE
        APPOINTMENT.matcher(lower).find() -> Reminder.TYPE_APPOINTMENT
        ORDER.matcher(lower).find() -> Reminder.TYPE_ORDER
        EVENT.matcher(lower).find() -> Reminder.TYPE_EVENT
        BILL.matcher(lower).find() -> Reminder.TYPE_BILL
        else -> null
    }

    private fun titleFor(type: String): String = when (type) {
        Reminder.TYPE_BILL -> "Bill due"
        Reminder.TYPE_TRAIN -> "Train journey"
        Reminder.TYPE_FLIGHT -> "Flight"
        Reminder.TYPE_BUS -> "Bus trip"
        Reminder.TYPE_MOVIE -> "Movie"
        Reminder.TYPE_APPOINTMENT -> "Appointment"
        Reminder.TYPE_EVENT -> "Event"
        Reminder.TYPE_ORDER -> "Delivery expected"
        else -> "Reminder"
    }

    /**
     * Finds the most likely event date+time in [body]. Prefers dates that appear near a
     * due/departure/showtime keyword, then the first parseable date in the text.
     */
    fun parseDueDate(body: String, now: Long): Long? {
        var best: Long? = null

        // Strategy 1: a date within ~40 chars after a due-context keyword
        val contextMatcher = DUE_CONTEXT.matcher(body)
        while (contextMatcher.find()) {
            val window = body.substring(
                contextMatcher.start(),
                minOf(body.length, contextMatcher.end() + 48))
            parseDateIn(window, now)?.let { date ->
                if (best == null) best = date
            }
            if (best != null) break
        }

        // Strategy 2: word dates ("today"/"tomorrow"/"tonight") anywhere
        if (best == null) {
            val wordMatcher = DATE_WORDS.matcher(body)
            if (wordMatcher.find()) best = parseWordDate(wordMatcher.group(1), now)
                .also { it?.let { d -> attachWordTime(body, d)?.let { t -> best = t } } }
        }

        // Strategy 3: first date anywhere in the message
        if (best == null) best = parseDateIn(body, now)

        return best
    }

    private fun parseDateIn(text: String, now: Long): Long? {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val year = cal.get(Calendar.YEAR)

        DATE_DMY_TEXT.matcher(text).let { m ->
            if (m.find()) {
                val day = m.group(1).toIntOrNull() ?: return@let
                val month = MONTHS[m.group(2).lowercase(Locale.getDefault())] ?: return@let
                val parsedYear = m.group(3)?.toIntOrNull() ?: year
                cal.set(if (parsedYear < 100) parsedYear + 2000 else parsedYear, month,
                    day.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                return attachTime(text, cal)
            }
        }

        DATE_MDY_TEXT.matcher(text).let { m ->
            if (m.find()) {
                val month = MONTHS[m.group(1).lowercase(Locale.getDefault())] ?: return@let
                val day = m.group(2).toIntOrNull() ?: return@let
                val parsedYear = m.group(3)?.toIntOrNull() ?: year
                cal.set(if (parsedYear < 100) parsedYear + 2000 else parsedYear, month,
                    day.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                return attachTime(text, cal)
            }
        }

        DATE_NUMERIC.matcher(text).let { m ->
            if (m.find()) {
                val first = m.group(1).toIntOrNull() ?: return@let
                val second = m.group(2).toIntOrNull() ?: return@let
                var parsedYear = m.group(3)?.toIntOrNull() ?: year
                if (parsedYear < 100) parsedYear += 2000

                // dd/mm assumed; fall back to mm/dd when first part isn't a valid day
                val (day, month) = if (first in 1..31 && second in 1..12) first to second - 1
                else if (second in 1..31 && first in 1..12) second to first - 1
                else return@let

                cal.set(parsedYear, month, day.coerceIn(1, cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                return attachTime(text, cal)
            }
        }

        return null
    }

    private fun parseWordDate(word: String, now: Long): Long? {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        when (word.lowercase(Locale.getDefault())) {
            "today", "tonight" -> {
                if (word.equals("tonight", true)) cal.set(Calendar.HOUR_OF_DAY, 20)
            }
            "tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "day after tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 2)
            else -> return null
        }
        return cal.timeInMillis
    }

    /** Finds a time in [text] and merges it into [cal]; returns millis or null. */
    private fun attachTime(text: String, cal: Calendar): Long? {
        TIME.matcher(text).let { m ->
            if (m.find()) {
                val hour12 = m.group(1)?.toIntOrNull()
                val minute = m.group(2)?.toIntOrNull()
                val meridiem = m.group(3)
                val hourOnly = m.group(4)?.toIntOrNull()
                val meridiemOnly = m.group(5)

                when {
                    hour12 != null -> {
                        var hour = hour12.coerceIn(1, 12)
                        if (meridiem.equals("pm", true) && hour != 12) hour += 12
                        if (meridiem.equals("am", true) && hour == 12) hour = 0
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute ?: 0)
                    }
                    hourOnly != null -> {
                        var hour = hourOnly.coerceIn(1, 12)
                        if (meridiemOnly.equals("pm", true) && hour != 12) hour += 12
                        if (meridiemOnly.equals("am", true) && hour == 12) hour = 0
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, 0)
                    }
                }
            }
        }
        return cal.timeInMillis
    }

    /** "today/tomorrow" + a time elsewhere in the sentence */
    private fun attachWordTime(text: String, date: Long): Long? {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        return attachTime(text, cal)
    }
}
