package ua.mypershymillion

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MillionTheme { MillionApp() } }
    }
}

private val Gold = Color(0xFFC79216)
private val GoldLight = Color(0xFFFFD85A)
private val GoldPale = Color(0xFFFFF3C7)
private val Green = Color(0xFF173D32)
private val Cream = Color(0xFFFFFBF2)

@Composable
fun MillionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Gold,
            secondary = Green,
            background = Cream,
            surface = Color.White
        ),
        content = content
    )
}

data class SavingEntry(val date: String, val amount: Int)

private const val PREFS = "million_prefs"
private const val KEY_CURRENT = "current"
private const val KEY_TARGET = "target"
private const val KEY_DAILY = "daily"
private const val KEY_YEARS = "years"
private const val KEY_HISTORY = "history"

private class SavingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun current() = prefs.getInt(KEY_CURRENT, 0)
    fun target() = prefs.getInt(KEY_TARGET, 1_000_000)
    fun daily() = prefs.getInt(KEY_DAILY, 20)
    fun years() = prefs.getInt(KEY_YEARS, 3)

    fun saveSettings(current: Int, target: Int, daily: Int, years: Int) {
        prefs.edit()
            .putInt(KEY_CURRENT, current.coerceAtLeast(0))
            .putInt(KEY_TARGET, target.coerceAtLeast(0))
            .putInt(KEY_DAILY, daily.coerceAtLeast(0))
            .putInt(KEY_YEARS, years.coerceAtLeast(0))
            .apply()
    }

    fun history(): List<SavingEntry> {
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size == 2) {
                val date = parts[0].takeIf { it.isNotBlank() }
                val amount = parts[1].toIntOrNull()
                if (date != null && amount != null) SavingEntry(date, amount) else null
            } else null
        }
    }

    fun saveHistory(history: List<SavingEntry>) {
        prefs.edit().putString(
            KEY_HISTORY,
            history.joinToString("\n") { "${it.date}|${it.amount}" }
        ).apply()
    }
}

private fun milestoneList(target: Int): List<Int> {
    val fixed = listOf(10_000, 50_000, 100_000, 250_000, 500_000, 1_000_000)
        .filter { it < target }
    return (fixed + target).distinct().sorted()
}

private fun milestoneLabel(value: Int, target: Int): String = when {
    value == target && target == 1_000_000 -> "Перший мільйон 👑"
    value == target -> "Твоя ціль 🎯"
    value == 1_000_000 -> "Перший мільйон 👑"
    else -> "%,d ₴".format(value)
}

private fun contributionLabel(amount: Int): String = when {
    amount < 500 -> "🌱 Маленький крок"
    amount < 1_000 -> "💪 Впевнений крок"
    amount < 3_000 -> "🔥 Хороший внесок"
    amount < 5_000 -> "🚀 Сильний внесок"
    amount < 10_000 -> "💎 Серйозний внесок"
    else -> "👑 Великий крок"
}

private enum class AppTab(val title: String) {
    HOME("Головна"),
    ADD("Додати"),
    PATH("До цілі"),
    HISTORY("Історія")
}

private enum class ContributionUnit { DAY, MONTH }
private enum class TermUnit { YEAR, MONTH }

