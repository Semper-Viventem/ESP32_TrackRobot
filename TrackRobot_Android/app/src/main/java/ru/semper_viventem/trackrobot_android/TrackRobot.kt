package ru.semper_viventem.trackrobot_android

import io.reactivex.Completable

interface TrackRobot {

    fun connect(ip: String): Completable

    fun moveStraight(): Completable

    fun moveBack(): Completable

    fun moveRight(): Completable

    fun moveLeft(): Completable

    fun stop(): Completable
}