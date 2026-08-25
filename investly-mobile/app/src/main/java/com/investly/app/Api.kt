package com.investly.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class ApiException(
    val httpCode: Int,
    val message: String,
    val errors: Map<String, List<String>> = emptyMap()
) : Exception(message)

class Api(context: Context) {

    companion object {
        const val BASE = "http://192.168.100.5:8000"
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    }

    private val prefs = context.getSharedPreferences("investly_cookies", Context.MODE_PRIVATE)
    private val cachePrefs = context.getSharedPreferences("investly_cache", Context.MODE_PRIVATE)
    private val cookies = mutableMapOf<String, Cookie>()

    init {
        prefs.all.forEach { (k, v) ->
            runCatching { Cookie.parse("http://192.168.100.5".toHttpUrl(), v.toString())?.let { cookies[k] = it } }
        }
    }

    private val jar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, list: List<Cookie>) {
            synchronized(cookies) {
                for (c in list) {
                    if (c.value.isEmpty()) cookies.remove(c.name)
                    else cookies[c.name] = c
                    prefs.edit().apply {
                        if (c.value.isEmpty()) remove(c.name) else putString(c.name, c.toString())
                    }.apply()
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            synchronized(cookies) { cookies.values.toList() }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(jar)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private fun csrfToken(): String? = synchronized(cookies) {
        cookies["XSRF-TOKEN"]?.value?.let { URLDecoder.decode(it, "UTF-8") }
    }

    fun clearSession() {
        synchronized(cookies) { cookies.clear() }
        prefs.edit().clear().apply()
    }

    /** Cached JSON for offline mode */
    fun readCache(key: String): String? = cachePrefs.getString("c_$key", null)

    fun writeCache(key: String, body: String) {
        cachePrefs.edit().putString("c_$key", body).apply()
    }

    private fun req(method: String, path: String, body: JsonObject? = null): String {
        val url = (BASE + path).toHttpUrl()
        val b = okhttp3.Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Origin", BASE)
            .header("Referer", "$BASE/")
        if (method == "POST" || method == "PUT" || method == "DELETE") {
            csrfToken()?.let { b.header("X-XSRF-TOKEN", it) }
        }
        val payload = body?.toString()
        val media = "application/json".toMediaType()
        when (method) {
            "GET" -> b.get()
            "POST" -> b.post((payload ?: "{}").toRequestBody(media))
            "PUT" -> b.put((payload ?: "{}").toRequestBody(media))
            "DELETE" -> b.delete()
        }
        client.newCall(b.build()).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                val root = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
                val msg = root.str("message")
                val errs = root?.get("errors")?.let { e ->
                    (e as? JsonObject)?.entries?.associate { entry ->
                        entry.key to ((entry.value as? kotlinx.serialization.json.JsonArray)
                            ?.mapNotNull { v -> v.jsonPrim() }
                            ?: listOf(entry.value.jsonPrim() ?: ""))
                    } ?: emptyMap()
                } ?: emptyMap()
                throw ApiException(resp.code, msg ?: "Request failed (${resp.code})", errs)
            }
            return text
        }
    }

    // ---------- endpoints ----------
    suspend fun me(): String = withContext(Dispatchers.IO) { req("GET", "/api/me") }
    suspend fun login(email: String, password: String): String =
        withContext(Dispatchers.IO) {
            req("POST", "/api/login", buildJsonObject {
                put("email", email); put("password", password)
            })
        }

    suspend fun logout(): String = withContext(Dispatchers.IO) { req("POST", "/api/logout") }
    suspend fun dashboard(): String = withContext(Dispatchers.IO) { req("GET", "/api/dashboard") }
    suspend fun plans(): String = withContext(Dispatchers.IO) { req("GET", "/api/plans") }
    suspend fun invest(planId: Int, amount: Double): String =
        withContext(Dispatchers.IO) {
            req("POST", "/api/plans/$planId/invest", buildJsonObject { put("amount", amount) })
        }

    suspend fun investments(page: Int = 1): String =
        withContext(Dispatchers.IO) { req("GET", "/api/investments?page=$page") }

    suspend fun transactions(): String = withContext(Dispatchers.IO) { req("GET", "/api/transactions") }
    suspend fun depositMeta(): String = withContext(Dispatchers.IO) { req("GET", "/api/deposit") }
    suspend fun depositChains(): String = withContext(Dispatchers.IO) { req("GET", "/api/deposit/chains") }
    suspend fun depositCrypto(token: String, chain: String): String =
        withContext(Dispatchers.IO) {
            req("POST", "/api/deposit/crypto", buildJsonObject {
                put("token", token); put("chain", chain)
            })
        }

    suspend fun withdrawMeta(): String = withContext(Dispatchers.IO) { req("GET", "/api/withdraw") }
    suspend fun withdraw(amount: Double): String =
        withContext(Dispatchers.IO) {
            req("POST", "/api/withdraw", buildJsonObject { put("amount", amount) })
        }

    suspend fun profile(): String = withContext(Dispatchers.IO) { req("GET", "/api/profile") }
}

// tiny json helpers
fun kotlinx.serialization.json.JsonElement?.jsonPrim(): String? =
    (this as? kotlinx.serialization.json.JsonPrimitive)?.content

fun JsonObject.str(k: String): String? = this[k].jsonPrim()
fun JsonObject.dbl(k: String): Double = this[k]?.let {
    (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
} ?: 0.0