@Composable
fun MillionApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { SavingsStore(context.applicationContext) }

    var current by remember { mutableIntStateOf(store.current()) }
    var target by remember { mutableIntStateOf(store.target()) }
    var daily by remember { mutableIntStateOf(store.daily()) }
    var monthly by remember { mutableIntStateOf(0) }
    var years by remember { mutableIntStateOf(store.years()) }
    var months by remember { mutableIntStateOf(0) }
    var contributionUnit by remember { mutableStateOf(ContributionUnit.DAY) }
    var termUnit by remember { mutableStateOf(TermUnit.YEAR) }
    var targetText by remember { mutableStateOf(if (store.target() > 0) store.target().toString() else "") }
    var dailyText by remember { mutableStateOf(if (store.daily() > 0) store.daily().toString() else "") }
    var yearsText by remember { mutableStateOf(if (store.years() > 0) store.years().toString() else "") }
    var customAddText by remember { mutableStateOf("") }
    var selectedQuickAmount by remember { mutableIntStateOf(50) }
    var message by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(store.history()) }
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }

    fun persist() = store.saveSettings(current, target, daily, years)

    fun addSavings(amount: Int) {
        if (amount <= 0) return
        val old = current
        current += amount
        val entry = SavingEntry(
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date()),
            amount
        )
        history = listOf(entry) + history
        store.saveHistory(history)
        persist()

        val milestones = milestoneList(target)
        val oldMilestone = milestones.lastOrNull { old >= it } ?: 0
        val newMilestone = milestones.lastOrNull { current >= it } ?: 0
        message = if (newMilestone > oldMilestone) {
            when {
                newMilestone == 1_000_000 -> "👑 МІЛЬЙОН! Ти це зробив!"
                newMilestone == target -> "🎯 Ціль досягнуто! Наступна вершина попереду."
                newMilestone == 500_000 -> "🏆 500 000 ₴! Половина мільйона позаду."
                newMilestone == 250_000 -> "🚀 250 000 ₴! Чверть мільйона позаду."
                newMilestone == 100_000 -> "💪 100 000 ₴! Перша велика вершина."
                newMilestone == 50_000 -> "🔥 50 000 ₴! Уже серйозний прогрес."
                else -> "🎉 Нова вершина! Рухаємося далі."
            }
        } else {
            "${contributionLabel(amount)}  +%,d ₴. Ще один крок до цілі.".format(amount)
        }
    }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            MillionBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                AppTab.HOME -> HomeTab(
                    current = current,
                    target = target,
                    historyCount = history.size,
                    onAdd = { selectedTab = AppTab.ADD },
                    onPath = { selectedTab = AppTab.PATH },
                    onHistory = { selectedTab = AppTab.HISTORY },
                    onReset = { showResetDialog = true }
                )

                AppTab.ADD -> AddTab(
                    selectedQuickAmount = selectedQuickAmount,
                    customAddText = customAddText,
                    message = message,
                    onQuickAmount = {
                        selectedQuickAmount = it
                        customAddText = ""
                    },
                    onCustomAmount = {
                        customAddText = it.filter(Char::isDigit)
                        if (customAddText.isNotBlank()) selectedQuickAmount = 0
                    },
                    onAdd = {
                        val amount = customAddText.toIntOrNull()?.takeIf { it > 0 } ?: selectedQuickAmount
                        if (amount > 0) {
                            addSavings(amount)
                            customAddText = ""
                            selectedQuickAmount = 50
                        }
                    },
                    current = current
                )

                AppTab.PATH -> PathTab(
                    current = current,
                    target = target,
                    targetText = targetText,
                    dailyText = dailyText,
                    yearsText = yearsText,
                    daily = daily,
                    monthly = monthly,
                    years = years,
                    months = months,
                    contributionUnit = contributionUnit,
                    termUnit = termUnit,
                    onContributionUnitChange = { contributionUnit = it },
                    onTermUnitChange = { termUnit = it },
                    onTargetText = { raw ->
                        val digits = raw.filter(Char::isDigit)
                        val normalized = when {
                            digits.isEmpty() -> ""
                            digits.length > 1 -> digits.trimStart('0').ifEmpty { "0" }
                            else -> digits
                        }
                        targetText = normalized
                        target = normalized.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        persist()
                    },
                    onDaily = { raw ->
                        val digits = raw.filter(Char::isDigit)
                        val normalized = when {
                            digits.isEmpty() -> ""
                            digits.length > 1 -> digits.trimStart('0').ifEmpty { "0" }
                            else -> digits
                        }
                        dailyText = normalized
                        daily = normalized.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        persist()
                    },
                    onMonthly = { raw ->
                        val digits = raw.filter(Char::isDigit)
                        monthly = if (digits.isEmpty()) 0 else digits.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    },
                    onMonths = { raw ->
                        val digits = raw.filter(Char::isDigit)
                        months = if (digits.isEmpty()) 0 else digits.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    },
                    onYears = { raw ->
                        val digits = raw.filter(Char::isDigit)
                        val normalized = when {
                            digits.isEmpty() -> ""
                            digits.length > 1 -> digits.trimStart('0').ifEmpty { "0" }
                            else -> digits
                        }
                        yearsText = normalized
                        years = normalized.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        persist()
                    }
                )

                AppTab.HISTORY -> HistoryTab(history = history)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Обнулити шкалу?") },
            text = { Text("Сума на шкалі стане 0 ₴. Уся історія поповнень залишиться без змін.") },
            confirmButton = {
                TextButton(onClick = {
                    current = 0
                    persist()
                    message = "↺ Шкалу обнулено. Історія залишилася."
                    showResetDialog = false
                    selectedTab = AppTab.HOME
                }) {
                    Text("Обнулити", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
private fun MillionBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            icon = { Icon(Icons.Filled.Home, null) },
            label = { Text("Головна") }
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.ADD,
            onClick = { onTabSelected(AppTab.ADD) },
            icon = { Icon(Icons.Filled.AddCircle, null) },
            label = { Text("Додати") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Green,
                selectedTextColor = Green,
                indicatorColor = GoldPale
            )
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.PATH,
            onClick = { onTabSelected(AppTab.PATH) },
            icon = { Icon(Icons.Filled.Flag, null) },
            label = { Text("До цілі") }
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.HISTORY,
            onClick = { onTabSelected(AppTab.HISTORY) },
            icon = { Icon(Icons.Filled.History, null) },
            label = { Text("Історія") }
        )
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
private fun AppTitle() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Калькулятор мільйонера",
            fontSize = 29.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Gold
        )
        Spacer(Modifier.height(3.dp))
    }
}

