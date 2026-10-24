package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AuthUser
import com.example.data.model.CashierSession
import com.example.data.model.StoreSettings
import com.example.data.model.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages persistent storage of Store Settings, Cashier Sessions,
 * and persistence configuration across app restarts.
 * Fast, synchronous & safe for low-spec devices like Samsung Galaxy J2.
 */
class StorePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "pos_store_preferences"

        // Store Settings keys
        private const val KEY_STORE_NAME = "store_name"
        private const val KEY_ADDRESS = "store_address"
        private const val KEY_PHONE = "store_phone"
        private const val KEY_FOOTER = "store_footer"
        private const val KEY_PAPER_SIZE = "paper_size"
        private const val KEY_CASHIER_NAME = "cashier_name"
        private const val KEY_PRINTER_MAC = "selected_printer_mac"
        private const val KEY_PRINTER_NAME = "selected_printer_name"

        // Session keys
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_SESSION_CASHIER = "session_cashier"
        private const val KEY_SESSION_START_TIME = "session_start_time"
        private const val KEY_SESSION_INITIAL_CASH = "session_initial_cash"
        private const val KEY_SESSION_IS_ACTIVE = "session_is_active"
        private const val KEY_STORAGE_MODE_ENABLED = "persistent_storage_enabled"

        // Auth keys
        private const val KEY_AUTH_ROLE = "auth_role"
        private const val KEY_AUTH_USERNAME = "auth_username"
        private const val KEY_AUTH_DISPLAY_NAME = "auth_display_name"
        private const val KEY_AUTH_LOGIN_TIME = "auth_login_time"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStoreSettings(): StoreSettings {
        return StoreSettings(
            storeName = prefs.getString(KEY_STORE_NAME, "KASIR KITA POS") ?: "KASIR KITA POS",
            address = prefs.getString(KEY_ADDRESS, "Jl. Sudirman No. 128, Indonesia") ?: "Jl. Sudirman No. 128, Indonesia",
            phone = prefs.getString(KEY_PHONE, "0812-3456-7890") ?: "0812-3456-7890",
            footerMessage = prefs.getString(
                KEY_FOOTER,
                "Terima kasih atas kunjungan Anda!\nBarang yang dibeli tidak dapat ditukar."
            ) ?: "Terima kasih atas kunjungan Anda!\nBarang yang dibeli tidak dapat ditukar.",
            paperSize = prefs.getInt(KEY_PAPER_SIZE, 58),
            cashierName = prefs.getString(KEY_CASHIER_NAME, "Kasir 1") ?: "Kasir 1",
            selectedPrinterMac = prefs.getString(KEY_PRINTER_MAC, "") ?: "",
            selectedPrinterName = prefs.getString(KEY_PRINTER_NAME, "") ?: ""
        )
    }

    fun saveStoreSettings(settings: StoreSettings) {
        prefs.edit()
            .putString(KEY_STORE_NAME, settings.storeName)
            .putString(KEY_ADDRESS, settings.address)
            .putString(KEY_PHONE, settings.phone)
            .putString(KEY_FOOTER, settings.footerMessage)
            .putInt(KEY_PAPER_SIZE, settings.paperSize)
            .putString(KEY_CASHIER_NAME, settings.cashierName)
            .putString(KEY_PRINTER_MAC, settings.selectedPrinterMac)
            .putString(KEY_PRINTER_NAME, settings.selectedPrinterName)
            .apply()
    }

    fun getCashierSession(): CashierSession {
        val existingSessionId = prefs.getString(KEY_SESSION_ID, null)
        val isStorageEnabled = prefs.getBoolean(KEY_STORAGE_MODE_ENABLED, true)

        if (existingSessionId == null) {
            // First run: create initial active session
            val newSession = generateNewSession(
                cashier = prefs.getString(KEY_CASHIER_NAME, "Kasir 1") ?: "Kasir 1",
                initialCash = 100000L
            )
            saveCashierSession(newSession)
            return newSession
        }

        return CashierSession(
            sessionId = existingSessionId,
            cashierName = prefs.getString(KEY_SESSION_CASHIER, "Kasir 1") ?: "Kasir 1",
            startTime = prefs.getLong(KEY_SESSION_START_TIME, System.currentTimeMillis()),
            initialCash = prefs.getLong(KEY_SESSION_INITIAL_CASH, 100000L),
            isActive = prefs.getBoolean(KEY_SESSION_IS_ACTIVE, true),
            isPersistentStorageEnabled = isStorageEnabled
        )
    }

    fun saveCashierSession(session: CashierSession) {
        prefs.edit()
            .putString(KEY_SESSION_ID, session.sessionId)
            .putString(KEY_SESSION_CASHIER, session.cashierName)
            .putLong(KEY_SESSION_START_TIME, session.startTime)
            .putLong(KEY_SESSION_INITIAL_CASH, session.initialCash)
            .putBoolean(KEY_SESSION_IS_ACTIVE, session.isActive)
            .putBoolean(KEY_STORAGE_MODE_ENABLED, session.isPersistentStorageEnabled)
            .apply()
    }

    fun startNewSession(cashier: String, initialCash: Long): CashierSession {
        val newSession = generateNewSession(cashier, initialCash)
        saveCashierSession(newSession)
        // Also update default cashier name in settings
        val currentSettings = getStoreSettings()
        saveStoreSettings(currentSettings.copy(cashierName = cashier))
        return newSession
    }

    fun closeSession(): CashierSession {
        val current = getCashierSession()
        val closed = current.copy(isActive = false, endTime = System.currentTimeMillis())
        saveCashierSession(closed)
        return closed
    }

    fun setStorageModeEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_STORAGE_MODE_ENABLED, enabled)
            .apply()
    }

    fun getAuthUser(): AuthUser? {
        val roleStr = prefs.getString(KEY_AUTH_ROLE, null) ?: return null
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (e: Exception) {
            return null
        }
        val username = prefs.getString(KEY_AUTH_USERNAME, "") ?: ""
        val displayName = prefs.getString(KEY_AUTH_DISPLAY_NAME, "") ?: ""
        val loginTime = prefs.getLong(KEY_AUTH_LOGIN_TIME, System.currentTimeMillis())
        return AuthUser(
            role = role,
            username = username,
            displayName = displayName,
            loginTime = loginTime
        )
    }

    fun saveAuthUser(user: AuthUser) {
        prefs.edit()
            .putString(KEY_AUTH_ROLE, user.role.name)
            .putString(KEY_AUTH_USERNAME, user.username)
            .putString(KEY_AUTH_DISPLAY_NAME, user.displayName)
            .putLong(KEY_AUTH_LOGIN_TIME, user.loginTime)
            .apply()
    }

    fun clearAuthUser() {
        prefs.edit()
            .remove(KEY_AUTH_ROLE)
            .remove(KEY_AUTH_USERNAME)
            .remove(KEY_AUTH_DISPLAY_NAME)
            .remove(KEY_AUTH_LOGIN_TIME)
            .apply()
    }

    private fun generateNewSession(cashier: String, initialCash: Long): CashierSession {
        val sdf = SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault())
        val id = "SESI-" + sdf.format(Date())
        return CashierSession(
            sessionId = id,
            cashierName = cashier,
            startTime = System.currentTimeMillis(),
            initialCash = initialCash,
            isActive = true,
            isPersistentStorageEnabled = true
        )
    }
}
