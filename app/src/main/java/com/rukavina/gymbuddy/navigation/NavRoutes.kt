package com.rukavina.gymbuddy.navigation

object NavRoutes {
    const val Splash = "splash"
    const val Main = "main"
    const val AuthGraph = "auth_graph"
    const val Login = "login"
    const val Registration = "registration"
    const val Home = "home"
    const val Templates = "templates"
    const val Exercises = "exercises"
    const val ExerciseDetails = "exercise_details/{exerciseId}"
    const val Workouts = "workouts"
    const val Statistics = "statistics"
    const val Settings = "settings"
    const val Profile = "profile"
    const val About = "about"
    const val ActiveWorkout = "active_workout"

    // Profile edit screens
    const val EditName = "edit_name"
    const val EditBio = "edit_bio"
    const val EditBirthdate = "edit_birthdate"
    const val EditWeight = "edit_weight"
    const val EditHeight = "edit_height"
    const val EditTargetWeight = "edit_target_weight"
    const val EditGender = "edit_gender"
    const val EditFitnessGoal = "edit_fitness_goal"
    const val EditActivityLevel = "edit_activity_level"
    const val EditUnits = "edit_units"
    const val HiddenExercises = "hidden_exercises"

    fun exerciseDetailsRoute(exerciseId: Int): String {
        return "exercise_details/$exerciseId"
    }
}