@Composable
private fun HomeTab(
    current: Int,
    target: Int,
    historyCount: Int,
    onAdd: () -> Unit,
    onPath: () -> Unit,
    onHistory: () -> Unit,
    onReset: () -> Unit
) {
    val progress = (current.toDouble() / target.coerceAtLeast(1)).coerceIn(0.0, 1.0)
    val animatedProgress by animateFloatAsState(progress.toFloat(), tween(700), label = "homeProgress")
    val animatedCurrent by animateIntAsState(current, tween(500), label = "homeCurrent")

    ScreenColumn {
        AppTitle()

        Card(
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.5.dp, Gold),
            colors = CardDefaults.cardColors(containerColor = Green),
            modifier = Modifier.shadow(10.dp, RoundedCornerShape(26.dp))
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ТВОЇ ЗАОЩАДЖЕННЯ", color = Color.White.copy(.72f), fontSize = 12.sp)
                Text("%,d ₴".format(animatedCurrent), color = GoldLight, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(13.dp),
                    color = GoldLight,
                    trackColor = Color.White.copy(.16f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("%.1f%% шляху".format(progress * 100), color = Color.White.copy(.9f))
                    Text("ціль %,d ₴".format(target), color = Color.White.copy(.8f))
                }
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldLight,
                        contentColor = Green
                    )
                ) {
                    Icon(Icons.Filled.AddCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Додати гроші", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, Color.White.copy(.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("↺ Обнулити шкалу")
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeShortcut(
                icon = "🎯",
                title = "До цілі",
                subtitle = "Твій шлях",
                modifier = Modifier.weight(1f),
                onClick = onPath
            )
            HomeShortcut(
                icon = "📜",
                title = "Історія",
                subtitle = "$historyCount внесків",
                modifier = Modifier.weight(1f),
                onClick = onHistory
            )
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = GoldPale),
            border = BorderStroke(1.dp, Color(0xFFE4C66A))
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("✨ Твій наступний крок", fontWeight = FontWeight.ExtraBold, color = Color(0xFF8A650F))
                Spacer(Modifier.height(5.dp))
                Text(
                    if (progress >= 1f) "Ціль досягнуто. Час ставити нову вершину! 👑"
                    else "Відкрий «Додати» і зроби ще один внесок.",
                    color = Color(0xFF8A650F)
                )
            }
        }
    }
}

