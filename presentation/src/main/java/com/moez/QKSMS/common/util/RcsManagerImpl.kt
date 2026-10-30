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
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.ims.ProvisioningManager
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.manager.RcsManager
import dev.octoshrimpy.quik.util.Preferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gate for every RCS-related feature in the app.
 *
 * The "RCS chats" preference is the master switch: when it is off, [isRcsEnabled] returns
 * false and no RCS code path runs at all. When it is on, we do a best-effort probe of the
 * device's RCS provisioning state (carrier dependent) purely for status display.
 *
 * Note: Android does not expose a public API for third-party apps to *transport* RCS
 * messages (that requires carrier/Google certification), so this app treats RCS as an
 * indicator layer on top of SMS/MMS and falls back to plain SMS for delivery.
 */
@Singleton
class RcsManagerImpl @Inject constructor(
    private val context: Context,
    private val prefs: Preferences
) : RcsManager {

    override fun isRcsEnabled(): Boolean = prefs.rcsEnabled.get()

    override fun isRcsAvailable(): Boolean {
        if (!isRcsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        if (!context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY)) {
            return false
        }

        return try {
            val subId = SubscriptionManager.getDefaultSubscriptionId()
            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return false

            val provisioningManager = context.getSystemService(ProvisioningManager::class.java)
                ?: return false

            // CAPABILITY_TYPE_PRESENCE_UC (value 2) is a hidden ImsRcsController constant:
            // CAPABILITY_TYPE_OPTIONS_UCE = 1 shl 0, CAPABILITY_TYPE_PRESENCE_UC = 1 shl 1
            val capabilityTypePresenceUc = 1 shl 1
            provisioningManager.getRcsProvisioningStatusForCapability(subId, capabilityTypePresenceUc)
        } catch (e: Throwable) {
            Timber.v(e, "RCS capability probe failed")
            false
        }
    }

    override fun getStatusSummary(): String? {
        return when {
            !isRcsEnabled() -> context.getString(R.string.settings_rcs_summary_off)
            isRcsAvailable() -> context.getString(R.string.settings_rcs_summary_on_available)
            else -> context.getString(R.string.settings_rcs_summary_on_unavailable)
        }
    }
}
