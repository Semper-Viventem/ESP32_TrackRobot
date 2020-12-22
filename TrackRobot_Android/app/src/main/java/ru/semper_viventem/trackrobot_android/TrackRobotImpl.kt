package ru.semper_viventem.trackrobot_android

import androidx.annotation.IntRange
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import okhttp3.*
import okio.ByteString
import timber.log.Timber
import java.util.concurrent.TimeUnit

class TrackRobotImpl : TrackRobot {

    private var webSocket: WebSocket? = null
    private var isConnected = BehaviorSubject.createDefault(false)

    override fun connect(ip: String): Completable {
        return if (webSocket != null) {
            Completable.complete()
        } else {
            openNewWebSocket(ip)
        }
    }

    override fun observeConnected(): Observable<Boolean> = isConnected.hide()

    override fun disconnect(): Completable {
        return Completable.fromAction {
            webSocket?.cancel()
        }
    }

    override fun moveStraight(): Completable {
        return rotate(MAX, MAX)
    }

    override fun moveBack(): Completable {
        return rotate(MIN, MIN)
    }

    override fun moveRight(): Completable {
        return rotate(MAX, MIN)
    }

    override fun moveLeft(): Completable {
        return rotate(MIN, MAX)
    }

    override fun stop(): Completable {
        return rotate(STOP_VALUE, STOP_VALUE)
    }

    private fun rotate(
        @IntRange(from = MIN_L, to = MAX_L) left: Int,
        @IntRange(from = MIN_L, to = MAX_L) right: Int
    ): Completable {
        return webSocket?.let {
            Completable.fromAction {
                if (left == right) {
                    it.send(rotateBoth(left))
                } else {
                    it.send(rotateLeft(left))
                    it.send(rotateRight(right))
                }
            }
        } ?: Completable.error(IllegalStateException("WebSocket is closed"))
    }

    private fun rotateLeft(@IntRange(from = MIN_L, to = MAX_L) value: Int) = "L $value\n"
    private fun rotateRight(@IntRange(from = MIN_L, to = MAX_L) value: Int) = "R $value\n"
    private fun rotateBoth(@IntRange(from = MIN_L, to = MAX_L) value: Int) = "B $value\n"

    private fun openNewWebSocket(ip: String): Completable = Completable.create { emitter ->
        val request = Request.Builder()
            .url("ws://$ip")
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    this@TrackRobotImpl.webSocket = webSocket
                    isConnected.onNext(true)
                    emitter.onComplete()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    this@TrackRobotImpl.webSocket = null
                    isConnected.onNext(false)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (!emitter.isDisposed) {
                        emitter.onError(t)
                    }
                    isConnected.onNext(false)
                    this@TrackRobotImpl.webSocket = null
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    Timber.d(bytes.hex())
                }
            }
        )

        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        okHttpClient.dispatcher.executorService.shutdown()
    }

    companion object {
        private const val MIN_L = -255L
        private const val MAX_L = 255L

        private const val MIN = -255
        private const val MAX = 255
        private const val STOP_VALUE = 0
    }
}