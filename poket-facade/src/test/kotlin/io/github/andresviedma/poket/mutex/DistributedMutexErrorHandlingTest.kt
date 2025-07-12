package io.github.andresviedma.poket.mutex

import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.config.ConstantConfigSource
import io.github.andresviedma.poket.config.configWith
import io.github.andresviedma.poket.mutex.local.LocalLockSystem
import com.github.andresviedma.trekkie.*
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import java.time.Duration

class DistributedMutexErrorHandlingTest : FeatureSpec({
    fun errorLockSystem(id: String, errorOnLock: Boolean, errorOnRelease: Boolean) = object : LockSystem {
        override fun getId(): String = id
        override suspend fun waitLock(name: String, timeout: Duration, ttl: Duration): LockContext {
            if (errorOnLock) error("boom") else return LockContext(hasLock = true)
        }
        override suspend fun getLockIfFree(name: String, ttl: Duration): LockContext {
            if (errorOnLock) error("boom") else return LockContext(hasLock = true)
        }
        override suspend fun releaseLock(name: String, lockContext: LockContext): Boolean {
            if (errorOnRelease) error("boom") else return true
        }
    }

    val workingLockSystem = LocalLockSystem()
    val errorLockSystem = errorLockSystem("error-all", errorOnLock = true, errorOnRelease = true)
    val errorLockSystem2 = errorLockSystem("error-all2", errorOnLock = true, errorOnRelease = true)
    val errorReleaseLockSystem = errorLockSystem("error-release", errorOnLock = false, errorOnRelease = true)
    val lockSystems = LockSystemProvider(
        setOf(workingLockSystem, errorLockSystem, errorLockSystem2, errorReleaseLockSystem)
    )

    val mutexSettings = MutexTypeConfig(
        lockSystem = errorLockSystem.getId()
    )
    val mutexConfig = configWith(MutexConfig(default = mutexSettings))

    val mutex = DistributedMutex(lockSystems, mutexConfig, "test-mutex")
    val mutexForceIgnoringErrors = DistributedMutex(
        lockSystems,
        mutexConfig,
        "test-mutex",
        forceIgnoreLockErrors = true
    )

    fun ConfigProvider.withDefault(typeConfig: MutexTypeConfig) =
        source<ConstantConfigSource>()!!.override(MutexConfig(default = typeConfig))

    feature("synchronized with error getting lock") {
        scenario("onLockSystemError = FAIL") {
            Given(mutexConfig) {
                withDefault(MutexTypeConfig(lockSystem = "error-all", onLockSystemError = MutexOnErrorAction.FAIL))
            }
            When {
                mutex.synchronized { "ok" }
            } thenExceptionThrown(IllegalStateException::class)
        }

        scenario("onLockSystemError = GET") {
            Given(mutexConfig) {
                withDefault(MutexTypeConfig(lockSystem = "error-all", onLockSystemError = MutexOnErrorAction.GET))
            }
            When {
                mutex.synchronized { "ok" }
            } then {
                it shouldBe "ok"
            }
        }

        scenario("onLockSystemError = FALLBACK with working fallback") {
            Given(mutexConfig) {
                withDefault(
                    MutexTypeConfig("error-all", onLockSystemError = MutexOnErrorAction.FALLBACK, fallbackLockSystem = "local")
                )
            }
            When {
                mutex.synchronized { "ok" }
            } then {
                it shouldBe "ok"
            }
        }

        scenario("onLockSystemError = FALLBACK with failed fallback") {
            Given(mutexConfig) {
                withDefault(
                    MutexTypeConfig("error-all", onLockSystemError = MutexOnErrorAction.FALLBACK, fallbackLockSystem = "error-all2")
                )
            }
            When {
                mutex.synchronized { "ok" }
            } thenExceptionThrown(IllegalStateException::class)
        }
    }

    feature("synchronized with error releasing lock") {
        scenario("failOnLockReleaseError = true") {
            Given(mutexConfig) {
                withDefault(MutexTypeConfig(lockSystem = "error-release", failOnLockReleaseError = true))
            }
            When {
                mutex.synchronized { "ok" }
            } thenExceptionThrown (IllegalStateException::class)
        }

        scenario("failOnLockReleaseError = false") {
            Given(mutexConfig) {
                withDefault(MutexTypeConfig(lockSystem = "error-release", failOnLockReleaseError = false))
            }
            When {
                mutex.synchronized { "ok" }
            } then {
                it shouldBe "ok"
            }
        }
    }

    feature("mayBeSynchronized with error getting lock") {
        scenario("onLockSystemError = FAIL") {
            Given(mutexConfig) {
                withDefault(MutexTypeConfig(lockSystem = "error-all", onLockSystemError = MutexOnErrorAction.FAIL))
            }
            When {
                mutex.maybeSynchronized { it }
            } thenExceptionThrown(IllegalStateException::class)
        }

        scenario("onLockSystemError = GET") {
            Given(mutexConfig) {
                withDefault(MutexTypeConfig(lockSystem = "error-all", onLockSystemError = MutexOnErrorAction.GET))
            }
            When {
                mutex.maybeSynchronized { it }
            } then {
                it shouldBe true
            }
        }

        scenario("onLockSystemError = FALLBACK with working fallback") {
            Given(mutexConfig) {
                withDefault(
                    MutexTypeConfig("error-all", onLockSystemError = MutexOnErrorAction.FALLBACK, fallbackLockSystem = "local")
                )
            }
            When {
                mutex.maybeSynchronized { it }
            } then {
                it shouldBe true
            }
        }

        scenario("onLockSystemError = FALLBACK with failed fallback") {
            Given(mutexConfig) {
                withDefault(
                    MutexTypeConfig("error-all", onLockSystemError = MutexOnErrorAction.FALLBACK, fallbackLockSystem = "error-all2")
                )
            }
            When {
                mutex.maybeSynchronized { it }
            } thenExceptionThrown(IllegalStateException::class)
        }
    }

    feature("maybeSynchronized with error releasing lock") {
        scenario("failOnLockReleaseError = true") {
            Given(mutexConfig) {
                withDefault(MutexTypeConfig(lockSystem = "error-release", failOnLockReleaseError = true))
            }
            When {
                mutex.maybeSynchronized { it }
            } thenExceptionThrown (IllegalStateException::class)
        }

        scenario("failOnLockReleaseError = false") {
            Given(mutexConfig) {
                withDefault(MutexTypeConfig(lockSystem = "error-release", failOnLockReleaseError = false))
            }
            When {
                mutex.maybeSynchronized { it }
            } then {
                it shouldBe true
            }
        }
    }

    feature("force ignoring errors") {
        Where(
            "onLockSystemError = FAIL" to
                MutexTypeConfig(lockSystem = "error-all", onLockSystemError = MutexOnErrorAction.FAIL),
            "onLockSystemError = FALLBACK with failed fallback" to
                MutexTypeConfig("error-all", onLockSystemError = MutexOnErrorAction.FALLBACK, fallbackLockSystem = "error-all2"),
            "failOnLockReleaseError = true" to
                MutexTypeConfig(lockSystem = "error-release", failOnLockReleaseError = true)
        ) { (description, defaultConfig) ->
            scenario("synchronized with $description") {
                Given(mutexConfig) {
                    withDefault(defaultConfig)
                }
                When {
                    mutexForceIgnoringErrors.synchronized { "ok" }
                } then {
                    it shouldBe "ok"
                }
            }

            scenario("maybeSynchronized with $description") {
                Given(mutexConfig) {
                    withDefault(defaultConfig)
                }
                When {
                    mutexForceIgnoringErrors.maybeSynchronized { it }
                } then {
                    it shouldBe true
                }
            }
        }
    }
})
