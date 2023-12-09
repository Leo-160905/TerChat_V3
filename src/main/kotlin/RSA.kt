import java.lang.Exception
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import kotlin.system.exitProcess


class RSA {
    private var publicKey: PublicKey
    private var privateKey: PrivateKey
    private lateinit var partnerPublicKey: PublicKey

    init {
        val generator = KeyPairGenerator.getInstance("RSA")

        generator.initialize(2048)
        val pair = generator.generateKeyPair()
        publicKey = pair.public
        privateKey = pair.private
    }

    fun addPublicKeys(k: PublicKey) {
        partnerPublicKey = k
    }

    fun encrypt(message: String): String {
        val m: ByteArray = message.toByteArray(Charsets.UTF_8)
        val encrypter: Cipher = Cipher.getInstance("RSA")
        encrypter.init(Cipher.ENCRYPT_MODE, partnerPublicKey)
        return Base64.getEncoder().encodeToString(encrypter.doFinal(m))
    }

    fun decrypt(message: String): String {
        val m: ByteArray = Base64.getDecoder().decode(message)
        val decrypter: Cipher = Cipher.getInstance("RSA")
        decrypter.init(Cipher.DECRYPT_MODE, privateKey)
        return decrypter.doFinal(m).toString(Charsets.UTF_8)
    }

    fun encrypt(message: ByteArray): ByteArray {
        val encrypter: Cipher = Cipher.getInstance("RSA")
        encrypter.init(Cipher.ENCRYPT_MODE, partnerPublicKey)
        return encrypter.doFinal(message)
    }

    fun decrypt(message: ByteArray): ByteArray {
        val decrypter: Cipher = Cipher.getInstance("RSA")
        decrypter.init(Cipher.DECRYPT_MODE, privateKey)
        return decrypter.doFinal(message)
    }

    fun getPublicKey(): String {
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    fun setPartnersPublicKey(k: String) {
          try {
              val keyBytes = Base64.getDecoder().decode(k)
              val keySpec = X509EncodedKeySpec(keyBytes)
              val keyFactory = KeyFactory.getInstance("RSA")
              partnerPublicKey = keyFactory.generatePublic(keySpec)
          }
          catch (_: Exception) {
              println("Unsuccessful key exchange!")
              exitProcess(0)
          }
    }

    fun soutKey() {
        println(publicKey)
        println(partnerPublicKey)
    }
}