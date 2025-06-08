package koshka

import kotlinx.coroutines.*
import java.io.PrintWriter
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread
import kotlin.coroutines.suspendCoroutine

class Communicator(
    private val socket: AsynchronousSocketChannel,
) {
    var onClose: (() -> Unit)? = null
    private var parse: ((String) -> Unit)? = null
    private var isRunning = false
    private val communicatorScope = CoroutineScope(Dispatchers.IO)

    private fun startMessageAccepting() {
        communicatorScope.launch {
            try {
                while (isRunning) {
                    var capacity = Int.SIZE_BYTES
                    repeat(2) { i ->
                        val buf = ByteBuffer.allocate(capacity)
                        val bytesRead = suspendCoroutine<Int> { cont ->
                            socket.read(buf, null, ActionCompletionHandler(cont))
                        }
                        if (bytesRead == -1) {
                            throw Exception("Connection closed by client")
                        }
                        buf.flip()
                        if (i == 0) {
                            capacity = buf.int
                            if (capacity < 0 || capacity > 65536) {
                                throw Exception("Invalid message size")
                            }
                        } else {
                            parse?.invoke(Charset.defaultCharset().decode(buf).toString())
                        }
                        buf.clear()
                    }
                }
            } catch (e: Exception) {
                isRunning = false
                onClose?.invoke()
            } finally {
                socket.close()
            }
        }
    }

    suspend fun sendMessage(message: String) {
        try {
            val bytes = message.toByteArray()
            val buffer = ByteBuffer.allocate(bytes.size + Int.SIZE_BYTES).apply {
                putInt(bytes.size)
                put(bytes)
                flip()
            }
            suspendCoroutine<Int> { cont ->
                socket.write(buffer, null, ActionCompletionHandler(cont))
            }
        } catch (e: Exception) {
            isRunning = false
            onClose?.invoke()
            throw e
        }
    }

    fun start(parser: (String) -> Unit) {
        if (!isRunning) {
            parse = parser
            isRunning = true
            startMessageAccepting()
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        onClose?.invoke()
        onClose = null
    }
}