package com.investly.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

data class UserData(
    val name: String = "",
    val email: String = "",
    val balance: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalInvested: Double = 0.0
) {
    val initials: String get() = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
}

class AppViewModel(context: Context) {

    val api = Api(context)

    var loggedIn by mutableStateOf<Boolean?>(null)
        private set
    var user by mutableStateOf(UserData())
    var offline by mutableStateOf(false)
        private set
    var toast by mutableStateOf<String?>(null)
    var unreadNotifications by mutableStateOf(0)
        private set
    var currentTab by mutableStateOf("home")

    /** scope for UI-triggered async work */
    val uiScope = CoroutineScope(Dispatchers.Main)

    /** raw json per endpoint, memory + disk */
    private val mem = mutableMapOf<String, JsonObject>()
    private val scope = CoroutineScope(Dispatchers.Main)
    private val jobs = mutableMapOf<String, Job>()

    private val _cacheVersion = MutableStateFlow(0)
    val cacheVersion: StateFlow<Int> = _cacheVersion

    // ---------- connectivity ----------
    private val _online = MutableStateFlow(true)
    val online: StateFlow<Boolean> = _online

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun startNetworkWatch(onRegained: () -> Unit) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val was = _online.value
                _online.value = true
                if (!was) onRegained()
            }

            override fun onLost(network: Network) {
                _online.value = hasInternet()
            }
        }
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        cm.registerNetworkCallback(req, callback)
        _online.value = hasInternet()
    }

    fun hasInternet(): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ---------- session ----------
    fun bootstrap() {
        scope.launch {
            loggedIn = null
            try {
                val body = api.me().obj()
                if (body.str("user") != null || body["id"] != null) {
                    applyUser(body)
                    loggedIn = true
                    refreshAll(force = false)
                } else {
                    loggedIn = false
                }
            } catch (e: Exception) {
                // offline but maybe cached session cookie exists -> try dashboard cache presence
                loggedIn = readJson("dashboard") != null
                offline = !hasInternet()
            }
        }
    }

    fun login(email: String, password: String, onDone: (String?) -> Unit) {
        scope.launch {
            try {
                val body = api.login(email.trim(), password).obj()
                applyUser(body)
                loggedIn = true
                refreshAll(force = true)
                onDone(null)
            } catch (e: ApiException) {
                onDone(e.errors.values.flatten().firstOrNull() ?: e.message)
            } catch (e: Exception) {
                onDone("Cannot reach server. Check Wi-Fi connection.")
            }
        }
    }

    fun logout() {
        scope.launch {
            runCatching { api.logout() }
            api.clearSession()
            mem.clear()
            user = UserData()
            loggedIn = false
            _cacheVersion.value++
        }
    }

    private fun applyUser(body: JsonObject) {
        val u = body["user"]?.let { it as? JsonObject } ?: body
        user = UserData(
            name = u.str("name") ?: "User",
            email = u.str("email") ?: "",
            balance = u.dbl("balance"),
            totalProfit = u.dbl("total_profit"),
            totalInvested = u.dbl("total_invested")
        )
    }

    // ---------- fetch / cache ----------
    fun cached(key: String): JsonObject? = mem[key] ?: readJson(key)

    private fun readJson(key: String): JsonObject? =
        api.readCache(key)?.let { runCatching { Api.json.parseToJsonElement(it) }.getOrNull() as? JsonObject }

    fun refresh(key: String, force: Boolean = false) {
        if (jobs[key]?.isActive == true && !force) return
        jobs[key]?.cancel()
        jobs[key] = scope.launch {
            try {
                val text = when (key) {
                    "dashboard" -> api.dashboard()
                    "plans" -> api.plans()
                    "investments" -> api.investments()
                    "transactions" -> api.transactions()
                    "deposit" -> api.depositMeta()
                    "profile" -> api.profile()
                    else -> return@launch
                }
                offline = false
                val obj = Api.json.parseToJsonElement(text) as? JsonObject ?: return@launch
                mem[key] = obj
                api.writeCache(key, text)
                if (key == "dashboard") {
                    obj["user"]?.let { applyUser(objOf(it)) }
                    unreadNotifications = (obj["unread_notifications"].toString().toIntOrNull()) ?: 0
                }
                if (key == "profile") obj["user"]?.let { applyUser(objOf(it)) }
                _cacheVersion.value++
            } catch (e: ApiException) {
                if (e.httpCode == 401 || e.httpCode == 419) {
                    loggedIn = false
                }
            } catch (e: Exception) {
                offline = true
                _cacheVersion.value++
            }
        }
    }

    fun refreshAll(force: Boolean) {
        listOf("dashboard", "plans", "investments", "transactions", "deposit", "profile").forEach {
            refresh(it, force)
        }
    }

    /** poll every 5 seconds */
    fun startPolling(keysProvider: () -> List<String>) {
        scope.launch {
            while (isActive) {
                delay(5000)
                if (_online.value && loggedIn == true) keysProvider().forEach { refresh(it) }
            }
        }
    }

    suspend fun invest(planId: Int, amount: Double): Result<String> {
        return try {
            val msg = api.invest(planId, amount).obj().str("message") ?: "Plan activated!"
            refresh("investments", force = true)
            refresh("dashboard", force = true)
            Result.success(msg)
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun withdraw(amount: Double): Result<String> {
        return try {
            val msg = api.withdraw(amount).obj().str("message") ?: "Withdrawal requested"
            refresh("transactions", force = true)
            refresh("dashboard", force = true)
            Result.success(msg)
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun depositAddress(token: String, chain: String): Result<JsonObject> {
        return try {
            Result.success(api.depositCrypto(token, chain).obj())
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun objOf(el: kotlinx.serialization.json.JsonElement): JsonObject = el as? JsonObject ?: JsonObject(emptyMap())
    }
}

fun JsonElement.obj(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())
fun JsonElement.arr(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())
