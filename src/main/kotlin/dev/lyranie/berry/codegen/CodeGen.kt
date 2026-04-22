/*
 * Copyright (c) 2026 lyranie
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.lyranie.berry.codegen

import dev.lyranie.berry.BerryGradlePlugin
import dev.lyranie.berry.parser.SchemaParser
import dev.lyranie.berry.parser.exception.UnsupportedTypeException
import dev.lyranie.berry.parser.schema.Field
import dev.lyranie.berry.parser.schema.Parameter
import dev.lyranie.berry.parser.schema.Table
import dev.lyranie.berry.parser.schema.Type
import org.gradle.api.GradleException
import java.io.File

@Suppress("TooGenericExceptionCaught")
object CodeGen {
    fun generate(schemaFolder: File, outputDir: File) {
        if (!schemaFolder.exists()) {
            throw GradleException("Unable to find schema directory: ${schemaFolder.path}")
        }

        if (!outputDir.exists()) {
            BerryGradlePlugin.LOGGER.warn("Unable to find output directory: ${outputDir.path}, creating...")
            outputDir.mkdirs()
        }

        schemaFolder.listFiles().orEmpty().forEach { file ->
            generateDatabase(file, outputDir)
        }
    }

    private fun generateDatabase(file: File, outputDir: File) {
        if (file.extension != "bdbs") {
            BerryGradlePlugin.LOGGER.info("File doesn't match schema extension: ${file.name}, skipping...")
            return
        }

        BerryGradlePlugin.LOGGER.info("Generating classes for schema: ${file.name}")

        val schema = try {
            SchemaParser.parse(file)
        } catch (e: Exception) {
            val message = "Unable to parse schema file: ${file.name}"

            BerryGradlePlugin.LOGGER.error(message, e)
            throw GradleException(e.message ?: message, e)
        }

        File(outputDir, "${file.nameWithoutExtension}.kt").writeText(
            buildString {
                appendLine("// Berry - Generated file")
                appendLine()
                appendLine("package dev.lyranie.berry.table")
                appendLine()
                appendLine("import dev.lyranie.berry.parser.annotation.Primary")
                appendLine()
                schema.tables.forEach { table ->
                    generateTable(table, this)
                }
            }
        )
    }

    private fun generateTable(table: Table, builder: StringBuilder) {
        builder.appendLine("data class ${table.name}(")

        table.fields.forEach { field ->
            generateField(field, builder)
        }

        builder.appendLine(")")
        builder.appendLine()
    }

    private fun generateField(field: Field, builder: StringBuilder) {
        if (field.parameters.contains(Parameter.Primary)) {
            builder.appendLine("    @Primary")
        }
        builder.appendLine("    val ${field.name}: ${generateType(field.type)},")
    }

    private fun generateType(type: Type): String = when (type) {
        Type.STRING -> "String"
        Type.NUMBER -> "Int"
        Type.BOOLEAN -> "Boolean"
        is Type.Reference -> "${type.name}?"
        is Type.List -> "List<${type.name}?>"
        else -> throw UnsupportedTypeException(type::class)
    }
}
