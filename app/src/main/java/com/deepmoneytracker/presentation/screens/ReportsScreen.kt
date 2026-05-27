package com.deepmoneytracker.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import java.util.Calendar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepmoneytracker.presentation.theme.AppStrings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepmoneytracker.data.local.entity.TransactionType
import com.deepmoneytracker.presentation.theme.ExpenseRed
import com.deepmoneytracker.presentation.theme.IncomeGreen
import com.deepmoneytracker.presentation.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val last7DaysExpenses = remember(state.recentTransactions) {
        val expenses = state.recentTransactions.filter { it.type == TransactionType.EXPENSE }
        val days = (0..6).map { i ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal to 0.0
        }.reversed().toMutableList()

        expenses.forEach { tx ->
            val txCal = Calendar.getInstance().apply {
                timeInMillis = tx.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayIndex = days.indexOfFirst { it.first.timeInMillis == txCal.timeInMillis }
            if (dayIndex != -1) {
                days[dayIndex] = days[dayIndex].first to (days[dayIndex].second + tx.amount)
            }
        }
        days.map { (cal, total) ->
            val dayLabel = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Sun"
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                else -> ""
            }
            dayLabel to total
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(AppStrings.reports_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(AppStrings.label_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "This Month",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(AppStrings.label_income), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "₹${"%,.2f".format(state.totalIncome)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(AppStrings.label_expense), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "₹${"%,.2f".format(state.totalExpense)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            // Weekly Spending Trend (Graph)
            item {
                Text(
                    stringResource(AppStrings.reports_weekly_trend),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BarChart(
                            data = last7DaysExpenses,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }

            // Pie Chart
            item {
                Text(
                    stringResource(AppStrings.dashboard_expense_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                if (state.categoryTotals.isNotEmpty()) {
                    PieChart(
                        data = state.categoryTotals.map {
                            PieChartData(
                                label = it.category?.name ?: "Unknown",
                                value = it.total.toFloat(),
                                color = parseColor(it.category?.color ?: "#9E9E9E")
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                } else {
                    Text(
                        stringResource(AppStrings.reports_no_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Category list
            items(state.categoryTotals) { ct ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(
                                    color = parseColor(ct.category?.color ?: "#9E9E9E")
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    ct.category?.name ?: "Unknown",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${"%.1f".format(ct.percentage)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "₹${"%,.2f".format(ct.total)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private data class PieChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
private fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.value.toDouble() }.toFloat()

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = minOf(canvasWidth, canvasHeight) / 2 * 0.8f
        val center = Offset(canvasWidth / 2, canvasHeight / 2)

        var startAngle = -90f

        for (item in data) {
            val sweepAngle = (item.value / total) * 360f

            drawArc(
                color = item.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            startAngle += sweepAngle
        }

        // Draw center circle for donut effect
        drawCircle(
            color = Color.White,
            radius = radius * 0.5f,
            center = center
        )
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}

@Composable
private fun BarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(data) { (data.maxOfOrNull { it.second } ?: 0.0).toFloat().coerceAtLeast(100f) }
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val valueColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 40.dp.toPx()
        val paddingBottom = 24.dp.toPx()
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom

        // Draw Axes
        drawLine(
            color = Color.LightGray,
            start = Offset(paddingLeft, 0f),
            end = Offset(paddingLeft, chartHeight),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.LightGray,
            start = Offset(paddingLeft, chartHeight),
            end = Offset(width, chartHeight),
            strokeWidth = 1.dp.toPx()
        )

        if (data.isNotEmpty()) {
            val barWidth = (chartWidth / data.size) * 0.6f
            val spacing = (chartWidth / data.size) * 0.4f

            data.forEachIndexed { index, (label, value) ->
                val x = paddingLeft + (index * (barWidth + spacing)) + (spacing / 2)
                val barHeight = if (maxVal > 0) (value.toFloat() / maxVal) * (chartHeight * 0.8f) else 0f
                val y = chartHeight - barHeight

                // Draw Bar
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

                // Draw Labels and Values inside drawContext
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = labelColor
                        textSize = 10.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(label, x + barWidth / 2, chartHeight + 16.dp.toPx(), paint)
                    
                    if (value > 0) {
                        paint.textSize = 8.dp.toPx()
                        paint.color = valueColor
                        drawText("₹${value.toInt()}", x + barWidth / 2, y - 6.dp.toPx(), paint)
                    }
                }
            }
        }
    }
}
