package com.curiouskids.cashapp.core.common

import java.time.Instant
import javax.inject.Inject

interface Clock {
    fun now(): Instant
}

class DefaultClock @Inject constructor() : Clock {
    override fun now(): Instant = Instant.now()
}
