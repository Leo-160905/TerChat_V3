import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.system.exitProcess

fun createServer() {
    val server = ServerSocket(SERVER_PORT)
    val ips: List<String> = getIP().split(";")

    println("Generating Key pair for encryption,")
    val rsa = RSA()
    println("your local IP-Address is ${ips[0]} and your public is ${ips[1]}")
    println("listening for clients")

    val client: Socket = server.accept()
    val send = DataOutputStream(client.getOutputStream())
    val receive = DataInputStream(client.getInputStream())

    println("start key exchange")
    send.writeUTF(rsa.getPublicKey())
    rsa.setPartnersPublicKey(receive.readUTF())
    println("key exchange successful")

    send.writeUTF(rsa.encrypt("$name joined"))

    if(!singleFileTransfer){
        chatting = true

        readMessages(rsa, receive, send, client)

        writing(send, client, rsa)
    }
    else {
        sendFile(send, rsa, programmParams)
        send.writeUTF(rsa.encrypt("stop"))
        client.close()
        send.close()
        receive.close()
        exitProcess(0)
    }

//    val thread = Thread {
//        while (chatting) {
//            try {
//                val message = rsa.decrypt(receive.readUTF())
//                if (message == "stop") {
//                    chatting = false
//                    client.close()
//                    println("connection closed")
//                    exitProcess(0)
//                }
//                if (message.startsWith("file:")) {
//                    readFile(receive, client, rsa, message.removePrefix("file:"))
//                }
//                else println("${ConsoleColors.RED}$message ${ConsoleColors.GREEN}")
//            } catch (e: Exception) {
//                println("connection closed")
//                println(e)
//                exitProcess(0)
//            }
//        }
//    }
//    thread.start()
}