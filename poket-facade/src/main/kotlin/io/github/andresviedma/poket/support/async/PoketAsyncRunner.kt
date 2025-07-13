package io.github.andresviedma.poket.support.async

import kotlinx.coroutines.Job

object PoketAsyncRunnerProvider {
    @Suppress("MemberVisibilityCanBePrivate")
    internal var injectedLauncher: PoketAsyncRunner? = null

    private val defaultLauncher = DefaultPoketAsyncRunner()

    val launcher: PoketAsyncRunner
        get() =
        injectedLauncher ?: defaultLauncher

    @Suppress("unused")
    fun init(launcher: PoketAsyncRunner?) = apply {
        injectedLauncher = launcher
    }
}

interface PoketAsyncRunner {
    suspend fun launch(operation: String, block: suspend Job.() -> Unit): Job
}
