package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.api.dto.ExerciseLibraryResponseDto
import com.rukavina.gymbuddy.api.dto.ReferenceVersionDto
import com.rukavina.gymbuddy.api.dto.TemplateLibraryResponseDto
import com.rukavina.gymbuddy.persistence.ExerciseReferenceRepository
import com.rukavina.gymbuddy.persistence.ReferenceVersionRepository
import com.rukavina.gymbuddy.persistence.WorkoutTemplateReferenceRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Read-only default exercise/template libraries, per api/openapi.yaml.
 * Deprecated entries are included deliberately - clients need them to
 * resolve existing references and are responsible for filtering them
 * out of browsing themselves.
 */
@RestController
class ReferenceController(
    private val referenceVersionRepository: ReferenceVersionRepository,
    private val exerciseReferenceRepository: ExerciseReferenceRepository,
    private val workoutTemplateReferenceRepository: WorkoutTemplateReferenceRepository,
) {

    @GetMapping("/v1/reference/version")
    fun getVersion(): ReferenceVersionDto = referenceVersionRepository.get()

    @GetMapping("/v1/reference/exercises")
    fun getExercises(): ExerciseLibraryResponseDto {
        val version = referenceVersionRepository.get()
        return ExerciseLibraryResponseDto(
            version = version.exerciseLibraryVersion,
            exercises = exerciseReferenceRepository.findAllDefault(),
        )
    }

    @GetMapping("/v1/reference/templates")
    fun getTemplates(): TemplateLibraryResponseDto {
        val version = referenceVersionRepository.get()
        return TemplateLibraryResponseDto(
            version = version.templateLibraryVersion,
            templates = workoutTemplateReferenceRepository.findAllDefault(),
        )
    }
}
