package koshka

import kotlinx.coroutines.*
import java.nio.channels.AsynchronousSocketChannel

class ConnectedClient(private val client: AsynchronousSocketChannel) {
    private val communicator = Communicator(client)
    private var userName: String? = null
    private val clientScope = CoroutineScope(Dispatchers.IO)

    init {
        connectedClients.add(this)
        communicator.onClose = ::stop
        try{
            clientScope.launch {
                communicator.sendMessage("[СИСТЕМА] Напишите имячко:")
            }
            communicator.start(::parse)
        } catch (e: Exception) {
            stop()
        }
    }


    private fun parse(data: String) {
        clientScope.launch {
            try {
                if (userName == null) {
                    registerUser(data)
                } else {
                    val trimmed = data.trim()
                    if (trimmed.isEmpty()) {
                        communicator.sendMessage("[СИСТЕМА] Ничего не вижу, напишите что-нибудь!!")
                    } else {
                        val match = Regex("""\*(.+?)\*,\s*(.+)""").matchEntire(trimmed)

                        if (match != null) {
                            val recipientName = match.groupValues[1].trim()
                            val message = match.groupValues[2].trim()

                            val recipient = connectedClients.find {
                                it.userName.equals(recipientName, ignoreCase = true)
                            }

                            if (recipient != null) {
                                if (recipient == this@ConnectedClient) {
                                    communicator.sendMessage("[СИСТЕМА] Вы зачем себе пишете?")
                                } else {
                                    recipient.communicator.sendMessage("[$userName]: $message")
                                    communicator.sendMessage("[Вы -> $recipientName]: $message")
                                }
                            }
                            else {
                                communicator.sendMessage("[СИСТЕМА] Пользователя '$recipientName' у нас в чятике нет!")
                            }
                        }
                        else if(trimmed == "q" || trimmed == "выход"){
                            stop()
                        }
                        else {
                            sendToAll("[$userName (всем)]: $trimmed", echo = false)
                        }
                    }
                }
            } catch (e: Exception) {
                stop()
            }
        }
    }


    private suspend fun registerUser(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isNotEmpty()) {
            if (connectedClients.any { it.userName?.equals(trimmedName, ignoreCase = true) == true }) {
                communicator.sendMessage("[СИСТЕМА] Имячко '$trimmedName' уже занято. Введите другое имячко:")
            } else {
                userName = trimmedName
                communicator.sendMessage("[СИСТЕМА] Привет, $userName!")
                sendToAll("[СИСТЕМА] $userName теперь в чятике!", echo = false)
            }
        } else {
            communicator.sendMessage("[СИСТЕМА] Ваше имячко пустое? Не верю! Введите другое:")
        }
    }

    fun stop() {
        clientScope.launch {
            val nameToNotify = userName
            userName = null

            if (nameToNotify != null) {
                sendToAll("[СИСТЕМА] $nameToNotify покинул чатик :(", echo = false)
            }

            try {
                communicator.sendMessage("[СИСТЕМА] покаааа :(")
            } catch (e: Exception) {
            } finally {
                connectedClients.remove(this@ConnectedClient)
                communicator.stop()
                clientScope.cancel()
            }
        }
    }

    private suspend fun sendToAll(data: String, echo: Boolean = true) {
        connectedClients.forEach { client ->
            if (echo || client != this) {
                clientScope.launch {
                    client.communicator.sendMessage(data)
                }
            }
        }
    }

    companion object {
        private val connectedClients = java.util.concurrent.CopyOnWriteArrayList<ConnectedClient>()
    }

}