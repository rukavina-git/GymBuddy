package com.rukavina.gymbuddy.domain

/**
 * Mirrors the enum schemas in api/openapi.yaml exactly - name-for-name -
 * so Jackson's default enum serialization (by .name) round-trips the
 * wire format with no custom (de)serializers.
 */

enum class MuscleGroup { CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE, OTHER }

enum class Equipment {
    BARBELL, DUMBBELL, KETTLEBELL, CABLE, MACHINE, BODYWEIGHT, RESISTANCE_BAND,
    EZ_BAR, TRAP_BAR, PULL_UP_BAR, BENCH, MEDICINE_BALL, SUSPENSION_TRAINER, SMITH_MACHINE
}

enum class DifficultyLevel { BEGINNER, INTERMEDIATE, ADVANCED }

enum class ExerciseCategory {
    STRENGTH, CARDIO, FLEXIBILITY, PLYOMETRIC, OLYMPIC_LIFTING, STRONGMAN, CALISTHENICS
}

enum class ExerciseType { COMPOUND, ISOLATION }

enum class ExerciseTrackingType {
    WEIGHT_REPS, REPS_ONLY, WEIGHT_DURATION, DURATION, DISTANCE_DURATION, WEIGHT_DISTANCE
}

enum class EntitySource { DEFAULT, CUSTOM }

enum class Gender { MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY }

enum class FitnessGoal { BUILD_MUSCLE, LOSE_WEIGHT, MAINTAIN, GENERAL_FITNESS, IMPROVE_ENDURANCE }

enum class ActivityLevel { SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, ATHLETE }
