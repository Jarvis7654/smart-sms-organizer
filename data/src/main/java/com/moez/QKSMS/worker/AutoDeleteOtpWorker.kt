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
import dev.octoshrimpy.quik.model.Conversation
import dev.octoshrimpy.quik.repository.ConversationRepository
import dev.octoshrimpy.quik.repository.MessageRepository
import dev.octoshrimpy.quik.util.MessageClassifier
import dev.octoshrimpy.quik.util.Preferences
import io.realm.Realm
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Deletes OTP-category messages that are older than 24 hours, like the SMS Organizer
 * "auto-delete OTPs" feature. Runs once per day; exits immediately when disabled.
 */
class AutoDeleteOtpWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "AutoDeleteOtpWorker"
        private const val OTP_MAX_AGE_MILLIS = 24L * 60 * 60 * 1000

        fun schedule() {
            WorkManager.getInstance().enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AutoDeleteOtpWorker>(1, TimeUnit.DAYS).build()
            )
        }
    }

    @Inject lateinit var prefs: Preferences
    @Inject lateinit var conversationRepo: ConversationRepository
    @Inject lateinit var messageRepo: MessageRepository

    override fun doWork(): Result {
        if (!prefs.autoDeleteOtps.get()) return Result.success()

        val cutoff = System.currentTimeMillis() - OTP_MAX_AGE_MILLIS

        Realm.getDefaultInstance().use { realm ->
            val otpConversations = realm.copyFromRealm(
                realm.where(Conversation::class.java)
                    .equalTo("category", MessageClassifier.CATEGORY_OTP)
                    .equalTo("archived", false)
                    .equalTo("blocked", false)
                    .findAll()
            )

            for (conversation in otpConversations) {
                val messages = messageRepo.getMessagesSync(conversation.id)
                val staleIds = messages.filter { it.date < cutoff }.map { it.id }

                when {
                    // nothing to clean for this thread
                    staleIds.isEmpty() -> Unit

                    // every message in the thread is a stale OTP, remove the whole thread
                    staleIds.size == messages.size ->
                        conversationRepo.deleteConversations(conversation.id)

                    // delete only the stale OTP messages
                    else -> messageRepo.deleteMessages(staleIds)
                }
            }
        }

        return Result.success()
    }
}
