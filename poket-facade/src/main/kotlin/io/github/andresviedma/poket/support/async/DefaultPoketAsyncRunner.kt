package io.github.andresviedma.poket.support.async

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext

private val logger = KotlinLogging.logger {}

class DefaultPoketAsyncRunner : PoketAsyncRunner {
    private val parentJob = Job()
    @OptIn(DelicateCoroutinesApi::class)
    private val scope: CoroutineScope = CoroutineScope(
        newFixedThreadPoolContext(10, "poket-async") + parentJob
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
