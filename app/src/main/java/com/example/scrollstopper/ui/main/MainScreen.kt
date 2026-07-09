package com.example.scrollstopper.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey

enum class ScreenTab {
    TODAY,
    CALENDAR,
    ALERTS,
    ROADMAP,
    BOXING
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(ScreenTab.TODAY) }

    // Listen to lifecycle events to refresh accessibility status on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F0C1B),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.TODAY,
                    onClick = { selectedTab = ScreenTab.TODAY },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Today") },
                    label = { Text("Today", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8B5CF6),
                        selectedTextColor = Color(0xFF8B5CF6),
                        unselectedIconColor = Color.White.copy(alpha = 0.4f),
                        unselectedTextColor = Color.White.copy(alpha = 0.4f),
                        indicatorColor = Color.White.copy(alpha = 0.05f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.CALENDAR,
                    onClick = { selectedTab = ScreenTab.CALENDAR },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                    label = { Text("Calendar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8B5CF6),
                        selectedTextColor = Color(0xFF8B5CF6),
                        unselectedIconColor = Color.White.copy(alpha = 0.4f),
                        unselectedTextColor = Color.White.copy(alpha = 0.4f),
                        indicatorColor = Color.White.copy(alpha = 0.05f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.ALERTS,
                    onClick = { selectedTab = ScreenTab.ALERTS },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("Alerts", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8B5CF6),
                        selectedTextColor = Color(0xFF8B5CF6),
                        unselectedIconColor = Color.White.copy(alpha = 0.4f),
                        unselectedTextColor = Color.White.copy(alpha = 0.4f),
                        indicatorColor = Color.White.copy(alpha = 0.05f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.ROADMAP,
                    onClick = { selectedTab = ScreenTab.ROADMAP },
                    icon = { Icon(Icons.Default.List, contentDescription = "Roadmap") },
                    label = { Text("Roadmap", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8B5CF6),
                        selectedTextColor = Color(0xFF8B5CF6),
                        unselectedIconColor = Color.White.copy(alpha = 0.4f),
                        unselectedTextColor = Color.White.copy(alpha = 0.4f),
                        indicatorColor = Color.White.copy(alpha = 0.05f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.BOXING,
                    onClick = { selectedTab = ScreenTab.BOXING },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Boxing") },
                    label = { Text("Boxing", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF8B5CF6),
                        selectedTextColor = Color(0xFF8B5CF6),
                        unselectedIconColor = Color.White.copy(alpha = 0.4f),
                        unselectedTextColor = Color.White.copy(alpha = 0.4f),
                        indicatorColor = Color.White.copy(alpha = 0.05f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0C1B)) // Deep dark slate blue base background
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                ScreenTab.TODAY -> {
                    TodayScreen(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ScreenTab.CALENDAR -> {
                    CalendarScreen(
                        state = state,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ScreenTab.ALERTS -> {
                    AlertsScreen(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ScreenTab.ROADMAP -> {
                    RoadmapScreen(
                        state = state,
                        viewModel = viewModel,
                        onNavigateToToday = { selectedTab = ScreenTab.TODAY },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ScreenTab.BOXING -> {
                    BoxingScreen(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
