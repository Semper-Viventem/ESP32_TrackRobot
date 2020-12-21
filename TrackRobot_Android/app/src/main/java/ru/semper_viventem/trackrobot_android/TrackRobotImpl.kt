package ru.semper_viventem.trackrobot_android

import androidx.annotation.IntRange
import io.reactivex.Completable
import okhttp3.*
import okio.ByteString
import timber.log.Timber
import java.util.concurrent.TimeUnit

class TrackRobotImpl : TrackRobot {

    private var webSocket: WebSocket? = null

    override fun connect(ip: String): Completable {
        return if (webSocket != null) {
            Completable.complete()
        } else {
            openNewWebSocket(ip)
        }
    }

    override fun moveStraight(): Completable {
        return rotate(MAX, MAX)
    }

    override fun moveBack(): Completable {
        return rotate(MIN, MIN)
    }

    override fun moveRight(): Completable {
        return rotate(MIN, MAX)
    }

    override fun moveLeft(): Completable {
        return rotate(MAX, MIN)
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
                it.send(rotateLeft(left))
                it.send(rotateRight(right))
            }
        } ?: Completable.error(IllegalStateException("WebSocket is closed"))
    }

    private fun rotateLeft(@IntRange(from = MIN_L, to = MAX_L) value: Int) = "L $value"
    private fun rotateRight(@IntRange(from = MIN_L, to = MAX_L) value: Int) = "R $value"

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
                    emitter.onComplete()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    this@TrackRobotImpl.webSocket = null
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    emitter.onError(t)
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