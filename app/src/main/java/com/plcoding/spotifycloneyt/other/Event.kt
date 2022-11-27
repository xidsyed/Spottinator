package com.plcoding.spotifycloneyt.other

open class Event<out T>(private val data: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        if(hasBeenHandled) return null
        hasBeenHandled = true
        return data
    }

    // in case you need to see the data after it has bene handled
    fun peekContent() = data
}