import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * This Gradle script captures the system properties available to each Test task, hashes the properties' values,
 * and adds these as custom values.
 * This should be applied to the root project since it configures all projects:
 * <code> apply from: file('gradle-test-execution-system-properties.gradle.kts') </code>
 */
val MESSAGE_DIGEST = MessageDigest.getInstance("SHA-256")

project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        allprojects {
            tasks.withType<Test> {
                doFirst {
                    systemProperties.forEach { (k, v) -> value("${identityPath}#sysProps-${k}", hash(v)) }
                }
            }
        }
    }
}

fun hash(value: Any?): String? {
    if (value == null) {
        return null
    } else {
        val str = java.lang.String.valueOf(value)
        val encodedHash = MESSAGE_DIGEST.digest(str.toByteArray())
        val hexString = StringBuilder()
        for (i in 0 until (encodedHash.size / 4)) {
            val hex = java.lang.Integer.toHexString(0xff and encodedHash[i].toInt())
            if (hex.length == 1) {
                hexString.append("0")
            }
            hexString.append(hex)
        }
        hexString.append("...")
        return hexString.toString()
    }
}
