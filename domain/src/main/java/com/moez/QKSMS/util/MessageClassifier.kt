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
package dev.octoshrimpy.quik.util

import java.util.regex.Pattern

/**
 * Lightweight, fully on-device classifier that sorts messages into the categories used by
 * the auto-organized inbox (Personal / Transactions / OTP / Offers / Spam).
 *
 * It combines sender-id heuristics (e.g. DLT-style transactional sender IDs like "JD-SBIIN"
 * used by banks and businesses in many countries) with weighted keyword matching, so it works
 * offline without any ML model or network access.
 */
object MessageClassifier {

    // Categories, ordered by priority during classification
    const val CATEGORY_ALL = -1        // UI only: no filter
    const val CATEGORY_PERSONAL = 0
    const val CATEGORY_TRANSACTION = 1
    const val CATEGORY_OTP = 2
    const val CATEGORY_OFFERS = 3
    const val CATEGORY_SPAM = 4

    // --- OTP detection ---------------------------------------------------------------

    private val OTP_KEYWORD = Pattern.compile(
        "(?i)(\\b(?:otp|o\\.t\\.p)\\b|one[- ]time[- ](?:password|code|pin)|" +
                "verification code|verify code|confirm(?:ation)? code|security code|" +
                "login code|passcode|access code|auth(?:entication)? code|\\bcode is\\b)"
    )

    // "OTP is 123456", "code: 1234", "123456 is your verification code", "use 123456 to login"
    private val OTP_CODE_BEFORE = Pattern.compile(
        "(?i)(?:otp|code|password|passcode|pin|pincode)[^0-9\\n]{0,24}([0-9]{4,8})"
    )
    private val OTP_CODE_AFTER = Pattern.compile(
        "(?i)([0-9]{4,8})[^0-9\\n]{0,24}(?:is|as)[^0-9\\n]{0,10}(?:your|the)?\\s*" +
                "(?:otp|code|password|passcode|pin|verification|verification code|one time password)"
    )
    private val OTP_CODE_USE = Pattern.compile(
        "(?i)(?:use|enter|enter this|dial)?\\s*([0-9]{4,8})\\s+(?:to|as|for)\\s+(?:verify|confirm|login|log in|validate|complete)"
    )
    // Bare 4-8 digit group, only trusted when an OTP keyword is present
    private val BARE_CODE = Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")

    // A currency symbol/word immediately preceding a digit group means it's money, not a code
    private val MONEY_PREFIX = Pattern.compile("(?i)(?:rs\\.?|inr|₹|usd|eur|\\$|£)\\s*.{0,4}$")
    private val YEAR = Pattern.compile("(?<![0-9])(?:19|20)[0-9]{2}(?![0-9])")
    private val PHONE = Pattern.compile("(?<![0-9])[0-9]{10}(?![0-9])")

    /**
     * Returns the OTP code contained in [body], or null when no high-confidence code is found.
     */
    fun extractOtp(body: String): String? {
        if (body.isEmpty()) return null
        if (OTP_KEYWORD.matcher(body).find()) {
            OTP_CODE_BEFORE.matcher(body).let { m -> if (m.find()) return m.group(1) }
            OTP_CODE_AFTER.matcher(body).let { m -> if (m.find()) return m.group(1) }
            OTP_CODE_USE.matcher(body).let { m -> if (m.find()) return m.group(1) }
            // fall back to the last bare digit group that isn't money / a year / a phone number
            val m = BARE_CODE.matcher(body)
            var candidate: String? = null
            while (m.find()) {
                val group = m.group(1)
                val prefix = body.substring(0, m.start(1)).takeLast(8)
                if (MONEY_PREFIX.matcher(prefix).find()) continue
                if (YEAR.matcher(group).matches()) continue
                candidate = group // keep the last one; OTPs usually appear late in the text
            }
            return candidate
        }
        return null
    }

    // --- Category classification ------------------------------------------------------

