package com.investly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import com.investly.app.ui.InvestlyApp
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = AppViewModel(applicationContext)

        setContent {
            LaunchedEffect(Unit) {
                vm.startNetworkWatch { vm.refreshAll(force = true) }
                vm.startPolling {
                    buildList {
                        when (vm.currentTab) {
                            "home" -> add("dashboard")
                            "plans" -> add("plans")
                            "deposit" -> add("deposit")
                            "activity" -> add("transactions")
                            "profile" -> add("profile")
                        }
                    }
                }
                vm.bootstrap()
            }

            // auto-refresh when internet comes back
            LaunchedEffect(Unit) {
                var prev = vm.online.first()
                while (true) {
                    val now = vm.online.first()
                    if (!prev && now) vm.refreshAll(force = true)
                    prev = now
                }
            }

            InvestlyApp(vm)
        }
    }
}
