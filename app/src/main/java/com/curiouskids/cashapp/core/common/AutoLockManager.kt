package com.curiouskids.cashapp.core.common

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoLockManager @Inject constructor(
    private val clock: Clock
) : DefaultLifecycleObserver {

    private var backgroundTimestamp: Instant? = null
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Configurable timeout (default 2 minutes)
    var lockTimeoutSeconds: Long = 120

    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundTimestamp = clock.now()
    }

    override fun onStart(owner: LifecycleOwner) {
        val lastTime = backgroundTimestamp
        if (lastTime != null) {
            val elapsedSecs = clock.now().epochSecond - lastTime.epochSecond
            if (elapsedSecs >= lockTimeoutSeconds) {
                _isLocked.value = true
            }
        }
        backgroundTimestamp = null
    }

    fun unlock() {
        _isLocked.value = false
    }
}