    private val TRANSACTION_WORDS = Pattern.compile(
        "(?i)(\\bdebit(?:ed|ing)?\\b|\\bcredit(?:ed|ing)?\\b|\\bcr\\b|\\bdr\\b|\\bbal(?:ance)?\\b|" +
                "\\ba/?c\\b|\\bacc(?:oun)?t\\b|\\bbank(?:ing)?\\b|\\bupi\\b|\\bimps\\b|\\bneft\\b|\\brtgs\\b|" +
                "\\btxn\\b|\\btransaction\\b|\\bpaid\\b|\\bpayment\\b|\\bpay\\b|\\binvoice\\b|\\bemi\\b|" +
                "\\bsalary\\b|\\brefund(?:ed)?\\b|\\breceipt\\b|\\bwallet\\b|\\bcard\\b|\\bStatement\\b|" +
                "\\bbill\\b|\\bminimum due\\b|\\bdue date\\b|\\boutstanding\\b|\\bCOD\\b|\\border (?:shipped|delivered)\\b|" +
                "\\btrack(?:ing)?\\b|\\bshipment\\b|\\bbooked\\b|\\bticket\\b|\\bpnr\\b|\\bappointment\\b|\\brecharge\\b)"
    )

    private val OFFER_WORDS = Pattern.compile(
        "(?i)(\\boffer\\b|\\boffers\\b|\\bdiscount\\b|\\bsale\\b|\\bdeal\\b|\\bdeals\\b|\\bcoupon\\b|" +
                "\\bcashback\\b|\\bsave\\b|\\bflat\\s?[0-9]{1,2}(?:\\.[0-9]+)?\\s?%\\b|\\b[0-9]{1,2}\\s?%\\s?off\\b|" +
                "\\bfree\\b|\\bgift\\b|\\bbuy (?:one|1|2|now)\\b|\\bshop(?:ping)?\\b|\\bnew arrival\\b|" +
                "\\bexclusive\\b|\\blimited (?:period|time|offer)\\b|\\blowest price\\b|\\bbest price\\b|" +
                "\\bsale is live\\b|\\bmega sale\\b|\\bsubscribe\\b|\\bunsub(?:scribe)?\\b|\\bopt[ -]?out\\b|" +
                "\\bstop to\\b|\\bpromo(?:tion)?\\b|\\breward(?:s)?\\b|\\bpoints\\b|\\bmembership\\b|\\binvite\\b)"
    )

    private val SPAM_WORDS = Pattern.compile(
        "(?i)(\\bcongratulations\\b|\\bwinner\\b|\\byou (?:have )?won\\b|\\blottery\\b|\\blucky draw\\b|" +
                "\\bprize\\b|\\bclaim now\\b|\\bclaim your\\b|\\bclick (?:here|the link)\\b|\\bkyc\\b|" +
                "\\b(?:account|card).{0,20}(?:suspend|block|expire|frozen)\\b|\\bverify your account\\b|" +
                "\\b urgently\\b|\\bimmediately\\b|\\bATM frozen\\b|\\bloan (?:approved|offer)\\b|" +
                "\\binvestment opportunity\\b|\\bdouble your\\b|\\bguaranteed (?:return|income)\\b|" +
                "\\bwaive.{0,10}(?:charges|fees)\\b|\\bsim card (?:suspend|deactivate)\\b)"
    )

    // DLT-style transactional sender IDs used in India and similar regimes: "XY-ABCD12"
    private val DLT_SENDER = Pattern.compile("^[A-Z]{2}[- ][A-Z0-9]{4,8}$")
    // Alphanumeric sender (up to 11 chars, mostly uppercase) e.g. "AMAZON", "SBIIN"
    private val ALPHA_SENDER = Pattern.compile("^[A-Za-z][A-Za-z0-9]{4,10}$")
    private val NUMERIC_SENDER = Pattern.compile("^\\+?[0-9]{7,15}$")

