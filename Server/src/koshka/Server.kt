package koshka

import java.io.PrintWriter
import java.net.ServerSocket
import java.util.Scanner
import kotlin.concurrent.thread
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import kotlin.coroutines.suspendCoroutine

class Server(
    port: Int = 5206
) {

    private val serverSocket: AsynchronousServerSocketChannel =
        AsynchronousServerSocketChannel.open()
    private val serverScope = CoroutineScope(Dispatchers.IO)

    init{
        serverSocket.bind(InetSocketAddress(port))

        runBlocking {
            while(true) {
                val socket = suspendCoroutine {
                    serverSocket.accept(
                        null,
                        ActionCompletionHandler(it)
                    )
                }
                ConnectedClient(socket)
            }
            serverSocket.close()
        }
    }
}