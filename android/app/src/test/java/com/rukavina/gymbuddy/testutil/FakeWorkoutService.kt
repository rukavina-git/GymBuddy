package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.ui.workout.service.RestBuzzScheduler
import com.rukavina.gymbuddy.ui.workout.service.WorkoutNotice
import com.rukavina.gymbuddy.ui.workout.service.WorkoutServiceStarter

/** Records every notice the ViewModel pushes; the last one is what the service shows. */
class FakeWorkoutServiceStarter : WorkoutServiceStarter {
    val notices = mutableListOf<WorkoutNotice?>()
    val last: WorkoutNotice? get() = notices.lastOrNull()

    override fun update(notice: WorkoutNotice?) {
        notices += notice
    }
}

/** Records schedule/cancel calls; [pending] is the buzz waiting, if any. */
class FakeRestBuzzScheduler : RestBuzzScheduler {
    val calls = mutableListOf<String>()
    var pending: Long? = null
        private set

    override fun schedule(leftSeconds: Long) {
        calls += "schedule $leftSeconds"
        pending = leftSeconds
    }

    override fun cancel() {
        calls += "cancel"
        pending = null
    }
}
