package ru.semper_viventem.trackrobot_android

import io.reactivex.Completable
import io.reactivex.Observable

interface TrackRobot {

    fun connect(ip: String): Completable

    fun disconnect(): Completable

    fun observeConnected(): Observable<Boolean>

    fun moveStraight(): Completable

    fun moveBack(): Completable

    fun moveRight(): Completable

    fun moveLeft(): Completable

    fun stop(): Completable
}