package com.rukavina.gymbuddy.update

/**
 * What to do about a newer version on Play, from the release's update
 * priority (0-5, set per release in android/fastlane/update-priority.yml):
 * 4-5 force it, 1-3 offer it once per version, 0 leaves it to Play's
 * normal auto-update.
 */
enum class UpdateAction { NONE, FORCE, PROMPT }

data class UpdateOffer(
    val available: Boolean,
    /** Play is already running an update we started (app was left mid-update). */
    val inProgress: Boolean,
    val priority: Int,
    val versionCode: Int,
    val immediateAllowed: Boolean,
)

object UpdatePolicy {
    const val FORCE_FROM = 4

    fun decide(offer: UpdateOffer, lastPromptedVersion: Int?): UpdateAction = when {
        offer.inProgress -> UpdateAction.FORCE
        !offer.available || !offer.immediateAllowed -> UpdateAction.NONE
        offer.priority >= FORCE_FROM -> UpdateAction.FORCE
        offer.priority > 0 && offer.versionCode != lastPromptedVersion -> UpdateAction.PROMPT
        else -> UpdateAction.NONE
    }
}
