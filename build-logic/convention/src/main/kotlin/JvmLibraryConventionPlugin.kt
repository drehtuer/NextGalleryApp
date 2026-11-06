import net.drehtuer.nextgallery.buildlogic.COVERAGE_REPORT_TASK
import net.drehtuer.nextgallery.buildlogic.configureJacoco
import net.drehtuer.nextgallery.buildlogic.configureKotlinToolchain
import net.drehtuer.nextgallery.buildlogic.libs
import net.drehtuer.nextgallery.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.testing.jacoco.tasks.JacocoReport

/** Pure Kotlin/JVM modules (no Android dependency, D25). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("nextgallery.detekt")
        pluginManager.apply("nextgallery.ktlint")
        configureKotlinToolchain()
        configureJacoco()
        tasks.withType(JacocoReport::class.java).configureEach {
            dependsOn("test")
            reports.xml.required.set(true)
        }
        tasks.register(COVERAGE_REPORT_TASK) { dependsOn("jacocoTestReport") }

        dependencies {
            add("implementation", libs.library("javax-inject"))
            add("testImplementation", libs.library("junit4"))
            add("testImplementation", libs.library("mockk"))
            add("testImplementation", libs.library("turbine"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
        }
    }
}
