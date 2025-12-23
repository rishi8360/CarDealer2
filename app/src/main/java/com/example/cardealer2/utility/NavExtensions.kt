package com.example.cardealer2.utility

import androidx.navigation.NavController

/**
 * Pops back one step by default. If the next destination back is `home` and
 * [makeHomeInclusive] is true, clears up to and including `home` and recreates it.
 * This ensures a fresh Home screen and avoids stale instances.
 */
fun NavController.smartPopBack(makeHomeInclusive: Boolean = true): Boolean {
    val previousRoute = this.previousBackStackEntry?.destination?.route
    if (makeHomeInclusive && previousRoute == "home") {
        // Remove existing home and navigate to a fresh instance
        this.popBackStack("home", true)
        this.navigate("home") {
            launchSingleTop = true
            restoreState = true
        }
        return true
    }
    return this.popBackStack()
}














