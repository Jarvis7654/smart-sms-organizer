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
import android.provider.Settings
import dev.octoshrimpy.quik.feature.otp.launchOtpPopup
import dev.octoshrimpy.quik.manager.OtpPopupManager
import dev.octoshrimpy.quik.util.Preferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtpPopupManagerImpl @Inject constructor(
    private val context: Context,
    private val prefs: Preferences
) : OtpPopupManager {

    override fun showOtpPopup(code: String, sender: String) {
        if (!prefs.otpPopup.get()) return

        // Android only allows launching an overlay activity from the background when the
        // user granted "draw over other apps". Without it we fall back to the notification
        // "Copy code" action instead.
        if (!Settings.canDrawOverlays(context)) return

        try {
            context.launchOtpPopup(code, sender)
        } catch (e: Exception) {
            Timber.w(e, "Could not show OTP popup")
        }
    }
}
