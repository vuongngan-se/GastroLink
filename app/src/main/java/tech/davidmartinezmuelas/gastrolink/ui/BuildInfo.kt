package tech.davidmartinezmuelas.gastrolink.ui

import tech.davidmartinezmuelas.gastrolink.BuildConfig

data class BuildInfo(
    val versionName: String,
    val versionCode: Int,
    val gitSha: String,
    val buildTime: String
)

object BuildInfoProvider {
    fun current(): BuildInfo {
        return BuildInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            gitSha = BuildConfig.GIT_SHA,
            buildTime = BuildConfig.BUILD_TIME
        )
    }
}
