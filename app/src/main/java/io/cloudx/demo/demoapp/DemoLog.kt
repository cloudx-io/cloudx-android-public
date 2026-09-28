package io.cloudx.demo.demoapp

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The demo's log. Every entry goes to logcat through [android.util.Log] and to the on-screen log
 * list. In your app, use your own logging instead.
 */
object DemoLog {

    enum class Level { INFO, WARN, ERROR }

    data class Entry(
        val level: Level,
        val tag: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _entries = MutableSharedFlow<Entry>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val entries: SharedFlow<Entry> = _entries.asSharedFlow()

    fun i(tag: String, message: String) {
        android.util.Log.i(tag, message)
        _entries.tryEmit(Entry(Level.INFO, tag, message))
    }

    fun w(tag: String, message: String) {
        android.util.Log.w(tag, message)
        _entries.tryEmit(Entry(Level.WARN, tag, message))
    }

    fun e(tag: String, message: String) {
        android.util.Log.e(tag, message)
        _entries.tryEmit(Entry(Level.ERROR, tag, message))
    }
}
