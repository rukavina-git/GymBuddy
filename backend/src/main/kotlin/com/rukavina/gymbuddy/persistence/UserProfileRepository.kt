package com.rukavina.gymbuddy.persistence

import com.rukavina.gymbuddy.api.dto.UserProfileDto
import com.rukavina.gymbuddy.api.dto.UserProfileSyncDto
import com.rukavina.gymbuddy.domain.ActivityLevel
import com.rukavina.gymbuddy.domain.FitnessGoal
import com.rukavina.gymbuddy.domain.Gender
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * uid is both the primary key and the owner id for this table (see
 * V1__initial_schema.sql), so every read here takes it as a mandatory
 * `ownerId` parameter - there is no method that can return a profile
 * without the caller supplying whose profile to look up. Callers must
 * source it from the authenticated SecurityContext, never from a
 * client-supplied field.
 */
@Repository
class UserProfileRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun findByOwner(ownerId: String): UserProfileDto? {
        val sql = """
            SELECT uid, name, email, profile_image_url, birth_date, weight, height, gender,
                   fitness_goal, activity_level, target_weight, joined_date, bio, updated_at,
                   deleted_at, revision
            FROM user_profile
            WHERE uid = :ownerId
        """.trimIndent()
        val params = MapSqlParameterSource("ownerId", ownerId)
        return jdbcTemplate.query(sql, params) { rs, _ -> mapRow(rs) }.firstOrNull()
    }

    /**
     * Just-in-time provisioning, race-safe: ON CONFLICT DO NOTHING means
     * two concurrent first requests from the same new uid both attempt
     * the insert, but only one row is ever created - the loser's insert
     * silently affects zero rows instead of erroring or duplicating.
     * Returns true if this call created the row, false if it already existed.
     */
    fun insertIfAbsent(
        uid: String,
        name: String,
        email: String,
        profileImageUrl: String?,
    ): Boolean {
        val now = System.currentTimeMillis()
        val sql = """
            INSERT INTO user_profile (uid, name, email, profile_image_url, joined_date, updated_at, revision)
            VALUES (:uid, :name, :email, :profileImageUrl, :now, :now, 0)
            ON CONFLICT (uid) DO NOTHING
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("uid", uid)
            .addValue("name", name)
            .addValue("email", email)
            .addValue("profileImageUrl", profileImageUrl)
            .addValue("now", now)
        val rowsAffected = jdbcTemplate.update(sql, params)
        return rowsAffected > 0
    }

    /**
     * ProfileSyncService's update path. There is no insertForSync
     * counterpart: FirebaseAuthenticationFilter's just-in-time
     * provisioning (ensureProfile, via insertIfAbsent above) guarantees
     * a user_profile row exists before any authenticated request
     * reaches sync/push, so the row this updates always pre-exists -
     * ProfileSyncService's RevisionChecker.Insert branch is unreachable
     * in practice but left in place for defensiveness.
     */
    fun updateForSync(uid: String, dto: UserProfileSyncDto, newRevision: Int, now: Long) {
        val sql = """
            UPDATE user_profile
            SET name = :name, email = :email, profile_image_url = :profileImageUrl, birth_date = :birthDate,
                weight = :weight, height = :height, gender = :gender, fitness_goal = :fitnessGoal,
                activity_level = :activityLevel, target_weight = :targetWeight, joined_date = :joinedDate,
                bio = :bio, updated_at = :now, revision = :newRevision
            WHERE uid = :uid
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("uid", uid)
            .addValue("name", dto.name)
            .addValue("email", dto.email)
            .addValue("profileImageUrl", dto.profileImageUrl)
            .addValue("birthDate", dto.birthDate)
            .addValue("weight", dto.weight)
            .addValue("height", dto.height)
            .addValue("gender", dto.gender?.name)
            .addValue("fitnessGoal", dto.fitnessGoal?.name)
            .addValue("activityLevel", dto.activityLevel?.name)
            .addValue("targetWeight", dto.targetWeight)
            .addValue("joinedDate", dto.joinedDate)
            .addValue("bio", dto.bio)
            .addValue("now", now)
            .addValue("newRevision", newRevision)
        jdbcTemplate.update(sql, params)
    }

    private fun mapRow(rs: ResultSet): UserProfileDto = UserProfileDto(
        uid = rs.getString("uid"),
        name = rs.getString("name"),
        email = rs.getString("email"),
        profileImageUrl = rs.getString("profile_image_url"),
        birthDate = rs.getNullableLong("birth_date"),
        weight = rs.getNullableFloat("weight"),
        height = rs.getNullableFloat("height"),
        gender = rs.getString("gender")?.let { Gender.valueOf(it) },
        fitnessGoal = rs.getString("fitness_goal")?.let { FitnessGoal.valueOf(it) },
        activityLevel = rs.getString("activity_level")?.let { ActivityLevel.valueOf(it) },
        targetWeight = rs.getNullableFloat("target_weight"),
        joinedDate = rs.getLong("joined_date"),
        bio = rs.getString("bio"),
        updatedAt = rs.getLong("updated_at"),
        deletedAt = rs.getNullableLong("deleted_at"),
        revision = rs.getInt("revision"),
    )
}

internal fun ResultSet.getNullableLong(column: String): Long? =
    getLong(column).takeUnless { wasNull() }

internal fun ResultSet.getNullableFloat(column: String): Float? =
    getFloat(column).takeUnless { wasNull() }
