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
package dev.octoshrimpy.quik.model

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

/**
 * A reminder extracted from an SMS (bill due, journey, appointment...) or created by the
 * user. Powers the "Upcoming" tab, in the spirit of SMS Organizer's reminder cards.
 */
open class Reminder(
    @PrimaryKey var id: Long = 0,
    /** Reminder type, one of ReminderExtractor.TYPE_* */
    var type: String = TYPE_OTHER,
    var title: String = "",
    var subtitle: String = "",
    /** Formatted amount, e.g. "₹1,250" or "Rs 499" */
    var amount: String = "",
    /** Epoch millis of the event; 0 when unknown */
    @Index var dueDate: Long = 0,
    var threadId: Long = 0,
    var messageId: Long = 0,
    @Index var dismissed: Boolean = false,
    /** True once the due-soon notification has been posted */
    var notified: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
) : RealmObject() {

    companion object {
        const val TYPE_BILL = "BILL"
        const val TYPE_TRAIN = "TRAIN"
        const val TYPE_FLIGHT = "FLIGHT"
        const val TYPE_BUS = "BUS"
        const val TYPE_MOVIE = "MOVIE"
        const val TYPE_APPOINTMENT = "APPOINTMENT"
        const val TYPE_EVENT = "EVENT"
        const val TYPE_ORDER = "ORDER"
        const val TYPE_OTHER = "OTHER"
    }
}
