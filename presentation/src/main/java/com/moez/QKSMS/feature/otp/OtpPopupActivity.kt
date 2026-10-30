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
package dev.octoshrimpy.quik.feature.otp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.getSystemService
import dagger.android.AndroidInjection
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.base.QkThemedActivity
import dev.octoshrimpy.quik.common.util.extensions.makeToast
import dev.octoshrimpy.quik.databinding.OtpPopupActivityBinding

/**
 * SMS Organizer style bottom-card overlay: shows a verification or offer code as soon as
 * the message arrives and copies it to the clipboard. Launched automatically when "draw
 * over other apps" is granted, or from the "Copy code" notification action.
 */
class OtpPopupActivity : QkThemedActivity() {

    companion object {
        const val EXTRA_CODE = "code"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_IS_OFFER = "isOffer"
        private const val AUTO_DISMISS_MILLIS = 6000L
    }

    private lateinit var binding: OtpPopupActivityBinding
    private val handler = Handler(Looper.getMainLooper())
    private val autoDismiss = Runnable { finish() }
    private var code: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(false)
        }

        binding = OtpPopupActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        code = intent.getStringExtra(EXTRA_CODE) ?: run {
            finish()
            return
        }
        val sender = intent.getStringExtra(EXTRA_SENDER)
            ?: getString(R.string.otp_popup_default_sender)
        val isOffer = intent.getBooleanExtra(EXTRA_IS_OFFER, false)

        binding.popupTitle.setText(
            if (isOffer) R.string.otp_popup_title_offer else R.string.otp_popup_title
        )
        binding.popupSender.text = sender
        binding.popupCode.text = code

        binding.popupCopy.setOnClickListener {
            copyToClipboard(code)
            makeToast(if (isOffer) R.string.otp_popup_offer_copied else R.string.otp_popup_copied)
            finish()
        }
        binding.popupDismiss.setOnClickListener { finish() }
        binding.popupRoot.setOnClickListener { finish() }

        handler.postDelayed(autoDismiss, AUTO_DISMISS_MILLIS)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        handler.removeCallbacks(autoDismiss)
        handler.postDelayed(autoDismiss, AUTO_DISMISS_MILLIS)
    }

    private fun copyToClipboard(code: String) {
        getSystemService<ClipboardManager>()?.setPrimaryClip(
            ClipData.newPlainText(getString(R.string.otp_popup_clipboard_label), code)
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoDismiss)
        super.onDestroy()
    }
}

/**
 * Launch helper used from background contexts (worker/receiver).
 */
fun Context.launchOtpPopup(code: String, sender: String, isOfferCode: Boolean) {
    val intent = Intent(this, OtpPopupActivity::class.java)
        .putExtra(OtpPopupActivity.EXTRA_CODE, code)
        .putExtra(OtpPopupActivity.EXTRA_SENDER, sender)
        .putExtra(OtpPopupActivity.EXTRA_IS_OFFER, isOfferCode)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    startActivity(intent)
}
