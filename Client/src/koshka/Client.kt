package koshka

import kotlinx.coroutines.*
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.AsynchronousSocketChannel
import java.util.Scanner
import kotlin.concurrent.thread
import kotlin.coroutines.suspendCoroutine

class Client(
    val host: String,
    val port: Int,
) {
    private val userScanner = Scanner(System.`in`)
    private val socket = AsynchronousSocketChannel.open()
    private val communicator = Communicator(socket)
    private val clientScope = CoroutineScope(Dispatchers.IO)

    init {
        runBlocking {
            suspendCoroutine<Void> {
                socket.connect(
                    InetSocketAddress(host, port),
                    null,
                    ActionCompletionHandler(it)
                )
            }


            communicator.start(::parse)

            launch {
                while (true) {
                    val userInput = userScanner.nextLine()
                    communicator.sendMessage(userInput)
                }
            }
        }
    }

    private fun parse(data: String){
        println(data)
    }

    fun stop() = communicator.stop()

}