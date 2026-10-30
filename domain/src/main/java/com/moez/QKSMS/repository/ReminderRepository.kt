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
package dev.octoshrimpy.quik.repository

import dev.octoshrimpy.quik.model.Reminder
import io.realm.RealmResults

interface ReminderRepository {

    /** Active (not dismissed) reminders sorted by due date, fed to the Upcoming tab */
    fun getReminders(): RealmResults<Reminder>

    /** Reminders due within the next [withinMillis] that haven't been notified */
    fun getDueSoon(withinMillis: Long): List<Reminder>

    fun getReminder(id: Long): Reminder?

    fun insertOrUpdate(reminder: Reminder)

    fun setDismissed(id: Long, dismissed: Boolean)

    fun setNotified(id: Long, notified: Boolean)

    /** Removes reminders that point at deleted messages/threads */
    fun cleanup()
}
