import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.api.AndroidBasePlugin
import java.io.ByteArrayOutputStream
import java.util.Date
import java.text.SimpleDateFormat
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Internal
import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.lsplugin.cmaker)
}

cmaker {
    default {
        arguments.addAll(
            arrayOf(
                "-DANDROID_STL=none",
            )
        )
        abiFilters("arm64-v8a")
    }
    buildTypes {
        if (it.name == "release") {
            arguments += "-DDEBUG_SYMBOLS_PATH=${layout.buildDirectory.asFile.get().absolutePath}/symbols"
        }
    }
}

val androidMinSdkVersion = 26
val androidTargetSdkVersion = 36
val androidCompileSdkVersion = 36
val androidCompileNdkVersion = "29.0.14206865"
val androidSourceCompatibility = JavaVersion.VERSION_21
val androidTargetCompatibility = JavaVersion.VERSION_21
val managerVersionCode by extra(getVersionCode())
val managerVersionName by extra(getVersionName())
val buildDate by extra(getDate())

abstract class GitInfo @Inject constructor(
    private val execOps: ExecOperations
) {
    fun getGitCommitCount(): Int {
        val out = ByteArrayOutputStream()
        execOps.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = out
        }
        return out.toString().trim().toInt()
    }
}

fun getVersionCode(): Int {
	val gitInfo = objects.newInstance(GitInfo::class.java)
    val commitCount = gitInfo.getGitCommitCount()
    val major = 1
    return major * 10000 + commitCount + 200
}

fun getVersionName(): String {
    return "v2.1.5"
}

fun getDate(): String {
    val date = SimpleDateFormat("yyyyMMdd").format(Date())
    val hours = SimpleDateFormat("hhmmss").format(Date())
    return "$date$hours"
}

subprojects {
    plugins.withType(AndroidBasePlugin::class.java) {
        extensions.configure(CommonExtension::class.java) {
            compileSdk = androidCompileSdkVersion
            ndkVersion = androidCompileNdkVersion

            defaultConfig {
                minSdk = androidMinSdkVersion
                if (this is ApplicationDefaultConfig) {
                    targetSdk = androidTargetSdkVersion
                    versionCode = managerVersionCode
                    versionName = managerVersionName
                }
                ndk {
                    abiFilters += listOf("arm64-v8a")
                }
            }

            lint {
                abortOnError = true
                checkReleaseBuilds = false
            }

            compileOptions {
                sourceCompatibility = androidSourceCompatibility
                targetCompatibility = androidTargetCompatibility
            }
        }
    }
}