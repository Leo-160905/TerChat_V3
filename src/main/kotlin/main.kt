import java.ConsoleColors
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.*
import java.util.*
import kotlin.system.exitProcess

/*
* Little Terminal Chat with encryption so no one else will read you messages!!
*/


const val SERVER_PORT = 64646
val scan = Scanner(System.`in`)
var ipAddress: String = ""
var chatting = false
var name = ""
var singleFileTransfer = false
var programmParams = ""
lateinit var thread: Thread

fun main(args: Array<String>) {
    programmParams = args.joinToString("")
    if (programmParams.startsWith("-f")) {
        singleFileTransfer = true
        programmParams = programmParams.removePrefix("-f")
        createServer()
    }
    if (programmParams.startsWith("-r")) {
        programmParams = programmParams.removePrefix("-r")
        joinServer(programmParams)
    }

    print("Hi to TerChat mark 3, please enter your name: ")
    name = scan.nextLine()
    print("do you want to \'j\'oin or \'c\'reate a server? ")
    print(ConsoleColors.GREEN)
    when (scan.next()) {
        "j" -> {
            var isIpCorrect: Boolean
            do {
                try {
                    isIpCorrect = true
                    print("type in the IP-Address of the server: ")
                    ipAddress = scan.next()
                    joinServer(ipAddress)
                } catch (_: Exception) {
                    isIpCorrect = false
                }
            } while (!isIpCorrect)
        }

        "c" -> {
            createServer()
        }
    }
}

fun getIP(): String {
    DatagramSocket().use { datagramSocket ->

        val url = URL("https://api.ipify.org")
        val connection = url.openConnection()

        val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
        val ip = reader.readLine()
        reader.close()

        datagramSocket.connect(InetAddress.getByName("8.8.8.8"), 12345)
        return datagramSocket.localAddress.hostAddress.toString() + ";" + ip
    }
}

fun writeMessages(send: DataOutputStream, rsa: RSA) {
    while (chatting) {
        try {
            // scans for new messages
            val message = scan.nextLine()

            // if message is an empty string, do nothing
            if (message == "") {
                continue
            }

            // if message contains exit, close the connection
            if (message == "exit") {
                send.writeUTF(rsa.encrypt("stop"))
            }

            // if message starts with  file:, start the file transfer prozedur
            if (message.startsWith("file:")) {
                sendFile(send, rsa, message.removePrefix("file:"))
            } else writing("$name $message", rsa, send) // call the writing funktion to split the message to bytes
        } catch (e: Exception) {
            println("connection closed 1")
            println(e)
            exitProcess(0)
        }
    }
}

fun readMessages(rsa: RSA, receive: DataInputStream, send: DataOutputStream, client: Socket) {
    thread = Thread {
        while (chatting) {
            try {
                val message = rsa.decrypt(receive.readUTF())
                if (message == "stop") {
                    println("connection closed \n")
                    send.writeUTF(rsa.encrypt("stop2"))
                    chatting = false
                    client.close()
                    exitProcess(0)
                }
                if (message == "stop2") {
                    println("connection closed \n")
                    chatting = false
                    client.close()
                    exitProcess(0)
                }
                if (message.startsWith("file:")) {
                    readFile(receive, rsa, message.removePrefix("file:"))
                }
                if (message.startsWith("message")) reading(receive, rsa) // code for an incoming message
            } catch (e: Exception) {
                println("connection closed 2")
                println(e)
                exitProcess(0)
            }
        }
    }
    thread.start()
}

fun writing(message: String, rsa: RSA, send: DataOutputStream) {
    send.writeUTF(rsa.encrypt("message")) // sends the code so the other partition knows that a message is coming

    // calculating chunk size of message bytes
    var messageSize = (message.length / 245) * 256
    if (message.length % 245 > 0) {
        messageSize += 256
    }
    send.writeUTF(rsa.encrypt(messageSize.toString()))

    val buffer = ByteArray(245)
    val ips = DataInputStream(message.byteInputStream())

    // send the message bytes to the other partition
    while (ips.read(buffer) > 0) {
        val stack = rsa.encrypt(buffer)
        send.write(stack, 0, stack.size)
    }
}

fun reading(receive: DataInputStream, rsa: RSA) {
    var messageSize = rsa.decrypt(receive.readUTF()).toInt() // receiving the byte size of each message chunk
    val buffer = ByteArray(256)
    var message = ""

    // receiving the bytes of the message
    while (messageSize > 0 && receive.read(buffer, 0, buffer.size) != -1) {
        val stack = rsa.decrypt(buffer)
        for (s in stack) {
            message += s.toInt().toChar()
        }
        messageSize -= 256
    }
    println("${ConsoleColors.RED}$message ${ConsoleColors.GREEN}")
}

fun sendFile(send: DataOutputStream, rsa: RSA, filePath: String) {
    // Create file and FileInputStream to work and read a File
    val file = File(filePath)
    val fis = FileInputStream(file)

    // Sends the command file: to the other socket
    send.writeUTF(rsa.encrypt("file:${file.name}"))

    var fileSize: ULong = ((file.length() / 245) * 256).toULong()
    if ((file.length() % 245) > 0) {
        fileSize += 256u
    }
    send.writeUTF(rsa.encrypt(fileSize.toString()))

    val buffer = ByteArray(245)

    // This reads the next [Size of buffer] bytes into the buffer and stores how many bytes were stored in count
    while (fis.read(buffer) != -1) {
        // makes a stack for the encrypted message to handle with it
        val stack = rsa.encrypt(buffer)
        // sends every bytearray to the other socket until the file is completely red
        send.write(stack, 0, stack.size)
    }
    // This closes the FileInputStream again
    fis.close()

    // Message for the user, that the File has been loaded
    println("${ConsoleColors.WHITE}File has been sent ${ConsoleColors.GREEN}")
    return
}

fun readFile(receive: DataInputStream, rsa: RSA, filePath: String) {
    // Create new scratch file from selection
    val file = File(filePath)
    val fos = FileOutputStream(file)

    // Signs the user that there will be sent a file
    println("${ConsoleColors.WHITE}Loading File: ${file.name}....")

    // defines the size of every chunk of bytes that arrives, it is 1024 because the RSA is set to 1024 byte
    var size: ULong = rsa.decrypt(receive.readUTF()).toULong()


    val buffer = ByteArray(256)

    // Reads every chunk of bytes, which were sent
//    while (size > 0u && (receive.read(buffer, 0, minOf(buffer.size.toULong(), size * 256u).toInt())
    while (size > 0u && receive.read(buffer, 0, buffer.size) != -1) {
        // Stack to handle the inputs
        val stack = rsa.decrypt(buffer)
        // fos writes the decrypted Bytes to a File
        fos.write(stack, 0, stack.size)

        size -= 256u
    }
    fos.close()
    println("File has been received ${ConsoleColors.GREEN}")
}