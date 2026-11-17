package com.rukavina.gymbuddy.data.sync

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * When the "You're signed out" dialog opens by itself (Figma "Signed out
 * · locked 2026-11-16", council 2026-11-16 + owner):
 *
 * - the first time a session is found dead: always;
 * - after "Continue without account": again only once [GAP_MS] has passed,
 *   and never after [MAX_CONTINUES] such taps. Then only the markers stay.
 *
 * A sign-in starts over. Counted on this phone only, in a prefs file
 * that Auto Backup leaves out, so a reinstall starts over too.
 */
class SignedOutPrompt(
    private val store: Store,
    private val now: () -> Long
) {
    interface Store {
        var continues: Int
        var lastContinueAt: Long
    }

    fun shouldShow(): Boolean =
        store.continues == 0 ||
            (store.continues < MAX_CONTINUES && now() - store.lastContinueAt >= GAP_MS)

    fun onContinue() {
        store.continues += 1
        store.lastContinueAt = now()
    }

    fun reset() {
        store.continues = 0
        store.lastContinueAt = 0
    }

    companion object {
        const val MAX_CONTINUES = 3
        const val GAP_MS = 48L * 60 * 60 * 1000

        /** Excluded in backup_rules.xml and data_extraction_rules.xml. */
        const val PREFS_NAME = "signed_out_prompt"
    }
}

class SharedPrefsSignedOutStore(private val prefs: SharedPreferences) : SignedOutPrompt.Store {
    override var continues: Int
        get() = prefs.getInt(KEY_CONTINUES, 0)
        set(value) { prefs.edit { putInt(KEY_CONTINUES, value) } }

    override var lastContinueAt: Long
        get() = prefs.getLong(KEY_LAST, 0)
        set(value) { prefs.edit { putLong(KEY_LAST, value) } }

    private companion object {
        const val KEY_CONTINUES = "continues"
        const val KEY_LAST = "last_continue_at"
    }
}
