import androidx.room.gradle.RoomExtension
import com.android.build.api.dsl.CommonExtension
import net.drehtuer.nextgallery.buildlogic.libs
import net.drehtuer.nextgallery.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room")

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }
        // The Room plugin only adds the schemas to instrumented-test assets; Robolectric migration tests need them too.
        (extensions.getByName("android") as CommonExtension).sourceSets.getByName("test").assets.srcDir("$projectDir/schemas")

        dependencies {
            add("implementation", libs.library("androidx-room-runtime"))
            add("implementation", libs.library("androidx-room-paging"))
            add("ksp", libs.library("androidx-room-compiler"))
            add("testImplementation", libs.library("androidx-room-testing"))
        }
    }
}
