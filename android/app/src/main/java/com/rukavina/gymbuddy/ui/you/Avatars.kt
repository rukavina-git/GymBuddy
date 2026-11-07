package com.rukavina.gymbuddy.ui.you

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.FullShape

/**
 * Preset avatars (R15-4, decision 5): 20 animals from Microsoft Fluent
 * Emoji (MIT, 3D style, 144 px PNG in drawable-nodpi). The profile
 * stores the id; null or an unknown id shows the letter.
 */
object Avatars {
    data class Animal(val id: String, @DrawableRes val image: Int, @StringRes val name: Int)

    /** Picker order (R15-4). Ids match `^[a-z0-9-]{1,32}$` (server). */
    val all: List<Animal> = listOf( // same ids and order as domain AvatarIds
        Animal("fox", R.drawable.avatar_fox, R.string.avatar_fox),
        Animal("cat", R.drawable.avatar_cat, R.string.avatar_cat),
        Animal("dog", R.drawable.avatar_dog, R.string.avatar_dog),
        Animal("panda", R.drawable.avatar_panda, R.string.avatar_panda),
        Animal("bear", R.drawable.avatar_bear, R.string.avatar_bear),
        Animal("koala", R.drawable.avatar_koala, R.string.avatar_koala),
        Animal("tiger", R.drawable.avatar_tiger, R.string.avatar_tiger),
        Animal("lion", R.drawable.avatar_lion, R.string.avatar_lion),
        Animal("frog", R.drawable.avatar_frog, R.string.avatar_frog),
        Animal("monkey", R.drawable.avatar_monkey, R.string.avatar_monkey),
        Animal("pig", R.drawable.avatar_pig, R.string.avatar_pig),
        Animal("rabbit", R.drawable.avatar_rabbit, R.string.avatar_rabbit),
        Animal("hamster", R.drawable.avatar_hamster, R.string.avatar_hamster),
        Animal("owl", R.drawable.avatar_owl, R.string.avatar_owl),
        Animal("penguin", R.drawable.avatar_penguin, R.string.avatar_penguin),
        Animal("unicorn", R.drawable.avatar_unicorn, R.string.avatar_unicorn),
        Animal("dragon", R.drawable.avatar_dragon, R.string.avatar_dragon),
        Animal("wolf", R.drawable.avatar_wolf, R.string.avatar_wolf),
        Animal("sloth", R.drawable.avatar_sloth, R.string.avatar_sloth),
        Animal("otter", R.drawable.avatar_otter, R.string.avatar_otter),
    )

    fun of(id: String?): Animal? = all.firstOrNull { it.id == id }

    /** First letter of [name], upper case; "?" when blank. */
    fun letter(name: String): String = name.trim().take(1).uppercase().ifEmpty { "?" }
}

/** The chosen animal, or the first letter on a plain circle. Decorative (no TalkBack text). */
@Composable
fun Avatar(id: String?, name: String, size: Dp, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val animal = Avatars.of(id)
    Box(
        modifier.size(size).clip(FullShape).background(cs.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (animal != null) {
            Image(painterResource(animal.image), null, Modifier.size(size).padding(size / 8))
        } else {
            Text(
                Avatars.letter(name),
                style = if (size >= LargeAvatarMin) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private val LargeAvatarMin = androidx.compose.ui.unit.Dp(80f)
