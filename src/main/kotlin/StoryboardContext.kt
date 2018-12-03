package org.openrndr.numate

import kotlinx.coroutines.*
import org.openrndr.Program

class StoryboardContext(val scope: CoroutineScope, val dispatcher: CoroutineDispatcher) {
    private var context: Job = scope.launch(dispatcher) {
        while (true) {
            yield()
        }
    }

    fun storyboard(loop: Boolean = false, init: Storyboard.() -> Unit): Storyboard {
        if (context.isCancelled) {
            context = scope.launch(dispatcher) {
                while (true) {
                    yield()
                }
            }
        }
        return storyboard(scope, context, dispatcher, loop, init)
    }

    fun cancel() {
        context.cancel()
        context.cancelChildren()
    }
}

fun Program.storyboardContext(): StoryboardContext {
    return StoryboardContext(GlobalScope, dispatcher)
}