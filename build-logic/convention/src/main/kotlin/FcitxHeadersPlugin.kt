import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.task

class FcitxHeadersPlugin : Plugin<Project> {

    companion object {
        const val INSTALL_TASK = "installFcitxHeaders"
        const val CLEAN_TASK = "cleanFcitxHeaders"
    }

    private val Project.headersInstallDir
        get() = file("build/headers")

    override fun apply(target: Project) {
        target.pluginManager.apply("cmake-dir")
        registerInstallTask(target)
        registerCleanTask(target)
    }

    private fun registerInstallTask(project: Project) {
        val installHeadersTask = project.task(INSTALL_TASK) {
            runAfterNativeConfigure(project)

            doLast {
                project.exec {
                    workingDir = project.cmakeDir
                    environment("DESTDIR", project.headersInstallDir.absolutePath)
                    commandLine("cmake", "--install", ".", "--component", "header")
                }
            }
        }

        project.extensions.configure<LibraryAndroidComponentsExtension> {
            onVariants {
                val variantName = it.name.capitalized()
                project.afterEvaluate {
                    project.tasks.findByName("prefab${variantName}ConfigurePackage")
                        ?.dependsOn(installHeadersTask)
                }
            }
            finalizeDsl {
                @Suppress("UnstableApiUsage")
                it.prefab.forEach { library ->
                    library.headers?.let { path -> project.file(path).mkdirs() }
                }
            }
        }
    }

    private fun registerCleanTask(project: Project) {
        project.task<Delete>(CLEAN_TASK) {
            delete(project.headersInstallDir)
        }.also {
            project.cleanTask.dependsOn(it)
        }
    }

}
