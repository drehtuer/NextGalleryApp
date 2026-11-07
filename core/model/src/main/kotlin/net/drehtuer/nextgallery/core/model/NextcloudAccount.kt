package net.drehtuer.nextgallery.core.model

/** The signed-in account. [appPassword] is a secret: it never appears in [toString], and so never in a log. */
data class NextcloudAccount(
    val serverUrl: String,
    val loginName: String,
    val userId: String,
    val appPassword: String,
) {
    override fun toString(): String =
        "NextcloudAccount(serverUrl=$serverUrl, loginName=$loginName, userId=$userId, appPassword=<redacted>)"
}
