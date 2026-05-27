package com.deepmoneytracker.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepmoneytracker.presentation.theme.LocalThemeColors
import com.deepmoneytracker.presentation.theme.ThemeColors

@Composable
fun <T> DateAccordionList(
    items: List<T>,
    getDate: (T) -> Long,
    summaryRight: (List<T>) -> String,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    themeColors: ThemeColors = LocalThemeColors.current,
    autoExpandFirst: Boolean = true,
    footerContent: (@Composable () -> Unit)? = null
) {
    val groupedByDate = remember(items, getDate) { items.groupByDateHeader(getDate) }
    var expandedDates by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(groupedByDate.keys) {
        if (autoExpandFirst && groupedByDate.isNotEmpty() && expandedDates.isEmpty()) {
            expandedDates = setOf(groupedByDate.keys.first())
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        groupedByDate.forEach { (dateHeader, dateItems) ->
            val isExpanded = dateHeader in expandedDates

            item(key = "header_$dateHeader") {
                DateAccordionHeader(
                    dateHeader = dateHeader,
                    summary = summaryRight(dateItems),
                    isExpanded = isExpanded,
                    themeColors = themeColors,
                    onClick = {
                        expandedDates = if (isExpanded) {
                            expandedDates - dateHeader
                        } else {
                            expandedDates + dateHeader
                        }
                    }
                )
            }

            if (isExpanded) {
                items(dateItems, key = { itemKey(it) }) { item ->
                    itemContent(item)
                }
            }
        }

        if (footerContent != null) {
            item { footerContent() }
        }
    }
}

@Composable
fun DateAccordionHeader(
    dateHeader: String,
    summary: String,
    isExpanded: Boolean,
    themeColors: ThemeColors,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.cardBackground
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = themeColors.primary
                )
                Text(
                    dateHeader,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (themeColors.isDark) Color.White else themeColors.onBackground
                )
            }
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (themeColors.isDark) Color.White.copy(alpha = 0.7f) else themeColors.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}
