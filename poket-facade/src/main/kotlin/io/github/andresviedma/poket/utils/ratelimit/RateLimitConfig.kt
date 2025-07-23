package io.github.andresviedma.poket.utils.ratelimit

data class RateLimitConfig(
    val type: Map<String, RateLimitTypeConfig> = emptyMap(),
    val default: RateLimitTypeConfig = RateLimitTypeConfig()
) {
    fun getTypeConfig(typeId: String): RateLimitTypeConfig =
        type[typeId] ?: default
}

data class RateLimitTypeConfig(
    val maxEvents: Int = Int.MAX_VALUE,
    val slotTimeMillis: Long = 1,
    private val disabled: Boolean = false
) {
    // Active only if it is not disabled and some limit is defined
    val active: Boolean get() = maxEvents < Int.MAX_VALUE && !disabled
}
