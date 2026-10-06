package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.MessageDigest

class AuthManager(context: Context) {
    companion object {
        const val APP_HASH = "2f03a273df7e7fc706f021d3722238fcceda044783a8c9082087b8b941de8c3e"
        const val ADMIN_HASH = "2f03a273df7e7fc706f021d3722238fcceda044783a8c9082087b8b941de8c3e"
        const val MAX_TRIES = 3
        const val LOCK_MS = 60_000L

        private const val PREFS_NAME = "stock_manager_auth"
        private const val KEY_AUTHED = "is_authed"
        private const val KEY_ADMIN = "is_admin"
        private const val KEY_FAILS = "login_fails"
        private const val KEY_LOCK_UNTIL = "lock_until"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isAuthed = MutableStateFlow(prefs.getBoolean(KEY_AUTHED, false))
    val isAuthed: StateFlow<Boolean> = _isAuthed.asStateFlow()

    private val _isAdmin = MutableStateFlow(prefs.getBoolean(KEY_ADMIN, false))
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _lockRemainingSeconds = MutableStateFlow(0)
    val lockRemainingSeconds: StateFlow<Int> = _lockRemainingSeconds.asStateFlow()

    private var loginFails = prefs.getInt(KEY_FAILS, 0)
    private var lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0L)
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        val now = System.currentTimeMillis()
        if (lockUntil > now) {
            startLockCountdown()
        } else {
            lockUntil = 0
            loginFails = 0
            prefs.edit().remove(KEY_LOCK_UNTIL).remove(KEY_FAILS).apply()
        }
    }

    fun isLocked(): Boolean = System.currentTimeMillis() < lockUntil

    fun getRemainingSeconds(): Int {
        val diff = lockUntil - System.currentTimeMillis()
        return if (diff > 0) ((diff + 999) / 1000).toInt() else 0
    }

    private fun startLockCountdown() {
        tickerJob?.cancel()
        _lockRemainingSeconds.value = getRemainingSeconds()
        tickerJob = scope.launch {
            while (isActive && isLocked()) {
                _lockRemainingSeconds.value = getRemainingSeconds()
                delay(500)
            }
            _lockRemainingSeconds.value = 0
            lockUntil = 0
            loginFails = 0
            prefs.edit().remove(KEY_LOCK_UNTIL).remove(KEY_FAILS).apply()
        }
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    sealed class AuthResult {
        object Success : AuthResult()
        data class Locked(val secondsLeft: Int) : AuthResult()
        data class WrongPassword(val triesLeft: Int) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    fun unlockApp(password: String): AuthResult {
        if (isLocked()) {
            return AuthResult.Locked(getRemainingSeconds())
        }
        val trimmed = password.trim()
        if (trimmed.isEmpty()) {
            return AuthResult.Error("Enter the password.")
        }
        val calculatedHash = hash(trimmed)
        if (calculatedHash == APP_HASH) {
            loginFails = 0
            lockUntil = 0
            prefs.edit()
                .putBoolean(KEY_AUTHED, true)
                .remove(KEY_FAILS)
                .remove(KEY_LOCK_UNTIL)
                .apply()
            _isAuthed.value = true
            return AuthResult.Success
        } else {
            loginFails++
            if (loginFails >= MAX_TRIES) {
                lockUntil = System.currentTimeMillis() + LOCK_MS
                prefs.edit()
                    .putInt(KEY_FAILS, loginFails)
                    .putLong(KEY_LOCK_UNTIL, lockUntil)
                    .apply()
                startLockCountdown()
                return AuthResult.Locked(60)
            } else {
                prefs.edit().putInt(KEY_FAILS, loginFails).apply()
                return AuthResult.WrongPassword(MAX_TRIES - loginFails)
            }
        }
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_AUTHED, false).apply()
        _isAuthed.value = false
    }

    fun unlockAdmin(pin: String): Boolean {
        val trimmed = pin.trim()
        val calculatedHash = hash(trimmed)
        return if (calculatedHash == ADMIN_HASH) {
            _isAdmin.value = true
            prefs.edit().putBoolean(KEY_ADMIN, true).apply()
            true
        } else {
            false
        }
    }

    fun exitAdmin() {
        _isAdmin.value = false
        prefs.edit().putBoolean(KEY_ADMIN, false).apply()
    }
}