@Composable
private fun AddTab(
    selectedQuickAmount: Int,
    customAddText: String,
    message: String,
    onQuickAmount: (Int) -> Unit,
    onCustomAmount: (String) -> Unit,
    onAdd: () -> Unit,
    current: Int
) {
    ScreenColumn {
        AppTitle()
        SectionTitle("💰 Додати гроші", "Кожен внесок рухає шкалу вперед.")

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE4C66A))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Швидкий внесок", fontWeight = FontWeight.Bold, color = Green)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50, 100, 200).forEach { amount ->
                        QuickAmountButton(
                            amount = amount,
                            selected = selectedQuickAmount == amount,
                            onClick = { onQuickAmount(amount) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    if (customAddText.isBlank()) "Обрано: %,d ₴".format(selectedQuickAmount)
                    else "Власна сума: %,d ₴".format(customAddText.toIntOrNull() ?: 0),
                    color = Color(0xFF9A6A09),
                    fontWeight = FontWeight.SemiBold
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customAddText,
                        onValueChange = onCustomAmount,
                        label = { Text("Інша сума") },
                        suffix = if (showCurrency) ({ Text("₴") }) else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onAdd,
                        modifier = Modifier.align(Alignment.CenterVertically).height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        Text("Додати", fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (message.isNotBlank()) {
                    Text(message, color = Color(0xFF9A6A09), fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Green)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("На шкалі зараз", color = Color.White.copy(.7f))
                Text("%,d ₴".format(current), color = GoldLight, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("Шкала змінюється тільки після натискання «Додати».", color = Color.White.copy(.82f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PathTab(
    current: Int,
    target: Int,
    targetText: String,
    dailyText: String,
    yearsText: String,
    daily: Int,
    monthly: Int,
    years: Int,
    months: Int,
    contributionUnit: ContributionUnit,
    termUnit: TermUnit,
    onContributionUnitChange: (ContributionUnit) -> Unit,
    onTermUnitChange: (TermUnit) -> Unit,
    onTargetText: (String) -> Unit,
    onDaily: (String) -> Unit,
    onMonthly: (String) -> Unit,
    onMonths: (String) -> Unit,
    onYears: (String) -> Unit
) {
    val milestones = milestoneList(target)
    val next = milestones.firstOrNull { current < it }
    val previous = milestones.lastOrNull { current >= it } ?: 0
    val segmentProgress = if (next != null && next > previous)
        ((current - previous).toFloat() / (next - previous).toFloat()).coerceIn(0f, 1f)
    else 1f

    val hasTarget = target > 0
    val safeTarget = target.coerceAtLeast(1)
    val remaining = if (hasTarget) (target - current).coerceAtLeast(0) else 0
    val millionRemaining = (1_000_000 - current).coerceAtLeast(0)

    val effectiveDaily = when (contributionUnit) {
        ContributionUnit.DAY -> daily.toDouble()
        ContributionUnit.MONTH -> monthly.toDouble() / 30.0
    }

    val effectiveDays = when (termUnit) {
        TermUnit.YEAR -> years * 365
        TermUnit.MONTH -> months * 30
    }

    val hasTerm = when (termUnit) {
        TermUnit.YEAR -> years > 0
        TermUnit.MONTH -> months > 0
    }
    val hasContribution = when (contributionUnit) {
        ContributionUnit.DAY -> daily > 0
        ContributionUnit.MONTH -> monthly > 0
    }

    val neededDaily = if (hasTerm && hasTarget)
        remaining.toDouble() / effectiveDays.coerceAtLeast(1) else 0.0
    val neededMonthly = if (hasTerm && hasTarget)
        remaining.toDouble() / when (termUnit) {
            TermUnit.YEAR -> (years * 12).coerceAtLeast(1)
            TermUnit.MONTH -> months.coerceAtLeast(1)
        } else 0.0

    val millionDailyForYears = if (hasTerm)
        millionRemaining.toDouble() / effectiveDays.coerceAtLeast(1) else 0.0
    val millionMonthlyForYears = if (hasTerm)
        millionRemaining.toDouble() / when (termUnit) {
            TermUnit.YEAR -> (years * 12).coerceAtLeast(1)
            TermUnit.MONTH -> months.coerceAtLeast(1)
        } else 0.0

    val targetDays = if (effectiveDaily > 0 && hasTarget && remaining > 0)
        kotlin.math.ceil(remaining.toDouble() / effectiveDaily).toInt() else null
    val millionDays = if (effectiveDaily > 0 && millionRemaining > 0)
        kotlin.math.ceil(millionRemaining.toDouble() / effectiveDaily).toInt() else null
    val currentDaily = effectiveDaily.roundToInt()

    ScreenColumn {
        AppTitle()
        SectionTitle("До цілі", "")

        // 1. Milestones first
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE4C66A))
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "🏆 Вершини",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green
                )
                Text(
                    "Великі результати складаються з маленьких кроків.",
                    fontSize = 12.sp,
                    color = Color(0xFF64706B)
                )
                Spacer(Modifier.height(3.dp))

                milestones.forEach { value ->
                    val reached = current >= value
                    val isNext = value == next

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (isNext) GoldPale else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (reached) "✓" else if (isNext) "→" else "○",
                            fontSize = 20.sp,
                            color = if (reached) Gold else Color(0xFF8A9690),
                            modifier = Modifier.width(30.dp)
                        )
                        Text(
                            milestoneLabel(value, target),
                            Modifier.weight(1f),
                            fontWeight = if (reached || isNext) FontWeight.Bold else FontWeight.Medium,
                            color = if (reached) Gold else Color(0xFF26352F)
                        )
                        if (reached) {
                            Text(
                                "ЗАРАХОВАНО",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gold
                            )
                        }
                    }
                }

                if (target <= 0) {
                    Text(
                        "Вкажи свою ціль нижче, щоб додати її до вершин.",
                        fontSize = 12.sp,
                        color = Color(0xFF8A650F)
                    )
                }
            }
        }

        // 2. Goal settings
        SectionTitle(
            "⚙️ Налаштування цілі",
            "Калькулятор рахує шлях до твоєї цілі, яку ти вкажеш."
        )

        MoneyField(
            value = targetText,
            onChange = onTargetText,
            label = "Ціль",
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "Як хочеш рахувати?",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Green
        )

        UnitChoiceRow(
            firstLabel = "За день",
            secondLabel = "За місяць",
            firstSelected = contributionUnit == ContributionUnit.DAY,
            onFirst = { onContributionUnitChange(ContributionUnit.DAY) },
            onSecond = { onContributionUnitChange(ContributionUnit.MONTH) }
        )

        MoneyField(
            value = if (contributionUnit == ContributionUnit.DAY) dailyText
                    else if (monthly > 0) monthly.toString() else "",
            onChange = if (contributionUnit == ContributionUnit.DAY) onDaily else onMonthly,
            label = if (contributionUnit == ContributionUnit.DAY) "Сума на день" else "Сума на місяць",
            modifier = Modifier.fillMaxWidth(),
            showCurrency = true
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "На який термін?",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Green
        )

        UnitChoiceRow(
            firstLabel = "Роки",
            secondLabel = "Місяці",
            firstSelected = termUnit == TermUnit.YEAR,
            onFirst = { onTermUnitChange(TermUnit.YEAR) },
            onSecond = { onTermUnitChange(TermUnit.MONTH) }
        )

        MoneyField(
            value = if (termUnit == TermUnit.YEAR)
                yearsText
            else if (months > 0) months.toString() else "",
            onChange = if (termUnit == TermUnit.YEAR) onYears else onMonths,
            label = if (termUnit == TermUnit.YEAR) "Термін, років" else "Термін, місяців",
            modifier = Modifier.fillMaxWidth(),
            showCurrency = false
        )

        if (contributionUnit == ContributionUnit.MONTH || termUnit == TermUnit.MONTH) {
            Text(
                "Для місячного розрахунку: 1 місяць = 30 днів.",
                fontSize = 11.sp,
                color = Color(0xFF7A817D)
            )
        }

        // 3. Concrete projection
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE4C66A))
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Що буде за цей термін",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Green
                )

                if (!hasTarget) {
                    Text(
                        "Вкажи ціль і термін — я покажу конкретний результат.",
                        fontSize = 13.sp,
                        color = Color(0xFF64706B)
                    )
                } else if (!hasTerm) {
                    Text(
                        "Вкажи термін, щоб побачити прогноз.",
                        fontSize = 13.sp,
                        color = Color(0xFF64706B)
                    )
                } else {
                    val totalDays = effectiveDays
                    val contributionPerDay = effectiveDaily
                    val plannedTotal = (current + contributionPerDay * totalDays)
                        .toLong()
                        .coerceAtMost(target.toLong())
                    val plannedGain = (plannedTotal - current).coerceAtLeast(0L)
                    val percentAtEnd = (plannedTotal.toDouble() / safeTarget * 100.0).coerceIn(0.0, 100.0)
                    val periodLabel = when (termUnit) {
                        TermUnit.YEAR -> "$years років"
                        TermUnit.MONTH -> "$months місяців"
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                "Через $periodLabel",
                                fontSize = 13.sp,
                                color = Color(0xFF64706B)
                            )
                            Text(
                                "%,d ₴".format(plannedTotal),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Gold
                            )
                        }
                        Text(
                            "%.1f%% цілі".format(percentAtEnd),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Green
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0E4BE))

                    Text(
                        "Додатково накопичиш: %,d ₴".format(plannedGain),
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                    Text(
                        "До цілі залишиться: %,d ₴".format((target - plannedTotal).coerceAtLeast(0L)),
                        color = Color(0xFF64706B)
                    )

                    if (!hasContribution) {
                        Text(
                            "Задай суму, щоб прогноз став точним.",
                            fontSize = 12.sp,
                            color = Color(0xFF8A650F)
                        )
                    }
                }
            }
        }

        // 4. Million calculation
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Green),
            border = BorderStroke(1.5.dp, Gold)
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    "Шлях до 1 000 000 ₴",
                    color = GoldLight,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (millionRemaining == 0)
                        "МІЛЬЙОН ДОСЯГНУТО! 🎉"
                    else
                        "Залишилось %,d ₴".format(millionRemaining),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                if (hasTerm && hasContribution) {
                    val termLabel = when (termUnit) {
                        TermUnit.YEAR -> "$years років"
                        TermUnit.MONTH -> "$months місяців"
                    }
                    val contributionLabel = when (contributionUnit) {
                        ContributionUnit.DAY -> "%,d ₴ / день".format(daily)
                        ContributionUnit.MONTH -> "%,d ₴ / місяць".format(monthly)
                    }
                    Text(
                        "За планом $termLabel: $contributionLabel",
                        color = Color.White.copy(.88f)
                    )
                    Text(
                        "До мільйона потрібно ≈ %,d ₴ / день".format(millionDailyForYears.roundToInt()),
                        color = Color.White.copy(.88f)
                    )
                    Text(
                        "Або ≈ %,d ₴ / місяць".format(millionMonthlyForYears.roundToInt()),
                        color = Color.White.copy(.88f)
                    )
                } else {
                    Text(
                        "Вкажи суму та термін, щоб побачити план до мільйона.",
                        color = Color.White.copy(.88f)
                    )
                }

                millionDays?.let {
                    Text(
                        "За твоїм темпом: приблизно $it днів до мільйона",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. Progress moved to the bottom
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE4C66A))
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Твій прогрес",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Green
                        )
                        Text(
                            if (hasTarget)
                                "До цілі залишилось %,d ₴".format(remaining)
                            else
                                "Ціль ще не вказана",
                            color = Color(0xFF64706B),
                            fontSize = 13.sp
                        )
                    }

                    if (hasTarget) {
                        Text(
                            "%.1f%%".format(
                                (current.toDouble() / safeTarget * 100).coerceAtLeast(0.0)
                            ),
                            color = Gold,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = {
                        if (hasTarget)
                            (current.toDouble() / safeTarget).coerceIn(0.0, 1.0).toFloat()
                        else 0f
                    },
                    Modifier.fillMaxWidth().height(13.dp),
                    color = Gold,
                    trackColor = Color(0xFFF2E8C9)
                )

                LinearProgressIndicator(
                    progress = { segmentProgress },
                    Modifier.fillMaxWidth().height(8.dp),
                    color = Green,
                    trackColor = Color(0xFFEAF4EF)
                )
            }
        }


    }
}


