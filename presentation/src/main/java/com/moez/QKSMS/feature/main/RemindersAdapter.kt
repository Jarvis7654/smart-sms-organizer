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
package dev.octoshrimpy.quik.feature.main

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.Navigator
import dev.octoshrimpy.quik.common.util.ReminderFormatter
import dev.octoshrimpy.quik.databinding.ReminderListItemBinding
import dev.octoshrimpy.quik.model.Reminder
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/** Renders the reminder cards for the Upcoming tab. */
class RemindersAdapter @Inject constructor(
    private val context: Context,
    private val navigator: Navigator
) : RecyclerView.Adapter<RemindersAdapter.ReminderViewHolder>() {

    var data: List<Reminder> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    val dismissClicks: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ReminderListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = data.getOrNull(position) ?: return
        val binding = holder.binding

        binding.typeIcon.setImageResource(iconFor(reminder.type))

        binding.title.text = reminder.title
        binding.due.text = when {
            reminder.dueDate > 0 -> ReminderFormatter.format(context, reminder.dueDate)
            else -> context.getString(R.string.upcoming_no_date)
        }
        binding.amount.isVisible = reminder.amount.isNotBlank()
        binding.amount.text = reminder.amount

        binding.subtitle.text = when {
            reminder.subtitle.isNotBlank() && reminder.threadId != 0L ->
                context.getString(R.string.upcoming_from_sender, reminder.subtitle)
            reminder.subtitle.isNotBlank() -> reminder.subtitle
            else -> ""
        }
        binding.subtitle.isVisible = reminder.subtitle.isNotBlank()

        binding.dismiss.setOnClickListener { dismissClicks.onNext(reminder.id) }
        binding.root.setOnClickListener {
            if (reminder.threadId != 0L) navigator.showConversation(reminder.threadId)
        }
    }

    override fun getItemCount(): Int = data.size

    private fun iconFor(type: String) = when (type) {
        Reminder.TYPE_BILL -> R.drawable.ic_reminder_bill
        Reminder.TYPE_TRAIN -> R.drawable.ic_reminder_train
        Reminder.TYPE_FLIGHT -> R.drawable.ic_reminder_flight
        Reminder.TYPE_BUS -> R.drawable.ic_reminder_bus
        Reminder.TYPE_MOVIE -> R.drawable.ic_reminder_movie
        Reminder.TYPE_APPOINTMENT -> R.drawable.ic_reminder_appointment
        Reminder.TYPE_EVENT -> R.drawable.ic_reminder_event
        Reminder.TYPE_ORDER -> R.drawable.ic_reminder_order
        else -> R.drawable.ic_reminder_event
    }

    class ReminderViewHolder(val binding: ReminderListItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
