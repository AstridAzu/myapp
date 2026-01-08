package com.example.myapp.ui.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapp.ui.admin.AdminManageClassScreen
import com.example.myapp.ui.login.EnrollClassScreen
import com.example.myapp.ui.login.LoginScreen
import com.example.myapp.ui.login.RegisterScreen

@Composable
fun AppNavigation(initialUserId: Int) {
    val navController = rememberNavController()
    val startDestination = if (initialUserId != -1) "main/$initialUserId" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable(
            "main/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: -1
            MainScreen(navController, userId)
        }
        composable(
            "admin_manage_class/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.IntType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: -1
            AdminManageClassScreen(navController) 
        }
        composable(
            "my_classes/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.IntType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: -1
            MyClassesScreen(navController, userId)
        }
        composable(
            "enroll_class/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: -1
            EnrollClassScreen(navController, userId)
        }
        composable(
            "class_detail/{classId}",
            arguments = listOf(navArgument("classId") { type = NavType.IntType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getInt("classId") ?: -1
            ClassDetailScreen(navController, classId)
        }
    }
}