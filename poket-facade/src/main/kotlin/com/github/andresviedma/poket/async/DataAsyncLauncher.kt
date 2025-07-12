package com.github.andresviedma.poket.async

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

object DataAsyncRunnerProvider {
    private var injectedLauncher: DataAsyncRunner? = null

    private val defaultLauncher = DefaultDataAsyncRunner()

    val launcher: DataAsyncRunner get() =
        injectedLauncher ?: defaultLauncher
}

interface DataAsyncRunner {
    suspend fun launch(operation: String, block: suspend Job.() -> Unit): Job
}

private val logger = KotlinLogging.logger {}

class DefaultDataAsyncRunner : DataAsyncRunner {
    private val parentJob = Job()
    private val scope: CoroutineScope = CoroutineScope(
        Executors.newFixedThreadPool(10).asCoroutineDispatcher() + parentJob
    )

    override suspend fun launch(operation: String, block: suspend Job.() -> Unit): Job =
        scope.launch(CoroutineName(operation)) {
            try {
                currentCoroutineContext().job.block()
            } catch (exception: Throwable) {
                logger.error(exception) { "$operation error" }
            }
        }

    suspend fun waitForAllPendingJobs() {
        while (parentJob.children.any()) {
            parentJob.children.forEach { it.join() }
        }
    }
}
