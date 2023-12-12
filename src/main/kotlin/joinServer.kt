import java.ConsoleColors
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

fun joinServer(ip: String) {
    val client = Socket(ip, SERVER_PORT)
    val send = DataOutputStream(client.getOutputStream())
    val receive = DataInputStream(client.getInputStream())

    println("Generating Key pair for encryption,")
    val rsa = RSA()

    println("start key exchange")
    rsa.setPartnersPublicKey(receive.readUTF())
    send.writeUTF(rsa.getPublicKey())
    println("key exchange successful")

    send.writeUTF(rsa.encrypt("$name joined"))
    println("${ConsoleColors.RED}${rsa.decrypt(receive.readUTF())} ${ConsoleColors.GREEN}")

    chatting = true

    readMessages(rsa, receive, send, client)

    writeMessages(send, rsa)
}