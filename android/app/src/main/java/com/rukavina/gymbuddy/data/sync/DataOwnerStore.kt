package com.rukavina.gymbuddy.data.sync

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whose data is open on this phone (ADR 0014 "dataOwnerUid"): an account
 * uid, or null for the guest. Device-local, and separate from Firebase's
 * signed-in user on purpose: Firebase drops the user by itself when a
 * session dies, but the data stays that account's until the user leaves
 * on purpose ([release]).
 *
 * Read synchronously at startup (the database opens before any
 * coroutine runs), hence SharedPreferences rather than DataStore.
 */
interface DataOwnerStore {
    val owner: String?

    /** The owner's email as last seen, for "k…@gmail.com" in the banner and dialogs. */
    val email: String?
    val ownerFlow: StateFlow<String?>

    fun set(uid: String, email: String?)

    /** A voluntary exit: logout, account deletion, merge Cancel, keep as guest, erase. */
    fun release()
}

class SharedPrefsDataOwnerStore(
    private val prefs: SharedPreferences,
    /** First start after the update: the Firebase user restored at startup owns the data. */
    initialOwner: () -> String?
) : DataOwnerStore {

    private val _owner: MutableStateFlow<String?>

    init {
        if (!prefs.contains(KEY_SET)) {
            val uid = initialOwner()
            prefs.edit().putBoolean(KEY_SET, true).putString(KEY_OWNER, uid).commit()
        }
        _owner = MutableStateFlow(prefs.getString(KEY_OWNER, null))
    }

    override val owner: String? get() = _owner.value
    override val email: String? get() = prefs.getString(KEY_EMAIL, null)
    override val ownerFlow: StateFlow<String?> = _owner.asStateFlow()

    override fun set(uid: String, email: String?) {
        prefs.edit().putString(KEY_OWNER, uid).putString(KEY_EMAIL, email).commit()
        _owner.value = uid
    }

    override fun release() {
        prefs.edit().remove(KEY_OWNER).remove(KEY_EMAIL).commit()
        _owner.value = null
    }

    private companion object {
        const val KEY_SET = "initialised"
        const val KEY_OWNER = "owner_uid"
        const val KEY_EMAIL = "owner_email"
    }
}
