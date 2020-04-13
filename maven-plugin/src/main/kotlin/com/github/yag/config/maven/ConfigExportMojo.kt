package com.github.yag.config.maven

import com.github.yag.config.ExportConfig.export
import com.github.yag.config.Profile
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.io.File
import java.io.PrintStream
import java.net.URL
import java.net.URLClassLoader

@Mojo(name = "export", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
class ConfigExportMojo : AbstractMojo() {

    @Parameter
    private lateinit var outputPath: String

    @Parameter
    private lateinit var configClass: String

    @Parameter(property = "project", readonly = true)
    private lateinit var project: MavenProject

    override fun execute() {
        val classLoader = URLClassLoader(
            ArrayList<URL>().apply {
                addAll(project.testClasspathElements.map {
                    File(it as String).toURI().toURL()
                })

            }.toTypedArray(),
            Thread.currentThread().contextClassLoader
        )

        File(outputPath).outputStream().use {
            val clazz = classLoader.loadClass(configClass)
            if (Profile::class.java.isAssignableFrom(clazz)) {
                val config = (clazz.getDeclaredConstructor().newInstance() as Profile).getConfigProfile()
                val configClass = config.javaClass
                export(configClass, config, PrintStream(it))
            } else {
                export(clazz, out = PrintStream(it))
            }
        }
    }

}