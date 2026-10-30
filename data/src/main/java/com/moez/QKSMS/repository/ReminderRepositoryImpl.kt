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

import dev.octoshrimpy.quik.model.Message
import dev.octoshrimpy.quik.model.Reminder
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor() : ReminderRepository {

    override fun getReminders(): RealmResults<Reminder> =
        Realm.getDefaultInstance()
            .where(Reminder::class.java)
            .equalTo("dismissed", false)
            .sort("dueDate", Sort.ASCENDING)
            .findAll()

    override fun getDueSoon(withinMillis: Long): List<Reminder> {
        val deadline = System.currentTimeMillis() + withinMillis
        return Realm.getDefaultInstance().use { realm ->
            realm.copyFromRealm(
                realm.where(Reminder::class.java)
                    .equalTo("dismissed", false)
                    .equalTo("notified", false)
                    .greaterThan("dueDate", System.currentTimeMillis() - 60 * 60 * 1000L)
                    .lessThanOrEqualTo("dueDate", deadline)
                    .findAll()
            )
        }
    }

    override fun getReminder(id: Long): Reminder? =
        Realm.getDefaultInstance().use { realm ->
            realm.where(Reminder::class.java)
                .equalTo("id", id)
                .findFirst()
                ?.let(realm::copyFromRealm)
        }

    override fun insertOrUpdate(reminder: Reminder) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm.copyToRealmOrUpdate(reminder)
            }
        }
    }

    override fun setDismissed(id: Long, dismissed: Boolean) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm.where(Reminder::class.java)
                    .equalTo("id", id)
                    .findFirst()?.dismissed = dismissed
            }
        }
    }

    override fun setNotified(id: Long, notified: Boolean) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm.where(Reminder::class.java)
                    .equalTo("id", id)
                    .findFirst()?.notified = notified
            }
        }
    }

    override fun cleanup() {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                val reminders = realm.where(Reminder::class.java).findAll()
                val stale = reminders.filter { reminder ->
                    realm.where(Message::class.java)
                        .equalTo("id", reminder.messageId)
                        .count() == 0L
                }
                stale.forEach { it.deleteFromRealm() }
            }
        }
    }
}
