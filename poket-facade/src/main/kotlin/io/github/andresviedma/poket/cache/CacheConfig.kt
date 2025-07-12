package io.github.andresviedma.poket.cache

import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

data class CacheConfig(
    val type: Map<String, io.github.andresviedma.poket.cache.CacheTypeConfig> = emptyMap(),
    val default: io.github.andresviedma.poket.cache.CacheTypeConfig = io.github.andresviedma.poket.cache.CacheTypeConfig()
) {
    fun getTypeConfig(type: String, defaultTypeConfig: io.github.andresviedma.poket.cache.CacheTypeConfig?): io.github.andresviedma.poket.cache.CacheTypeConfig =
        io.github.andresviedma.poket.cache.CacheTypeConfig.Companion.DEFAULTS
            .overriddenWith(default)
            .overriddenWith(defaultTypeConfig)
            .overriddenWith(this.type[type])
}

data class CacheTypeConfig(
    /** Cache implementation to use */
    val cacheSystem: String? = null,

    /** Max TTL in seconds of the values in the cache */
    val ttlInSeconds: Long? = null,

    /** Version of the cache, can be used to invalidate all elements in the cache when needed */
    val version: String? = null,

    /** If true, parallel requests will be collapsed, waiting for first request to finish instead of making the call twice */
    val requestCollapsing: Boolean? = null,

    /** Optional namespace of the values in the cache, especially important for shared cache servers when we don't want to mix different services data */
    val distributedComponentNamespace: String? = null,

    /** If true this cache will not be used */
    val disabled: Boolean? = null,

    /**
     * For load testing: rolling factor to reduce cache efficiency. null for production usage.
     * When > 1, the cache will have an expected hit ratio of 1 / rollingFactor over normal usage.
     */
    val loadTestRollingFactor: Int? = null,

    /** If true, when there is an error reading an element from the underlying cache system, an exception will be thrown */
    val failOnGetError: Boolean? = null,

    /** If true, when there is an error writing an element to the underlying cache system, an exception will be thrown */
    val failOnPutError: Boolean? = null,

    /** If true, when there is an error invalidating an element in the underlying cache system, an exception will be thrown */
    val failOnInvalidateError: Boolean? = null,

    /** If not null, after the given seconds the current value will be returned but updated asynchronously (if using getOrPut) */
    val outdateTimeInSeconds: Int? = null
) {
    fun namespace(type: String, serializationVersion: String): String =
        listOfNotNull(distributedComponentNamespace, type, serializationVersion, version, rollingParameter())
            .joinToString("-")

    fun isUpdatableAsynchronously() =
        outdateTimeInSeconds != null

    fun isOutdated(generationTime: Long?, now: Instant): Boolean =
        isUpdatableAsynchronously() && (
            generationTime == null ||
                    (now - Instant.fromEpochMilliseconds(generationTime)).inWholeSeconds > outdateTimeInSeconds!!.toLong()
            )

    fun overriddenWith(typeOverride: io.github.andresviedma.poket.cache.CacheTypeConfig?) =
        io.github.andresviedma.poket.cache.CacheTypeConfig(
            cacheSystem = typeOverride?.cacheSystem ?: cacheSystem,
            ttlInSeconds = typeOverride?.ttlInSeconds ?: ttlInSeconds,
            version = typeOverride?.version ?: version,
            requestCollapsing = typeOverride?.requestCollapsing ?: requestCollapsing,
            distributedComponentNamespace = typeOverride?.distributedComponentNamespace
                ?: distributedComponentNamespace,
            disabled = typeOverride?.disabled ?: disabled,
            loadTestRollingFactor = typeOverride?.loadTestRollingFactor ?: loadTestRollingFactor,
            failOnGetError = typeOverride?.failOnGetError ?: failOnGetError,
            failOnPutError = typeOverride?.failOnPutError ?: failOnPutError,
            failOnInvalidateError = typeOverride?.failOnInvalidateError ?: failOnInvalidateError,
            outdateTimeInSeconds = typeOverride?.outdateTimeInSeconds ?: outdateTimeInSeconds
        )

    private fun rollingParameter(): String? =
        loadTestRollingFactor?.let { "r" + (1..it).random() }

    companion object {
        val DEFAULTS = io.github.andresviedma.poket.cache.CacheTypeConfig(
            cacheSystem = "memory",
            ttlInSeconds = 5.minutes.inWholeSeconds,
            requestCollapsing = true,
            failOnGetError = false,
            failOnPutError = false,
            failOnInvalidateError = false
        )
    }
}
