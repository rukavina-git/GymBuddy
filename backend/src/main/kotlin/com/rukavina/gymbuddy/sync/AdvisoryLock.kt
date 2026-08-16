package com.rukavina.gymbuddy.sync

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

/**
 * Serialises every mutation for one user within the current transaction,
 * via pg_advisory_xact_lock(hashtext(userId)) - a session-level lock
 * that Postgres releases automatically at transaction end (commit or
 * rollback), so there is no leak path and nothing to remember to
 * unlock.
 *
 * Why this matters: change_log.seq is a BIGSERIAL, allocated at insert
 * time, not commit time. Two concurrent transactions for the same user
 * can commit out of sequence order - T1 takes seq 100, T2 takes 101, T2
 * commits first - and a pull that lands in that window sees 101 but not
 * 100, advances its cursor past both, and silently loses row 100
 * forever. Taking this lock as the first statement of every mutation
 * transaction for a user forces those transactions to serialise, which
 * makes seq monotonic per user - exactly what the pull cursor needs.
 *
 * Single implementation, called by every per-type sync service before
 * it reads or writes anything for that user.
 */
@Component
class AdvisoryLock(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun acquireForUser(userId: String) {
        jdbcTemplate.execute(
            "SELECT pg_advisory_xact_lock(hashtext(:userId))",
            MapSqlParameterSource("userId", userId),
        ) { ps -> ps.execute() }
    }
}
