package koshka

import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ActionCompletionHandler<R>(private val c: Continuation<R>)
    : CompletionHandler<R, Any?>
{
    override fun completed(result: R, attachment: Any?) {
        c.resume(result)
    }

    override fun failed(exc: Throwable, attachment: Any?) {
        c.resumeWithException(exc)
    }
}