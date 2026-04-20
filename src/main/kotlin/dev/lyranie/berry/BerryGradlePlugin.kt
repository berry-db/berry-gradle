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

package dev.lyranie.berry

import dev.lyranie.berry.codegen.CodeGen
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension

class BerryGradlePlugin : Plugin<Project> {
    companion object {
        val LOGGER: Logger = LogManager.getLogger("berry-db")
    }

    override fun apply(target: Project) {
        val extension = target.extensions.create("Berry", BerryGradlePluginExtension::class.java)
        val outDir = target.layout.buildDirectory.dir("generated/berry").get()
        val generateTask = target.tasks.register("generate") {
            group = "Berry"
            doLast {
                val schemas = extension.schemaDir.getOrElse(project.layout.projectDirectory.dir("schemas"))
                val output = extension.outDir.getOrElse(outDir)

                CodeGen.generate(schemas.asFile, output.asFile)
            }
        }

        target.afterEvaluate {
            val output = extension.outDir.getOrElse(outDir)

            target.extensions.findByType(JavaPluginExtension::class.java)
                ?.sourceSets
                ?.findByName("main")
                ?.java
                ?.srcDir(output)

            target.tasks.matching { it.name == "compileKotlin" || it.name == "compileJava" }
                .configureEach { dependsOn(generateTask) }
        }
    }
}