@Composable
private fun HistoryTab(history: List<SavingEntry>) {
    val total = history.sumOf { it.amount }

    ScreenColumn {
        AppTitle()
        SectionTitle("📜 Історія", "Твій шлях у цифрах.")

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Green),
            border = BorderStroke(1.dp, Gold)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Усього внесків", color = Color.White.copy(.7f))
                    Text("${history.size}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("За всю історію", color = Color.White.copy(.7f))
                    Text("+%,d ₴".format(total), color = GoldLight, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        if (history.isEmpty()) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🌱", fontSize = 42.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Історія поки порожня", fontWeight = FontWeight.Bold, color = Green)
                    Text("Перший внесок стане першим записом.", color = Color(0xFF64706B))
                }
            }
        } else {
            history.forEach { entry ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF0E4BE))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(contributionLabel(entry.amount), fontWeight = FontWeight.Bold, color = Color(0xFF8A6A20))
                            Text(entry.date, color = Color(0xFF64706B), fontSize = 12.sp)
                        }
                        Text("+%,d ₴".format(entry.amount), fontWeight = FontWeight.ExtraBold, color = Gold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Green)
        Text(subtitle, fontSize = 13.sp, color = Color(0xFF64706B))
    }
}

@Composable
private fun HomeShortcut(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, Color(0xFFD6A72C)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF9A6A09)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$icon  $title", fontWeight = FontWeight.ExtraBold)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF8A7650))
        }
    }
}

@Composable
private fun QuickAmountButton(
    amount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, if (selected) Gold else Color(0xFFE0D2A5)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) GoldPale else Color.White,
            contentColor = Color(0xFF9A6A09)
        )
    ) {
        Text("%,d ₴".format(amount), fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun MoneyField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    showCurrency: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        suffix = if (showCurrency) ({ Text("₴") }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