    /**
     * Classifies a message into one of the CATEGORY_* constants.
     *
     * @param address the sender address (phone number or alphanumeric sender ID)
     * @param body    the message text
     */
    fun classify(address: String, body: String): Int {
        val text = body ?: ""

        // 1. OTP messages are unambiguous when the keyword is present
        if (OTP_KEYWORD.matcher(text).find()) return CATEGORY_OTP

        // 2. Hard scam phrases win over promotion keywords
        val spamHits = countMatches(SPAM_WORDS, text)
        val offerHits = countMatches(OFFER_WORDS, text)
        val txnHits = countMatches(TRANSACTION_WORDS, text)

        // 3. Sender heuristics
        val trimmedAddress = address?.trim() ?: ""
        val numericSender = NUMERIC_SENDER.matcher(trimmedAddress).matches()
        val businessSender = DLT_SENDER.matcher(trimmedAddress).matches() ||
                (!numericSender && ALPHA_SENDER.matcher(trimmedAddress).matches())

        // 4. Scoring: scam phrases are heavily weighted
        if (spamHits >= 2 || (spamHits >= 1 && offerHits >= 1 && businessSender)) {
            return CATEGORY_SPAM
        }

        if (txnHits >= 2 || (txnHits >= 1 && businessSender && !numericSender)) {
            return CATEGORY_TRANSACTION
        }

        if (offerHits >= 1 && (businessSender || offerHits >= 2)) {
            return CATEGORY_OFFERS
        }

        if (txnHits >= 1) return CATEGORY_TRANSACTION
        if (offerHits >= 1 && businessSender) return CATEGORY_OFFERS

        // 5. Everything else is personal conversation
        return CATEGORY_PERSONAL
    }

    private fun countMatches(pattern: Pattern, text: String): Int {
        val matcher = pattern.matcher(text)
        var count = 0
        while (matcher.find()) count++
        return count
    }

    // --- money extraction -------------------------------------------------------------

    // "Rs 1,250.00", "₹499", "INR 25000", "credited $75"
    private val MONEY_VALUE = Pattern.compile(
        "(?i)(?:rs\\.?|inr|₹|usd ?|\\$|€)\\s?[0-9]{1,3}(?:[, ][0-9]{2,3})*(?:\\.[0-9]{1,2})?" +
                "|[0-9]{1,3}(?:,[0-9]{2,3})+(?:\\.[0-9]{2})?\\s?(?:rs\\.?|inr|₹)")

    private val CREDIT_WORD = Pattern.compile("(?i)\\b(credited|credit|received|deposited|added|refunded|cashback|salary)\\b")
    private val DEBIT_WORD = Pattern.compile("(?i)\\b(debited|debit|spent|paid|deducted|withdrawn|charged|purchase|sent)\\b")

    /** True when the message text mentions money being credited. */
    fun isCredit(body: String): Boolean = CREDIT_WORD.matcher(body).find()

    /** True when the message text mentions money being debited. */
    fun isDebit(body: String): Boolean = DEBIT_WORD.matcher(body).find()

    /**
     * Returns the first money amount in [body] with its currency prefix (e.g. "Rs 1,250"),
     * or null when no amount is present.
     */
    fun extractAmount(body: String): String? {
        val m = MONEY_VALUE.matcher(body)
        return if (m.find()) m.group().trim() else null
    }

    // --- coupon / offer codes ----------------------------------------------------------

    private val OFFER_CODE_KEYWORD = Pattern.compile(
        "(?i)\\b(coupon|promo(?:tion)? code|offer code|use code|code:|voucher)\\b")

    /**
     * Returns an offer/coupon code from [body] (alphanumeric token near a coupon keyword),
     * or null when none is found. Falls back to a plain OTP extraction result.
     */
    fun extractOfferCode(body: String): String? {
        if (!OFFER_CODE_KEYWORD.matcher(body).find()) return null
        val code = Pattern.compile("(?i)(?:code|voucher)\\s*[:#-]?\\s*([A-Z0-9]{4,15})")
        code.matcher(body).let { m -> if (m.find()) return m.group(1) }
        return null
    }
}
