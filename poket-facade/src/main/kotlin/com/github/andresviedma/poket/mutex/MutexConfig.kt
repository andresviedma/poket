package com.github.andresviedma.poket.mutex

import com.github.andresviedma.poket.mutex.MutexTypeConfig.Companion.DEFAULTS

data class MutexConfig(
    val type: Map<String, MutexTypeConfig> = emptyMap(),
    val default: MutexTypeConfig = MutexTypeConfig()
) {
    /**
     * Returns the config for the given type, or the default if not defined.
     * Types can be defined in a hierarchical way using "::" as separator.
     * For example, if we request "cache::mylock", for any config property it will return the value in
     * "cache::mylock" config if it is defined, and otherwise will return "cache" after that
     * (or the default if it is not defined either).
     */
    fun getTypeConfig(type: String, baseTypeConfig: MutexTypeConfig? = null): MutexTypeConfig =
        type.typeSubparts().fold(
            DEFAULTS.overriddenWith(default)
                .overriddenWith(baseTypeConfig)
        ) { config, partType ->
            config.overriddenWith(this.type[partType])
        }

    private fun String.typeSubparts(): List<String> =
        split("::").let { pieces ->
            List(pieces.size) { index -> pieces.slice(0..index).joinToString("::") }
        }
}

data class MutexTypeConfig(
    val lockSystem: String? = null,
    val timeoutInMillis: Int? = null,
    val ttlInMillis: Int? = null,
    val onLockSystemError: MutexOnErrorAction? = null,
    val fallbackLockSystem: String? = null,
    val failOnLockReleaseError: Boolean? = null
) {
    fun overriddenWith(typeOverride: MutexTypeConfig?) = MutexTypeConfig(
        lockSystem = typeOverride?.lockSystem ?: lockSystem,
        timeoutInMillis = typeOverride?.timeoutInMillis ?: timeoutInMillis,
        ttlInMillis = typeOverride?.ttlInMillis ?: ttlInMillis,
        onLockSystemError = typeOverride?.onLockSystemError ?: onLockSystemError,
        fallbackLockSystem = typeOverride?.fallbackLockSystem ?: fallbackLockSystem,
        failOnLockReleaseError = typeOverride?.failOnLockReleaseError ?: failOnLockReleaseError
    )

    companion object {
        val DEFAULTS = MutexTypeConfig(
            lockSystem = "syncdb",
            timeoutInMillis = 10000,
            ttlInMillis = 10000,
            onLockSystemError = MutexOnErrorAction.FAIL,
            failOnLockReleaseError = false
        )
    }
}

enum class MutexOnErrorAction {
    /** On error accessing the lock system, throw an exception */
    FAIL,

    /** Continue as if the lock was obtained */
    GET,

    /** Get the lock with the fallbackLockSystem (if not null) */
    FALLBACK
}
