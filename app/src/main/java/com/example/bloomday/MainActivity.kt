package com.example.bloomday

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure this layout file contains the FragmentContainerView NavHost and BottomNav
        setContentView(R.layout.activity_main)

        // --- SAFER: find the NavHostFragment from the FragmentManager ---
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment == null) {
            Log.e(TAG, "NavHostFragment not found via supportFragmentManager.findFragmentById(R.id.nav_host_fragment). " +
                    "Make sure activity_main.xml includes a FragmentContainerView with android:id=\"@+id/nav_host_fragment\" and\n" +
                    "android:name=\"androidx.navigation.fragment.NavHostFragment\" and that res/navigation/nav_graph.xml exists.")
            Toast.makeText(this, "Startup error: nav_host_fragment not found (see Logcat)", Toast.LENGTH_LONG).show()
            return
        }

        // now safely get navController
        navController = navHostFragment.navController

        // validate nav graph loaded
        val navGraph = try {
            navController!!.graph
        } catch (e: Exception) {
            Log.e(TAG, "navController.graph failed to load", e)
            Toast.makeText(this, "Startup error: nav_graph not loaded (see Logcat)", Toast.LENGTH_LONG).show()
            return
        }

        // top-level destinations
        val topLevelIds = setOf(
            R.id.homeFragment,
            R.id.habitsFragment,
            R.id.moodFragment,
            R.id.profileFragment,
            R.id.calendarFragment
        )

        // check missing destinations (optional but helpful)
        val missing = topLevelIds.filter { id -> navGraph.findNode(id) == null }
        if (missing.isNotEmpty()) {
            Log.e(TAG, "Missing destinations in nav_graph: $missing")
            Toast.makeText(this, "Startup warning: nav_graph missing destinations (see Logcat)", Toast.LENGTH_LONG).show()
            // continue anyway so you can visually debug
        }

        val appBarConfiguration = AppBarConfiguration(topLevelIds)

        // Toolbar
        val toolbar = findViewById<MaterialToolbar?>(R.id.topAppBar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            setupActionBarWithNavController(navController!!, appBarConfiguration)
        } else {
            Log.w(TAG, "Toolbar (R.id.topAppBar) not found in layout")
        }

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView?>(R.id.bottom_nav)
        if (bottomNav == null) {
            Log.e(TAG, "BottomNavigationView R.id.bottom_nav not found in layout")
            Toast.makeText(this, "Startup error: bottom_nav missing (see Logcat)", Toast.LENGTH_LONG).show()
            return
        } else {
            bottomNav.setupWithNavController(navController!!)
            bottomNav.setOnItemReselectedListener { /* no-op */ }
        }

        Log.i(TAG, "MainActivity started successfully")
    }

    override fun onSupportNavigateUp(): Boolean {
        val nc = navController ?: return super.onSupportNavigateUp()
        return nc.navigateUp() || super.onSupportNavigateUp()
    }
}
