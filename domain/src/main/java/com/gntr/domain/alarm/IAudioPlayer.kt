package com.gntr.domain.alarm

interface IAudioPlayer {
    fun play(uriString: String)
    fun stop()
    fun release()
}