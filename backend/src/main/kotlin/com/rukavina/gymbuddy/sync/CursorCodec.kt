package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.util.Base64

/**
 * The decoded form of an opaque pull cursor. Two variants, both
 * present in the wire format as an explicit "mode" field rather than a
 * sentinel encoded into a shared numeric field - see CursorCodec's
 * header for why.
 */
sealed class Cursor {
    /** Resume from this change_log position - the ordinary delta-pull case. */
    data class Delta(val seq: Long) : Cursor()

    /** Resume an in-progress full sync (point 4) at this many entities already returned, in FullSyncEntityDiscovery's (type, id) order. */
    data class FullSync(val offset: Long) : Cursor()
}

/** Thrown by CursorCodec.decode() for a structurally malformed cursor. Maps to 400 VALIDATION_FAILED - see GlobalExceptionHandler. */
class CursorDecodeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * All fields nullable, deliberately not the non-null types the wire
 * format implies: jackson-module-kotlin's handling of a missing field
 * on a non-null Kotlin constructor parameter depends on the injected
 * ObjectMapper's strictNullChecks setting, which this class doesn't
 * control and shouldn't have to reason about. Nullable fields plus an
 * explicit presence check in decode() make "field absent" fail the
 * same way regardless of how the app's Jackson happens to be
 * configured.
 */
private data class CursorPayload(val v: Int?, val mode: String?, val seq: Long?, val offset: Long?)

/**
 * Encodes/decodes the opaque pull cursor:
 *
 *   base64url(json({"v":1,"mode":"DELTA","seq":128374}))
 *   base64url(json({"v":1,"mode":"FULL_SYNC","offset":42}))
 *
 * "v" is a format version so the internal representation can change
 * later without breaking clients already holding an older cursor - any
 * version this build doesn't recognise decodes as malformed rather than
 * guessed at. "mode" is an explicit discriminator between the two kinds
 * of position a cursor can resume from (see the Cursor sealed class) -
 * not a sign bit or other implicit encoding folded into "seq".
 *
 * An implicit encoding was considered and rejected: a negative "seq"
 * could distinguish full-sync continuation from a delta position
 * without adding a field, since BIGSERIAL change_log.seq is always
 * >= 1. It was rejected because the two things it would justify -
 * "don't add a field" and "keep the payload opaque" - don't actually
 * hold up. The payload is already versioned JSON with room to spare;
 * a field costs nothing here. And opacity is a constraint on what the
 * CLIENT does with this string (never parse, construct, or compare it -
 * enforced by convention and code review on the client, not by making
 * the server's own internal shape harder to read) - it was never a
 * property of the payload's shape, so hiding "which kind of position
 * this is" behind a sign bit didn't buy any actual opacity back. What
 * it did cost: a magic-value convention only visible in code, not in a
 * decoded payload; half the numeric range quietly reserved; and two
 * unrelated concerns - is this a delta position or a full-sync offset -
 * coupled into one field's sign instead of two things a reader can see
 * are different. The explicit "mode" field costs one more short string
 * per cursor and removes all three problems, so a self-documenting
 * field wins on every axis "add nothing" was supposed to protect.
 *
 * One small class with its own tests (CursorCodecTest) - deliberately
 * ignorant of what "seq"/"offset" mean to a caller beyond their own
 * variant; interpreting a Cursor is a pull-orchestration concern (see
 * SyncPullOrchestrator), not a cursor-encoding one.
 *
 * Clients must still never parse, construct, or compare this string -
 * "opaque" describes the client contract, not the server's freedom to
 * choose an internal representation.
 */
@Component
class CursorCodec(private val objectMapper: ObjectMapper) {

    companion object {
        private const val CURRENT_VERSION = 1
        private const val MODE_DELTA = "DELTA"
        private const val MODE_FULL_SYNC = "FULL_SYNC"
    }

    fun encode(cursor: Cursor): String {
        val payload = when (cursor) {
            is Cursor.Delta -> CursorPayload(CURRENT_VERSION, MODE_DELTA, cursor.seq, null)
            is Cursor.FullSync -> CursorPayload(CURRENT_VERSION, MODE_FULL_SYNC, null, cursor.offset)
        }
        val bytes = objectMapper.writeValueAsBytes(payload)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** @throws CursorDecodeException if raw isn't valid base64url, valid JSON, missing a required field, or a recognised version/mode. */
    fun decode(raw: String): Cursor {
        val payload = try {
            val bytes = Base64.getUrlDecoder().decode(raw)
            objectMapper.readValue(bytes, CursorPayload::class.java)
        } catch (e: Exception) {
            throw CursorDecodeException("Malformed cursor.", e)
        }
        val version = payload.v ?: throw CursorDecodeException("Cursor is missing its version field.")
        if (version != CURRENT_VERSION) {
            throw CursorDecodeException("Unrecognised cursor version: $version")
        }
        return when (payload.mode) {
            // Absent mode is treated as DELTA so a bare {"v":1,"seq":N}
            // payload - the shape before full-sync continuation needed
            // its own variant - still decodes; costs nothing to keep.
            null, MODE_DELTA ->
                Cursor.Delta(payload.seq ?: throw CursorDecodeException("Cursor is missing its seq field."))
            MODE_FULL_SYNC ->
                Cursor.FullSync(payload.offset ?: throw CursorDecodeException("Cursor is missing its offset field."))
            else ->
                throw CursorDecodeException("Unrecognised cursor mode: ${payload.mode}")
        }
    }
}
