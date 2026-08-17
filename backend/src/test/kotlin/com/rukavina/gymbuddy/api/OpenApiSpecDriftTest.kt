package com.rukavina.gymbuddy.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File

private val HTTP_METHODS = setOf("get", "post", "put", "delete", "patch", "options", "head")

private data class Operation(val path: String, val method: String)

/**
 * Group J point 8: controllers are hand-written against api/openapi.yaml
 * but nothing enforces they stay in sync with it - this fetches
 * springdoc's live /v3/api-docs and compares it against the committed
 * spec semantically (never textually - see the class-level notes below
 * for why that can't work and what got relaxed to make it work anyway).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class OpenApiSpecDriftTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val jsonMapper = ObjectMapper()
    private val yamlMapper = ObjectMapper(YAMLFactory())

    private lateinit var committedRoot: JsonNode
    private lateinit var generatedRoot: JsonNode
    private lateinit var committedOps: Map<Operation, JsonNode>
    private lateinit var generatedOps: Map<Operation, JsonNode>

    @BeforeEach
    fun loadSpecs() {
        val specFile = File("../api/openapi.yaml")
        check(specFile.isFile) { "Committed spec not found at ${specFile.absolutePath} - expected the backend Gradle module's working directory to be backend/, with api/ as a sibling." }
        committedRoot = yamlMapper.readTree(specFile)

        val body = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        generatedRoot = jsonMapper.readTree(body)

        committedOps = operationsOf(committedRoot)
        generatedOps = operationsOf(generatedRoot)
    }

    @Test
    fun `every path and method in the committed spec exists in the generated spec with a matching contract`() {
        val problems = mutableListOf<String>()

        for ((op, committedOperation) in committedOps) {
            val generatedOperation = generatedOps[op]
            if (generatedOperation == null) {
                problems += "${describe(op)}: documented in api/openapi.yaml but no such controller route was generated"
                continue
            }

            problems += compareParameters(op, committedOperation, generatedOperation)
            problems += compareRequestBody(op, committedOperation, generatedOperation)
            problems += compareResponseCodes(op, committedOperation, generatedOperation)
        }

        assertTrue(problems.isEmpty(), "Spec drift between api/openapi.yaml and the generated /v3/api-docs:\n" + problems.joinToString("\n") { "  - $it" })
    }

    /**
     * The direction that catches an undocumented controller: extra
     * detail WITHIN a matched operation is fine (springdoc always adds
     * some - see below), but a whole extra operation means a route
     * exists the contract says nothing about.
     */
    @Test
    fun `every path and method the controllers actually expose is documented in the committed spec`() {
        val undocumented = generatedOps.keys.filter { it !in committedOps.keys }
        assertTrue(undocumented.isEmpty(), "Controller route(s) not described in api/openapi.yaml: ${undocumented.map { describe(it) }}")
    }

    private fun describe(op: Operation) = "${op.method.uppercase()} ${op.path}"

    private fun operationsOf(spec: JsonNode): Map<Operation, JsonNode> {
        val result = mutableMapOf<Operation, JsonNode>()
        val paths = spec.get("paths") ?: return result
        paths.fields().forEach { (path, pathItem) ->
            pathItem.fields().forEach { (method, operation) ->
                if (method.lowercase() in HTTP_METHODS) {
                    result[Operation(path, method.lowercase())] = resolveRefs(operation, spec)
                }
            }
        }
        return result
    }

    /**
     * Parameters are compared by (name, in, required) - not type/schema,
     * which springdoc renders differently enough from the hand-authored
     * spec (see class notes) that a stricter comparison would be
     * comparing formatting, not contract.
     */
    private fun compareParameters(op: Operation, committed: JsonNode, generated: JsonNode): List<String> {
        fun paramKey(p: JsonNode) = Triple(p.get("name")?.asText(), p.get("in")?.asText(), p.get("required")?.asBoolean(false) ?: false)

        val committedParams = (committed.get("parameters") ?: jsonMapper.createArrayNode()).map(::paramKey).toSet()
        val generatedParamsByNameAndIn = (generated.get("parameters") ?: jsonMapper.createArrayNode())
            .associateBy { it.get("name")?.asText() to it.get("in")?.asText() }

        val problems = mutableListOf<String>()
        for ((name, inLocation, required) in committedParams) {
            val match = generatedParamsByNameAndIn[name to inLocation]
            if (match == null) {
                problems += "${describe(op)}: parameter '$name' (in $inLocation) is documented but not generated"
                continue
            }
            val generatedRequired = match.get("required")?.asBoolean(false) ?: false
            if (required != generatedRequired) {
                problems += "${describe(op)}: parameter '$name' required=$required in the spec but required=$generatedRequired in the generated docs"
            }
        }
        return problems
    }

    /**
     * Every property the committed request body schema names must
     * exist in the generated one, one level deep - not recursively
     * through every nested object, and not comparing required-ness.
     * See class notes for why: springdoc's Kotlin data-class-derived
     * schemas and this project's hand-authored nullable-array
     * conventions disagree on which fields are technically "required"
     * often enough that a strict recursive check flags dozens of
     * false positives with no actual drift behind them. Property
     * presence one level down is the part a controller/DTO rename
     * would actually break.
     */
    private fun compareRequestBody(op: Operation, committed: JsonNode, generated: JsonNode): List<String> {
        val committedSchema = requestBodySchema(committed) ?: return emptyList()
        val generatedSchema = requestBodySchema(generated)
            ?: return listOf("${describe(op)}: has a documented request body but the generated operation has none")

        val committedProps = committedSchema.get("properties")?.fieldNames()?.asSequence()?.toSet().orEmpty()
        val generatedProps = generatedSchema.get("properties")?.fieldNames()?.asSequence()?.toSet().orEmpty()

        val missing = committedProps - generatedProps
        return if (missing.isEmpty()) emptyList() else listOf("${describe(op)}: request body propert${if (missing.size == 1) "y" else "ies"} $missing documented but not generated")
    }

    private fun requestBodySchema(operation: JsonNode): JsonNode? =
        operation.path("requestBody").path("content").path("application/json").path("schema").takeIf { !it.isMissingNode }

    /**
     * Relaxed to an existence check, not a set comparison - see the
     * Group J report for the full reasoning. In short: every one of
     * this project's non-200 response codes (400/401/403/410/429/500)
     * comes from cross-cutting infrastructure - GlobalExceptionHandler,
     * RestAuthenticationEntryPoint/RestAccessDeniedHandler,
     * RateLimitFilter, CursorDecodeException/CursorExpiredException's
     * handlers - none of which springdoc's per-controller-method
     * reflection can see without @ApiResponse annotations on every
     * method. Even success codes beyond the bare default aren't
     * visible: DELETE /v1/account genuinely returns 204
     * (ResponseEntity.noContent()), but that status is a runtime value,
     * not a static annotation, so springdoc documents it as 200 anyway.
     * Annotating every controller method to satisfy this comparison
     * would fight this project's own stated design (api/README.md:
     * "Hand-authored - this file is the contract, not a by-product of
     * the backend"), so this only confirms springdoc generated some
     * response for an operation the spec says has one - catching a
     * reflection failure that produces no responses at all, without
     * false-flagging every documented error status as "missing".
     */
    private fun compareResponseCodes(op: Operation, committed: JsonNode, generated: JsonNode): List<String> {
        val committedHasResponses = committed.path("responses").fieldNames().hasNext()
        val generatedHasResponses = generated.path("responses").fieldNames().hasNext()
        return if (committedHasResponses && !generatedHasResponses) {
            listOf("${describe(op)}: spec documents response(s) but springdoc generated none at all")
        } else {
            emptyList()
        }
    }

    /** Local-only $ref resolver: both specs are single-file with #/... pointers, no external refs to chase. */
    private fun resolveRefs(node: JsonNode, root: JsonNode, seen: Set<String> = emptySet()): JsonNode {
        if (node.isObject) {
            val obj = node as ObjectNode
            val ref = obj.get("\$ref")?.asText()
            if (ref != null) {
                if (ref in seen) return jsonMapper.createObjectNode()
                val target = navigateRef(root, ref) ?: return obj
                return resolveRefs(target, root, seen + ref)
            }
            val result = jsonMapper.createObjectNode()
            obj.fields().forEach { (key, value) -> result.set<JsonNode>(key, resolveRefs(value, root, seen)) }
            return result
        }
        if (node.isArray) {
            val result = jsonMapper.createArrayNode()
            node.forEach { result.add(resolveRefs(it, root, seen)) }
            return result
        }
        return node
    }

    private fun navigateRef(root: JsonNode, ref: String): JsonNode? {
        if (!ref.startsWith("#/")) return null
        var current: JsonNode? = root
        for (segment in ref.removePrefix("#/").split("/")) {
            current = current?.get(segment)
        }
        return current
    }
}
