package com.investly.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.investly.app.AppViewModel
import com.investly.app.UserData
import com.investly.app.arr
import com.investly.app.dbl
import com.investly.app.obj
import com.investly.app.str
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private fun fmt(v: Double): String = String.format("%,.2f", v)
private fun fmt0(v: Double): String = String.format("%,.0f", v)
private fun trimNum(v: Double): String =
    if (v % 1.0 == 0.0) String.format("%.0f", v) else String.format("%.1f", v)

/* ============ root ============ */

@Composable
fun InvestlyApp(vm: AppViewModel) {
    val loggedIn = vm.loggedIn
    Surface(color = SurfaceBg, modifier = Modifier.fillMaxSize()) {
        when (loggedIn) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
            false -> LoginScreen(vm)
            else -> MainShell(vm)
        }
    }
}

/* ============ login ============ */

@Composable
fun LoginScreen(vm: AppViewModel) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2D39D5), Color(0xFF1A2185))))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(84.dp))
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(IcTrend, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
        Spacer(Modifier.height(18.dp))
        Text("Investly", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text("Grow your money every day", color = Color.White.copy(alpha = .7f), fontSize = 13.sp)
        Spacer(Modifier.height(44.dp))

        Card(shape = RoundedCornerShape(24.dp), colors = CardColors()) {
            Column(Modifier.padding(22.dp)) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it; err = null },
                    label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp), colors = FieldColors()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it; err = null },
                    label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp), colors = FieldColors()
                )
                err?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        busy = true
                        vm.login(email, pass) { e -> err = e; busy = false }
                    },
                    enabled = !busy && email.isNotBlank() && pass.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("SIGN IN", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

/* ============ shell + pill nav ============ */

@Composable
fun MainShell(vm: AppViewModel) {
    var tab by remember { mutableStateOf("home") }
    vm.currentTab = tab
    var overlay by remember { mutableStateOf<String?>(null) }
    var snackbar by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    fun go(t: String) {
        tab = t; overlay = null
        val key = when (t) {
            "home" -> "dashboard"; "plans" -> "plans"; "deposit" -> "deposit"
            "activity" -> "transactions"; else -> "profile"
        }
        vm.refresh(key, force = true)
        if (!vm.online.value) snackbar = "Offline — will refresh automatically" to false
    }

    LaunchedEffect(snackbar?.first) { snackbar?.let { delay(2600); snackbar = null } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OfflineBanner(vm)

            Box(Modifier.weight(1f)) {
                val o = overlay
                when {
                    o == null -> when (tab) {
                        "home" -> HomeScreen(vm, onWithdraw = { overlay = "withdraw" }, onDeposit = { go("deposit") })
                        "plans" -> PlansScreen(vm, onOpenCat = { overlay = "cat:$it" })
                        "deposit" -> DepositScreen(vm, onToast = { m, ok -> snackbar = m to ok })
                        "activity" -> ActivityScreen(vm)
                        else -> ProfileScreen(vm)
                    }
                    o == "withdraw" -> WithdrawScreen(vm,
                        onBack = { overlay = null },
                        onToast = { m, ok -> snackbar = m to ok })
                    o.startsWith("cat:") -> CategoryScreen(vm, catName = o.removePrefix("cat:"),
                        onBack = { overlay = null },
                        onOpenPlan = { id -> overlay = "plan:$id" })
                    o.startsWith("plan:") -> PlanDetailScreen(vm,
                        planId = o.removePrefix("plan:").toIntOrNull() ?: -1,
                        onBack = { overlay = null },
                        onDone = { m, ok ->
                            snackbar = m to ok
                            if (ok) overlay = null
                        })
                }
            }

            PillNav(tab, onTab = ::go)
            Spacer(Modifier.height(8.dp))
        }

        snackbar?.let { (msg, ok) ->
            Snackbar(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp, start = 16.dp, end = 16.dp),
                containerColor = if (ok) Color(0xFF123524) else DangerRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp)
            ) { Text(msg, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
fun PillNav(current: String, onTab: (String) -> Unit) {
    val items = listOf(
        Triple("home", IcHome, "Home"),
        Triple("plans", IcTrend, "Invest"),
        Triple("deposit", IcWallet, "Deposit"),
        Triple("activity", IcSwap, "Activity"),
        Triple("profile", IcPerson, "Profile")
    )
    Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (key, ic, label) ->
                val active = current == key
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onTab(key) }
                        .background(if (active) SurfaceBg else Color.Transparent)
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Icon(ic, label, tint = if (active) InkDark else MutedGray, modifier = Modifier.size(20.dp))
                    Text(label, fontSize = 9.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) InkDark else MutedGray)
                }
            }
        }
    }
}

