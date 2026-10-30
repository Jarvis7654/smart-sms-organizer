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
package dev.octoshrimpy.quik.manager

interface RcsManager {

    /**
     * True when the user turned the "RCS chats" master switch on. When this is false, every
     * RCS-related feature in the app is fully disabled.
     */
    fun isRcsEnabled(): Boolean

    /**
     * True when RCS chats are enabled and the device reports RCS provisioning on the active
     * subscription. Best-effort probe: returns false whenever capability cannot be determined.
     */
    fun isRcsAvailable(): Boolean

    /**
     * Human readable status shown under the settings entry, or null when not applicable.
     */
    fun getStatusSummary(): String?
}
