package dev.bitspittle.racketeer.console.user

import dev.bitspittle.racketeer.console.command.commands.system.UserDataDir
import dev.bitspittle.racketeer.console.utils.encodeToYaml
import kotlinx.serialization.Serializable
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@Serializable
data class Settings(
    var admin: Admin = Admin(),
    var unlocks: Unlocks = Unlocks(),
    var features: Features = Features(),
) {
    @Serializable
    data class Admin(
        var showDebugInfo: Boolean = true,
        var enabled: Boolean = false,
    )

    @Serializable
    data class Unlocks(
        var buildings: Boolean = false,
        var discord: Boolean = false,
    )

    @Serializable
    data class Features(
        var buildings: Boolean = false,
    )

    fun setFrom(other: Settings) {
        admin = other.admin.copy()
        unlocks = other.unlocks.copy()
        features = other.features.copy()
    }
}

val Settings.inAdminModeAndShowDebugInfo get() = admin.enabled && admin.showDebugInfo

fun Settings.saveInto(userDataDir: UserDataDir) {
    val self = this
    userDataDir.pathForSettings().apply {
        parent.createDirectories()
        writeText(self.encodeToYaml())
    }
}