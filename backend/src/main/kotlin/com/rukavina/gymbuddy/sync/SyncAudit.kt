package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.domain.EntityType
import com.rukavina.gymbuddy.domain.SyncStatus
import org.slf4j.Logger

/**
 * One structured line per mutation attempt - the audit trail for
 * "what happened to entity X", including CONFLICT/INVALID/FORBIDDEN
 * results, which never touch change_log. Every per-type sync service
 * calls this exactly once per entity, so the log format can't drift
 * between them. Deliberately built now, before an incident makes it
 * needed (see Group F's design notes, point 12).
 */
object SyncAudit {

    fun log(
        logger: Logger,
        traceId: String,
        uid: String,
        entityType: EntityType,
        entityId: String,
        status: SyncStatus,
        revisionBefore: Int?,
        revisionAfter: Int?,
        reason: String? = null,
    ) {
        logger.info(
            "sync push [traceId={}] uid={} entityType={} entityId={} status={} revisionBefore={} revisionAfter={}{}",
            traceId, uid, entityType, entityId, status, revisionBefore, revisionAfter,
            if (reason != null) " reason=\"$reason\"" else "",
        )
    }
}
