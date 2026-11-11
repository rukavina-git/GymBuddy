package com.rukavina.gymbuddy.domain.model

import kotlin.random.Random

/**
 * Preset avatar ids (R15-4), in picker order. `ui/you/Avatars` maps each
 * to its picture and name; ProfileViewModelTest keeps the two lists equal.
 */
object AvatarIds {
    val all: List<String> = listOf(
        "fox", "cat", "dog", "panda", "bear", "koala", "tiger", "lion", "frog", "monkey",
        "pig", "rabbit", "hamster", "owl", "penguin", "unicorn", "dragon", "wolf", "sloth", "otter",
    )

    /** A new profile's default picture (F4). */
    fun random(random: Random = Random.Default): String = all.random(random)
}
