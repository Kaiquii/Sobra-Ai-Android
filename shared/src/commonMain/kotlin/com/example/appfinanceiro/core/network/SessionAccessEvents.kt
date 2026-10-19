package com.example.appfinanceiro.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SessionAccessEvents {
    private val _accessRevoked = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val accessRevoked: SharedFlow<String> = _accessRevoked.asSharedFlow()

    fun notifyAccessRevoked(message: String) {
        _accessRevoked.tryEmit(message)
    }
}