@Composable
fun OfflineBanner(vm: AppViewModel) {
    val online by vm.online.collectAsState()
    if (!online) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFFFF4D6)).padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Offline — showing last saved data", fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, color = Color(0xFF8A6D00))
        }
    }
}

/* ============ HOME ============ */

@Composable
fun HomeScreen(vm: AppViewModel, onWithdraw: () -> Unit, onDeposit: () -> Unit) {
    vm.cached("dashboard") // touch cache so offline works
    val u = vm.user

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        HomeHeader(u)
        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), colors = CardColors()) {
            Column(Modifier.padding(20.dp)) {
                Text("$${fmt(u.balance)}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = InkDark)
                Spacer(Modifier.height(4.dp))
                Text("Total Balance   ·   +$${fmt(u.totalProfit)} profit",
                    fontSize = 12.sp, color = MutedGray)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("Deposit", "Add funds", IcWallet, SuccessGreen, Modifier.weight(1f), onClick = onDeposit)
            ActionCard("Withdraw", "Cash out", IcArrowUpRight, DangerRed, Modifier.weight(1f), onClick = onWithdraw)
        }

        Spacer(Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(20.dp), colors = CardColors()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Budget", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkDark)
                    Text("Invested this period", fontSize = 11.sp, color = MutedGray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$${fmt(u.totalInvested)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkDark)
                    Text("$${fmt(u.balance)} left", fontSize = 11.sp, color = MutedGray)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Today", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkDark)
        Spacer(Modifier.height(10.dp))

        val txs = vm.cached("dashboard")?.get("transactions")?.arr() ?: emptyList()
        if (txs.isEmpty()) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardColors()) {
                Text("No transactions yet", Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = TextAlign.Center, color = MutedGray, fontSize = 13.sp)
            }
        } else {
            txs.take(5).forEach { t ->
                TxRow(t.obj())
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
fun HomeHeader(u: UserData) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Welcome back,", fontSize = 12.sp, color = MutedGray)
            Text(u.name.ifBlank { "Investor" }, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = InkDark)
        }
        Box(Modifier.size(42.dp).clip(CircleShape).background(BrandBlue), contentAlignment = Alignment.Center) {
            Text(u.initials.ifBlank { "?" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun ActionCard(title: String, sub: String, icon: ImageVector, tint: Color, mod: Modifier, onClick: () -> Unit) {
    Card(mod, shape = RoundedCornerShape(20.dp), colors = CardColors(), onClick = onClick) {
        Column(Modifier.padding(vertical = 18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(tint), contentAlignment = Alignment.Center) {
                Icon(icon, title, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InkDark)
            Text(sub, fontSize = 10.sp, color = MutedGray)
        }
    }
}

@Composable
fun TxRow(t: Map<String, kotlinx.serialization.json.JsonElement>) {
    val type = t.str("type") ?: ""
    val amount = t.dbl("amount")
    val isW = type == "withdrawal"
    val tint = when {
        isW -> Color(0xFFFEE7E8); type == "deposit" -> Color(0xFFDFF7F4); else -> SurfaceBg
    }
    val fg = when {
        isW -> DangerRed; type == "deposit" -> TealAccent; else -> BrandBlue
    }
    Card(shape = RoundedCornerShape(16.dp), colors = CardColors()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                Icon(if (isW) IcArrowUpRight else IcWallet, null, tint = fg, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(type.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InkDark)
                Text((t.str("created_at") ?: "").take(16).replace('T', ' '), fontSize = 10.sp, color = MutedGray)
            }
            Text(
                "${if (isW) "-" else "+"}$${
                    String.format("%.2f", amount)
                }",
                fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = if (isW) DangerRed else SuccessGreen
            )
        }
    }
}

/* ============ PLANS ============ */

@Composable
fun PlansScreen(vm: AppViewModel, onOpenCat: (String) -> Unit) {
    val d = vm.cached("plans")
    val plans = d?.get("plans")?.arr() ?: emptyList()
    val activeCats = d?.get("active_categories")?.arr()
        ?.mapNotNull { it.toString().filter { c -> c.isDigit() || c == '-' || c == '.' }.toDoubleOrNull()?.toInt() }
        ?: emptyList()

    val order = listOf("Daily", "2-Day", "3-Day", "Weekly", "Bi-Weekly", "Monthly", "60-Day", "90-Day")
    val icons: Map<String, ImageVector> = mapOf(
        "Daily" to IcTrend, "2-Day" to IcSwap, "3-Day" to IcArrowUpRight, "Weekly" to IcTrend,
        "Bi-Weekly" to IcHome, "Monthly" to IcWallet, "60-Day" to IcCheckCircle, "90-Day" to IcStar
    )

    Column(Modifier.fillMaxSize()) {
        PageTitle("Plans", subtitle = "Pick a bundle & activate instantly")

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            order.forEach { cat ->
                val catPlans = plans.map { it.obj() }.filter { it.str("category") == cat }
                if (catPlans.isNotEmpty()) item(cat) {
                    val locked = activeCats.contains(catPlans.first().dbl("duration_days").toInt())
                    val maxRet = catPlans.maxOf { it.dbl("interest_rate") }
                    val minAmt = catPlans.minOf { it.dbl("min_amount") }
                    val days = catPlans.first().dbl("duration_days").toInt()
                    val col = CatColor(cat)
                    Card(
                        shape = RoundedCornerShape(20.dp), colors = CardColors(),
                        onClick = { onOpenCat(cat) },
                        modifier = if (locked) Modifier.alpha(0.8f) else Modifier
                    ) {
                        Column(
                            Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(col),
                                contentAlignment = Alignment.Center) {
                                Icon(icons[cat] ?: IcTrend, cat, tint = Color.White, modifier = Modifier.size(23.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(cat, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkDark)
                            Text("${days}d · ${catPlans.size} plans · from $${fmt0(minAmt)}",
                                fontSize = 9.sp, color = MutedGray, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(6.dp))
                            Text("+${trimNum(maxRet)}%", fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold, color = col)
                            Text("MAX RETURN", fontSize = 8.sp, letterSpacing = 1.sp, color = MutedGray)
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (locked) Color(0xFFDCFCE7) else col)
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(if (locked) "ACTIVE" else "VIEW", fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (locked) Color(0xFF15803D) else Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

val IcStar: ImageVector get() = IcTrend

fun CatColor(cat: String): Color = when (cat) {
    "Daily" -> Color(0xFF2F62EB)
    "2-Day" -> Color(0xFF7C3AED)
    "3-Day" -> Color(0xFFEC4899)
    "Weekly" -> Color(0xFFF59E0B)
    "Bi-Weekly" -> Color(0xFF10B981)
    "Monthly" -> Color(0xFF0EA5E9)
    "60-Day" -> Color(0xFF6366F1)
    else -> Color(0xFFF97316)
}

/* ============ CATEGORY ============ */

@Composable
fun CategoryScreen(vm: AppViewModel, catName: String, onBack: () -> Unit, onOpenPlan: (Int) -> Unit) {
    val d = vm.cached("plans")
    val plans = (d?.get("plans")?.arr() ?: emptyList()).map { it.obj() }
        .filter { it.str("category") == catName }.sortedBy { it.dbl("min_amount") }
    val activeCats = d?.get("active_categories")?.arr()
        ?.mapNotNull { it.toString().toDoubleOrNull()?.toInt() } ?: emptyList()
    val locked = activeCats.contains(plans.firstOrNull()?.dbl("duration_days")?.toInt())

    Column(Modifier.fillMaxSize()) {
        SubPageHeader(catName, onBack)
        if (locked) {
            Box(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)).background(Color(0xFFDCFCE7))
                    .padding(vertical = 10.dp)
            ) {
                Text("You already have an active $catName plan",
                    Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    color = Color(0xFF15803D), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            plans.forEach { p ->
                Spacer(Modifier.height(12.dp))
                PlanGridCard(p, onClick = { onOpenPlan(p.dbl("id").toInt()) })
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun PlanGridCard(p: Map<String, kotlinx.serialization.json.JsonElement>, onClick: () -> Unit) {
    val col = hexColor(p.str("color"))
    Card(shape = RoundedCornerShape(20.dp), colors = CardColors(), onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(col), contentAlignment = Alignment.Center) {
                    Icon(IcTrend, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.str("name") ?: "", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkDark)
                    p.str("badge")?.let {
                        Text(it, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandBlue, letterSpacing = 1.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("+${trimNum(p.dbl("interest_rate"))}%", fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold, color = BrandBlue, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${p.dbl("duration_days").toInt()} days", fontSize = 11.sp, color = MutedGray)
                Text("$${fmt0(p.dbl("min_amount"))}", fontSize = 11.sp, color = MutedGray)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(BrandBlue)
                    .padding(vertical = 10.dp)
            ) {
                Text("ACTIVATE NOW", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
        }
    }
}

/* ============ PLAN DETAIL ============ */

@Composable
fun PlanDetailScreen(vm: AppViewModel, planId: Int, onBack: () -> Unit, onDone: (String, Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val d = vm.cached("plans")
    val p = (d?.get("plans")?.arr() ?: emptyList()).map { it.obj() }
        .firstOrNull { it.dbl("id").toInt() == planId }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        SubPageHeader(p?.str("name") ?: "Plan", onBack)

        if (p == null) {
            Text("Plan not found — go back and refresh.", color = MutedGray, fontSize = 13.sp)
            return@Column
        }

        val price = p.dbl("min_amount")
        val rate = p.dbl("interest_rate")
        val days = p.dbl("duration_days").toInt()
        val profit = price * rate / 100.0
        val total = price + profit
        val enough = vm.user.balance >= price
        val col = hexColor(p.str("color"))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier.background(
                Brush.linearGradient(listOf(col, col.copy(alpha = 0.82f))), RoundedCornerShape(24.dp))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                        Icon(IcTrend, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    p.str("badge")?.let {
                        Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .22f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Text(it.uppercase(), color = Color.White, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(p.str("name") ?: "", fontSize = 12.sp, color = Color.White.copy(alpha = .85f), letterSpacing = 2.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("+${trimNum(rate)}%", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("in ${days} day${if (days > 1) "s" else ""}", fontSize = 13.sp,
                        color = Color.White.copy(alpha = .85f), modifier = Modifier.padding(bottom = 7.dp))
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = .25f))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Package price", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
                    Text("$${fmt0(price)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(shape = RoundedCornerShape(20.dp), colors = CardColors()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                InfoLine("You invest", "$${fmt(price)}")
                InfoLine("Expected profit", "+$${fmt(profit)}", valueColor = SuccessGreen)
                InfoLine("Payout after ${days}d", "$${fmt(total)}")
                HorizontalDivider(color = Color(0xFFEEF0F3))
                InfoLine("Your balance", "$${fmt(vm.user.balance)}",
                    valueColor = if (enough) SuccessGreen else DangerRed)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (enough) {
            var busy by remember(planId) { mutableStateOf(false) }
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        vm.invest(planId, price).fold(
                            onSuccess = { m -> onDone(m, true) },
                            onFailure = { e -> onDone(e.message ?: "Failed", false) }
                        )
                        busy = false
                    }
                },
                enabled = !busy,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("ACTIVATE NOW · $${fmt0(price)}", fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp, fontSize = 13.sp)
            }
        } else {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(FieldBg).padding(vertical = 15.dp)) {
                Text("Deposit $${fmt0(price)} to activate", Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, color = InkDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Instant activation · Auto payout on maturity",
            Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MutedGray, fontSize = 10.sp)
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun InfoLine(label: String, value: String, valueColor: Color = InkDark) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedGray, fontSize = 13.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/* ============ DEPOSIT ============ */

@Composable
fun DepositScreen(vm: AppViewModel, onToast: (String, Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val meta = vm.cached("deposit")
    var selected by remember { mutableStateOf<String?>(null) }
    var addr by remember { mutableStateOf<JsonObject?>(null) }
    var busy by remember { mutableStateOf(false) }

    val tokens = (meta?.get("crypto")?.obj()?.get("tokens")?.arr() ?: emptyList()).map { it.obj() }
    val cryptoObj = meta?.get("crypto")?.obj()
    val configured = cryptoObj != null && cryptoObj["configured"].toString() == "true"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        PageTitle("Deposit", subtitle = "Min $5 · USDT crypto credited automatically")

        Card(shape = RoundedCornerShape(20.dp), colors = CardColors()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your Balance", fontSize = 11.sp, color = MutedGray)
                    Text("$${fmt(vm.user.balance)}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = InkDark)
                }
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFE8EDFB)),
                    contentAlignment = Alignment.Center) {
                    Icon(IcWallet, null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), colors = CardColors()) {
            Column(Modifier.padding(18.dp)) {
                if (!configured && tokens.isEmpty()) {
                    Text("Crypto deposits not enabled yet.", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InkDark)
                    Spacer(Modifier.height(6.dp))
                    Text(cryptoObj?.str("error") ?: "Administrator has not configured the payment gateway.",
                        fontSize = 12.sp, color = MutedGray)
                } else {
                    Text("SELECT COIN", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp, color = InkDark)
                    Spacer(Modifier.height(10.dp))
                    tokens.forEach { tk ->
                        val key = "${tk.str("token")}:${tk.str("chain")}"
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(if (selected == key) Color(0xFFE8EDFB) else SurfaceBg)
                                .clickable { selected = key; addr = null }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(IcWallet, null, tint = BrandBlue, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(tk.str("label") ?: key, fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp, color = InkDark)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            val sel = selected ?: return@Button
                            val parts = sel.split(":")
                            busy = true
                            scope.launch {
                                vm.depositAddress(parts[0], parts.getOrNull(1) ?: "").fold(
                                    onSuccess = { a ->
                                        addr = a
                                        onToast(a.str("address")?.let { "Address ready — send min $5" }
                                            ?: (a.str("message") ?: "No address"), a.str("address") != null)
                                    },
                                    onFailure = { e -> onToast(e.message ?: "Failed", false) }
                                )
                                busy = false
                            }
                        },
                        enabled = selected != null && !busy,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("GET DEPOSIT ADDRESS", fontWeight = FontWeight.Bold,
                            fontSize = 12.sp, letterSpacing = 1.5.sp)
                    }
                    addr?.let { a ->
                        Spacer(Modifier.height(14.dp))
                        val address = a.str("address") ?: ""
                        Text("SEND ONLY ${(selected ?: "").split(":").firstOrNull()?.uppercase()} TO:",
                            fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFFB8860B), letterSpacing = 1.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(address.ifBlank { a.str("message") ?: "-" },
                            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkDark)
                        Spacer(Modifier.height(10.dp))
                        Text("Minimum deposit is $5. Funds are credited automatically after network confirmation.",
                            fontSize = 10.sp, color = MutedGray)
                    }
                }
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

/* ============ ACTIVITY ============ */

@Composable
fun ActivityScreen(vm: AppViewModel) {
    val d = vm.cached("transactions")
    val txs = d?.get("data")?.arr()?.map { it.obj() }
        ?: (d?.get("transactions")?.arr() ?: emptyList()).map { it.obj() }

    Column(Modifier.fillMaxSize()) {
        PageTitle("Activity", subtitle = "All transactions")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (txs.isEmpty()) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardColors()) {
                    Text("No transactions yet", Modifier.fillMaxWidth().padding(26.dp),
                        textAlign = TextAlign.Center, color = MutedGray, fontSize = 13.sp)
                }
            } else txs.forEach { t ->
                TxRow(t)
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(110.dp))
        }
    }
}

/* ============ PROFILE ============ */

@Composable
fun ProfileScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        PageTitle("Profile", subtitle = vm.user.email)
        Card(shape = RoundedCornerShape(20.dp), colors = CardColors()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(74.dp).clip(CircleShape).background(BrandBlue), contentAlignment = Alignment.Center) {
                    Text(vm.user.initials.ifBlank { "?" }, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text(vm.user.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = InkDark)
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(20.dp), colors = CardColors()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoLine("Balance", "$${fmt(vm.user.balance)}")
                InfoLine("Total invested", "$${fmt(vm.user.totalInvested)}")
                InfoLine("Total profit", "+$${fmt(vm.user.totalProfit)}", valueColor = SuccessGreen)
            }
        }
        Spacer(Modifier.height(16.dp))
        var confirm by remember { mutableStateOf(false) }
        if (!confirm) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFFEF2F2))
                    .clickable { confirm = true }.padding(vertical = 16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(IcLogout, null, tint = DangerRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(SurfaceBg)
                    .clickable { confirm = false }.padding(vertical = 15.dp)) {
                    Text("Cancel", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        color = InkDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(DangerRed)
                    .clickable { vm.logout() }.padding(vertical = 15.dp)) {
                    Text("Yes, sign out", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

/* ============ WITHDRAW ============ */

@Composable
fun WithdrawScreen(vm: AppViewModel, onBack: () -> Unit, onToast: (String, Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        SubPageHeader("Withdraw", onBack)
        Card(shape = RoundedCornerShape(20.dp), colors = CardColors()) {
            Column(Modifier.padding(20.dp)) {
                Text("Available balance", fontSize = 11.sp, color = MutedGray)
                Text("$${fmt(vm.user.balance)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = InkDark)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (USD)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = FieldColors()
                )
                Spacer(Modifier.height(8.dp))
                Text("Minimum withdrawal $5 · processed within 24h", fontSize = 10.sp, color = MutedGray)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val amt = amount.toDoubleOrNull() ?: return@Button
                busy = true
                scope.launch {
                    vm.withdraw(amt).fold(
                        onSuccess = { m -> onToast(m, true); onBack() },
                        onFailure = { e -> onToast(e.message ?: "Failed", false) }
                    )
                    busy = false
                }
            },
            enabled = !busy && (amount.toDoubleOrNull() ?: 0.0) >= 5,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text("REQUEST WITHDRAWAL", fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp)
        }
        Spacer(Modifier.height(100.dp))
    }
}

/* ============ shared pieces ============ */

@Composable
fun PageTitle(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 12.dp)) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = InkDark)
        subtitle?.let { Text(it, fontSize = 12.sp, color = MutedGray) }
    }
}

@Composable
fun SubPageHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(Color.White).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(IcBack, "back", tint = InkDark, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkDark)
    }
}

@Composable
fun CardColors() = CardDefaults.cardColors(containerColor = CardWhite)

@Composable
fun FieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandBlue,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = FieldBg,
    unfocusedContainerColor = FieldBg,
    cursorColor = BrandBlue
)

fun hexColor(hex: String?): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex ?: "#2F62EB"))
}.getOrDefault(BrandBlue)
