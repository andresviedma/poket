package io.github.andresviedma.poket.utils.retry

data class RetryProfileConfig(
    val profiles: Map<String, RetryPolicyConfig> = emptyMap(),
    val default: RetryPolicyConfig = RetryPolicyConfig(enabled = false)
) {
    /**
     * Returns the config for the given profile, or the default if not defined.
     * Profiles can be defined in a hierarchical way using "." as separator.
     * For example, if we request "cpq.myevent" it will return "cpq.myevent" if it is defined
     * in the config, and otherwese will return "cpq" after that (or the default if it is not defined either).
     */
    fun getProfileConfig(profile: String?): RetryPolicyConfig =
        profile?.profileSubparts()?.mapNotNull { profiles[it] }?.firstOrNull()
            ?: default

    private fun String.profileSubparts(): List<String> =
        split(".")
            .let { pieces ->
                pieces.mapIndexed { index, _ -> pieces.slice(0..index).joinToString(".") }
            }.reversed()
}

data class RetryPolicyConfig(
    val retries: List<Long> = emptyList(),
    val enabled: Boolean = true
) {
    constructor(vararg retryArgs: Long) : this(retryArgs.toList())

    val maxRetries: Int get() = effectiveRetries.size
    private val effectiveRetries: List<Long> get() = if (enabled) retries else emptyList()
}
