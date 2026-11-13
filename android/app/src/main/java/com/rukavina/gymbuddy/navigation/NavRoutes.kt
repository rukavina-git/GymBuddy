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
    /** Exercise page (R13-1). */
    const val ExercisePage = "exercise/{exerciseId}?atProgress={atProgress}"
    /** Create A / Edit (R13-4); no exerciseId = new exercise. */
    const val ExerciseEdit = "exercise_edit?exerciseId={exerciseId}"
    const val Workouts = "workouts"
    const val Progress = "progress"
    /** All sessions (R7-7); exerciseId filters it (R7-8). */
    const val AllSessions = "all_sessions?exerciseId={exerciseId}"
    /** Session detail (R14-1); exerciseId opens and outlines that exercise (R14-3). */
    const val SessionDetail = "session/{sessionId}?exerciseId={exerciseId}"
    /** Edit workout (R14-4). */
    const val SessionEdit = "session/{sessionId}/edit"
    const val You = "you"
    const val Profile = "profile"
    const val About = "about"
    const val Licences = "licences"
    const val DeleteAccount = "delete_account"
    const val ActiveWorkout = "active_workout"
    const val WorkoutSummary = "workout_summary"
    const val TemplatePage = "template/{templateId}"
    /** Template editor; no templateId = new template. */
    const val TemplateEditor = "template_editor?templateId={templateId}"
    /** Exercise picker; inTemplate = comma-joined exercise ids already in the template. */
    const val ExercisePicker = "exercise_picker?inTemplate={inTemplate}"

    /** Body weight (R15-5). */
    const val BodyWeight = "body_weight"
    const val HiddenExercises = "hidden_exercises"
    const val HiddenTemplates = "hidden_templates"
    const val SyncDebug = "sync_debug"

    fun signIn() = SignIn

    fun setup(offerAccount: Boolean = true) = "setup?offerAccount=$offerAccount"

    fun createAccount(from: CreateFrom) = "create_account?from=${from.name}"

    fun templatePage(templateId: String) = "template/$templateId"

    fun templateEditor(templateId: String? = null) =
        if (templateId == null) "template_editor" else "template_editor?templateId=$templateId"

    fun exercisePicker(inTemplate: List<String>) = "exercise_picker?inTemplate=${inTemplate.distinct().joinToString(",")}"

    fun exercisePage(exerciseId: String, atProgress: Boolean = false) = "exercise/$exerciseId?atProgress=$atProgress"

    fun allSessions(exerciseId: String? = null) =
        if (exerciseId == null) "all_sessions" else "all_sessions?exerciseId=$exerciseId"

    fun sessionDetail(sessionId: String, exerciseId: String? = null) =
        if (exerciseId == null) "session/$sessionId" else "session/$sessionId?exerciseId=$exerciseId"

    fun sessionEdit(sessionId: String) = "session/$sessionId/edit"

    fun exerciseEdit(exerciseId: String? = null) =
        if (exerciseId == null) "exercise_edit" else "exercise_edit?exerciseId=$exerciseId"
}