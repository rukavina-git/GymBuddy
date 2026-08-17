package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Base64

/**
 * Pure unit test, no Spring context - CursorCodec is deliberately a
 * small, self-contained encode/decode class (see its own header).
 */
class CursorCodecTest {

    private val codec = CursorCodec(jacksonObjectMapper())

    @Test
    fun `round-trips a Delta cursor`() {
        val encoded = codec.encode(Cursor.Delta(128374))
        assertEquals(Cursor.Delta(128374), codec.decode(encoded))
    }

    @Test
    fun `round-trips a Delta cursor at zero`() {
        val encoded = codec.encode(Cursor.Delta(0))
        assertEquals(Cursor.Delta(0), codec.decode(encoded))
    }

    @Test
    fun `round-trips a FullSync cursor`() {
        val encoded = codec.encode(Cursor.FullSync(42))
        assertEquals(Cursor.FullSync(42), codec.decode(encoded))
    }

    @Test
    fun `a Delta cursor and a FullSync cursor with the same numeric value decode to different variants`() {
        val delta = codec.encode(Cursor.Delta(7))
        val fullSync = codec.encode(Cursor.FullSync(7))
        assertNotEquals(delta, fullSync)
        assertEquals(Cursor.Delta(7), codec.decode(delta))
        assertEquals(Cursor.FullSync(7), codec.decode(fullSync))
    }

    @Test
    fun `different seq values encode differently`() {
        assertNotEquals(codec.encode(Cursor.Delta(1)), codec.encode(Cursor.Delta(2)))
    }

    @Test
    fun `encoded cursor is url-safe`() {
        val encoded = codec.encode(Cursor.Delta(Long.MAX_VALUE))
        assertEquals(encoded, java.net.URLEncoder.encode(encoded, "UTF-8"), "must not contain characters that need URL escaping")
    }

    @Test
    fun `a bare payload with no mode field defaults to Delta`() {
        val bare = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"v":1,"seq":9}""".toByteArray())
        assertEquals(Cursor.Delta(9), codec.decode(bare))
    }

    @Test
    fun `decoding garbage that is not valid base64 throws CursorDecodeException`() {
        assertThrows(CursorDecodeException::class.java) { codec.decode("not valid base64!!! @@@") }
    }

    @Test
    fun `decoding valid base64 that is not valid json throws CursorDecodeException`() {
        val notJson = Base64.getUrlEncoder().withoutPadding().encodeToString("not json at all".toByteArray())
        assertThrows(CursorDecodeException::class.java) { codec.decode(notJson) }
    }

    @Test
    fun `decoding a Delta payload missing the seq field throws CursorDecodeException`() {
        val missingSeq = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"v":1,"mode":"DELTA"}""".toByteArray())
        assertThrows(CursorDecodeException::class.java) { codec.decode(missingSeq) }
    }

    @Test
    fun `decoding a FullSync payload missing the offset field throws CursorDecodeException`() {
        val missingOffset = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"v":1,"mode":"FULL_SYNC"}""".toByteArray())
        assertThrows(CursorDecodeException::class.java) { codec.decode(missingOffset) }
    }

    @Test
    fun `decoding an unrecognised mode throws CursorDecodeException`() {
        val badMode = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"v":1,"mode":"SIDEWAYS","seq":5}""".toByteArray())
        assertThrows(CursorDecodeException::class.java) { codec.decode(badMode) }
    }

    @Test
    fun `decoding an unrecognised version throws CursorDecodeException`() {
        val futureVersion = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"v":2,"mode":"DELTA","seq":5}""".toByteArray())
        assertThrows(CursorDecodeException::class.java) { codec.decode(futureVersion) }
    }

    @Test
    fun `empty string throws CursorDecodeException`() {
        assertThrows(CursorDecodeException::class.java) { codec.decode("") }
    }
}
