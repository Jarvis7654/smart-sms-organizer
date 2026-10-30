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
package dev.octoshrimpy.quik.common.util

import android.content.Context
import dev.octoshrimpy.quik.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Formats reminder due dates the way SMS Organizer does: "Today, 5:30 PM". */
object ReminderFormatter {

    fun format(context: Context, millis: Long): String {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val dayDiff = ((cal.timeInMillis - today) / (24 * 60 * 60 * 1000L)).toInt()

        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
        return when {
            dayDiff == 0 -> "${context.getString(R.string.upcoming_today)}, $time"
            dayDiff == 1 -> "${context.getString(R.string.upcoming_tomorrow)}, $time"
            dayDiff == -1 -> "${context.getString(R.string.upcoming_yesterday)}, $time"
            else -> {
                val sameYear =
                    cal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
                val date = SimpleDateFormat(
                    if (sameYear) "d MMM" else "d MMM yyyy",
                    Locale.getDefault()
                ).format(cal.time)
                "$date, $time"
            }
        }
    }
}
