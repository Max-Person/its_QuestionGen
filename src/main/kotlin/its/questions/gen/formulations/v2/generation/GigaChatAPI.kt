package its.questions.gen.formulations.v2.generation

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object GigaChatAPI {
    var clientId = ""
    var clientSecret = ""
    private var lastCreatedTokenTime = 0L
    private var access_token = ""
    var isActive = false

    private val client = createUnsafeOkHttpClient()
    private val mapper = jacksonObjectMapper()

    fun generate(string: String): String {
        return try {
            if (lastCreatedTokenTime == 0L || lastCreatedTokenTime + 30 * 60 * 1000 > System.currentTimeMillis()) {
                access_token = getAccessToken() ?: ""
            }
            sendMessageToGigaChat(
                string,
                "Перепиши текст, исправив грамматические, орфографические и пунктуационные ошибки в тексте."
            )
        } catch (e: Exception) {
            println("Ошибка во время отправки запроса на генерацию текста: ${e.message}")
            string
        }
    }

    fun toCase(string: String, case: Case) : String {
        return try {
            if (lastCreatedTokenTime == 0L || lastCreatedTokenTime + 30 * 60 * 1000 > System.currentTimeMillis()) {
                access_token = getAccessToken()?: ""
            }
            sendMessageToGigaChat(string, "Приведи слово к падежу ${case.description}. Объяснения не нужны, верни весь переданный текст полностью.")
        } catch (e: Exception) {
            println("Ошибка во время отправки запроса на приведение слова к падежу: ${e.message}")
            return string.toCase(case)
        }
    }

    private fun getAccessToken(): String? {
        val mediaType = "application/x-www-form-urlencoded".toMediaTypeOrNull()
        val body = "scope=GIGACHAT_API_PERS".toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://ngw.devices.sberbank.ru:9443/api/v2/oauth")
            .method("POST", body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .addHeader("RqUID", "23003284-3f74-45a5-a43b-ce1c88a20900")
            .addHeader(
                "Authorization",
                "Basic " + Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
            )
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            println("Ошибка: ${response.code}")
            return null
        }

        val parsed: GigaTokenResponse = mapper.readValue(response.body!!.string())

        lastCreatedTokenTime = parsed.expires_at
        return parsed.access_token
    }

    private fun sendMessageToGigaChat(message: String, prompt: String): String {
        if (access_token.isEmpty()) {
            return message
        }
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = """
        {
          "model": "GigaChat",
          "messages": [
            {"role": "system", "content": "${prompt.replace("\"", "\\")}}"},
            {
            "created_at": 1748373231,
            "role": "user",
            "content": "${message.replace("\"", "\\")}"
            }
          ],
          "profanity_check": true
        }
    """.trimIndent().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://gigachat.devices.sberbank.ru/api/v1/chat/completions")
            .method("POST", body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer $access_token")
            .build()

        return client.newCall(request).execute().use { r ->
            if (!r.isSuccessful) {
                println("Ошибка: ${r.code}")
                return@use message
            }
            val body = r.body?.string() ?: return message

            val parsed: GigaResponse = mapper.readValue(body)

            // Печатаем content первого сообщения
            val content = parsed.choices.firstOrNull()?.message?.content
            var msg = content?:message
            if (message.first().isLowerCase()) {
                msg = msg.replaceFirstChar { it.lowercase(Locale.getDefault()) }
            }
            return@use msg
        }
    }

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        try {
            // Создание TrustManager, который ничего не проверяет
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            // Установка SSL-контекста с доверенным TrustManager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // Отключаем проверку имени хоста
                .connectTimeout(3, TimeUnit.SECONDS)  // время на соединение
                .readTimeout(1, TimeUnit.SECONDS)     // время на чтение ответа
                .writeTimeout(1, TimeUnit.SECONDS)    // время на отправку тела запроса
                .build()

        } catch (e: Exception) {
            println("Ошибка во время формирования запроса: ${e.message}")
            throw RuntimeException(e)
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class GigaTokenResponse(
        val access_token: String,
        val expires_at: Long
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class GigaResponse(
        val choices: List<Choice>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class Choice(
        val message: Message
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class Message(
        val role: String,
        val content: String
    )
}