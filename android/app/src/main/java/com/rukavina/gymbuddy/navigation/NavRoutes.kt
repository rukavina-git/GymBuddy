package com.rukavina.gymbuddy.navigation

import com.rukavina.gymbuddy.ui.auth.CreateFrom

object NavRoutes {
    const val Splash = "splash"
    const val Main = "main"
    const val Welcome = "welcome"
    /** Setup; [setup] with offerAccount = false skips step 5 (they already said "Not now"). */
    const val Setup = "setup?offerAccount={offerAccount}"
    const val SignIn = "sign_in"
    /** Create account (method → email → consent); [createAccount] fills where it started. */
    const val CreateAccount = "create_account?from={from}"
    const val Home = "home"
    const val Templates = "templates"
    const val Exercises = "exercises"
    const val ExerciseDetails = "exercise_details/{exerciseId}"
    const val Workouts = "workouts"
    const val Progress = "progress"
    const val You = "you"
    const val Profile = "profile"
    const val About = "about"
    const val ActiveWorkout = "active_workout"
    const val WorkoutSummary = "workout_summary"
    const val TemplatePage = "template/{templateId}"
    /** Template editor; no templateId = new template. */
    const val TemplateEditor = "template_editor?templateId={templateId}"
    /** Exercise picker; inTemplate = comma-joined exercise ids already in the template. */
    const val ExercisePicker = "exercise_picker?inTemplate={inTemplate}"

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
    const val HiddenTemplates = "hidden_templates"
    const val AppPreferences = "app_preferences"
    const val SyncDebug = "sync_debug"

    fun signIn() = SignIn

    fun setup(offerAccount: Boolean = true) = "setup?offerAccount=$offerAccount"

    fun createAccount(from: CreateFrom) = "create_account?from=${from.name}"

    fun templatePage(templateId: String) = "template/$templateId"

    fun templateEditor(templateId: String? = null) =
        if (templateId == null) "template_editor" else "template_editor?templateId=$templateId"

    fun exercisePicker(inTemplate: List<String>) = "exercise_picker?inTemplate=${inTemplate.distinct().joinToString(",")}"

    fun exerciseDetailsRoute(exerciseId: String): String {
        return "exercise_details/$exerciseId"
    }
}