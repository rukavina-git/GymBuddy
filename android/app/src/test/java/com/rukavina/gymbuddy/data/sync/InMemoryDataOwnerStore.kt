package com.rukavina.gymbuddy.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [DataOwnerStore] without SharedPreferences, for tests. */
class InMemoryDataOwnerStore(initial: String? = null, override var email: String? = null) : DataOwnerStore {
    private val _owner = MutableStateFlow(initial)
    override val owner: String? get() = _owner.value
    override val ownerFlow: StateFlow<String?> = _owner.asStateFlow()

    override fun set(uid: String, email: String?) {
        this.email = email
        _owner.value = uid
    }

    override fun release() {
        email = null
        _owner.value = null
    }
}
