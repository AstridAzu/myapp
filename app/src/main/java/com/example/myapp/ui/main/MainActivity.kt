package com.example.myapp.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import com.example.myapp.data.sync.SyncScheduler
import com.example.myapp.ui.navigation.NavGraph
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.theme.MyAppTheme
import com.example.myapp.utils.SessionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Log.d("DEBUG_TEST", "MainActivity.onCreate ejecutado") // Eliminado
        enableEdgeToEdge()
        SyncRuntimeDispatcher.init(this)
        SyncScheduler.enqueuePeriodicSync(this)
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            SyncScheduler.enqueueOneTimeSync(this)
        }
        setContent {
            MyAppTheme {
                val navController = rememberNavController()
                val startDestination = if (sessionManager.isLoggedIn()) {
                    Routes.Main.route
                } else {
                    Routes.Login.route
                }
                NavGraph(navController = navController, startDestination = startDestination)
            }
        }
    }
}
