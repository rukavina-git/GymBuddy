package com.rukavina.gymbuddy.domain.model

/** How long the user has been lifting, as onboarding asks (Plan 6 step 05). */
enum class ExperienceLevel {
    /** "New to lifting": under 6 months. */
    NEW,

    /** "Some experience": knows the main lifts. */
    SOME,

    /** "Experienced": years of training. */
    EXPERIENCED
}
