package com.rukavina.gymbuddy.persistence

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.sql.ResultSet

/**
 * The Postgres JDBC driver hands back jsonb columns as their JSON text
 * on ResultSet.getString - no PGobject unwrapping needed - so a plain
 * Jackson read is enough to turn a `'["CHEST"]'::jsonb` column into a
 * List<String>, which callers then map onto the matching enum.
 */
internal fun ObjectMapper.readStringArrayColumn(rs: ResultSet, column: String): List<String> =
    readValue(rs.getString(column))
