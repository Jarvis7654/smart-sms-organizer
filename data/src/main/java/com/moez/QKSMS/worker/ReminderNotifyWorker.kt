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
package dev.octoshrimpy.quik.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.octoshrimpy.quik.manager.NotificationManager
import dev.octoshrimpy.quik.repository.ReminderRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Posts a heads-up notification for reminders that come due within the next 12 hours
 * (SMS Organizer's "You have an upcoming event" behaviour). Runs a few times a day.
 */
class ReminderNotifyWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "ReminderNotifyWorker"

        fun schedule() {
            WorkManager.getInstance().enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderNotifyWorker>(6, TimeUnit.HOURS).build()
            )
        }
    }

    @Inject lateinit var reminderRepo: ReminderRepository
    @Inject lateinit var notificationManager: NotificationManager

    override fun doWork(): Result {
        val due = reminderRepo.getDueSoon(TimeUnit.HOURS.toMillis(12))
        if (due.isEmpty()) return Result.success()

        notificationManager.notifyReminders(due)
        return Result.success()
    }
}
