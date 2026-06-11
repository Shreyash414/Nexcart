package com.example.nexcart

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import android.view.View
import com.example.nexcart.databinding.ActivityMainBinding
import com.example.nexcart.ui.auth.AuthEvent
import com.example.nexcart.ui.auth.AuthViewModel
import com.example.nexcart.ui.homeScreens.HomeViewModel
import com.example.nexcart.ui.seller.SellerViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.RangeSlider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val sellerViewModel: SellerViewModel by viewModels()
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val currentId = navController.currentDestination?.id
            val isAuth = currentId == R.id.loginFragment || currentId == R.id.registerFragment
            
            if (isAuth) {
                // Auth screens: No top padding, let content go edge-to-edge
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                binding.appBar.setPadding(0, 0, 0, 0)
            } else {
                // Main screens: Side/bottom padding on container, top padding on AppBar
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                binding.appBar.setPadding(0, systemBars.top, 0, 0)
            }
            insets
        }

        setupNavigationVisibility()
        setupToolbar()
        setupDrawerContent()
        setupSearchBar()
        observeCategories()
        observeAuthEvents()
    }

    private fun setupNavigationVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isAuthDestination = destination.id == R.id.loginFragment || destination.id == R.id.registerFragment
            
            // Force AppBar to collapse and hide on Auth screens
            if (isAuthDestination) {
                binding.appBar.setExpanded(false, false)
                binding.appBar.isVisible = false
            } else {
                binding.appBar.isVisible = true
            }
            
            // Request inset re-application for the new destination
            ViewCompat.requestApplyInsets(binding.root)
            
            // Also hide Search View if it was open
            if (isAuthDestination) {
                binding.searchView.hide()
                binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
            }

            // Reset search/filter states when navigation destination changes
            viewModel.resetFilters()
            sellerViewModel.resetFilters()
            binding.searchBar.setText("")
            binding.searchView.editText.setText("")
            
            val priceSlider = binding.navView.findViewById<RangeSlider>(R.id.price_range_slider)
            priceSlider?.setValues(0f, 1000f)

            // Update ChipGroup based on destination
            val chipGroup = binding.navView.findViewById<ChipGroup>(R.id.chip_group_category)
            if (destination.id == R.id.homeFragment) {
                updateCategoryChips(chipGroup, viewModel.categories.value)
            } else if (destination.id == R.id.sellerFragment) {
                updateCategoryChips(chipGroup, sellerViewModel.categories.value)
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupSearchBar() {
        binding.searchView.editText.setOnEditorActionListener { textView, _, _ ->
            val query = textView.text.toString()
            binding.searchBar.setText(query)
            
            when (navController.currentDestination?.id) {
                R.id.homeFragment -> viewModel.searchProducts(query)
                R.id.sellerFragment -> sellerViewModel.searchProducts(query)
            }
            
            binding.searchView.hide()
            false
        }
    }

    private fun setupDrawerContent() {
        val navView = binding.navView

        // Theme Switch
        val themeSwitch = navView.findViewById<MaterialSwitch>(R.id.switch_theme)
        themeSwitch?.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        themeSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Apply Filters Button
        navView.findViewById<Button>(R.id.btn_apply_filters)?.setOnClickListener {
            val chipGroup = navView.findViewById<ChipGroup>(R.id.chip_group_category)
            val selectedCategories = mutableListOf<String>()
            chipGroup?.checkedChipIds?.forEach { id ->
                val chip = navView.findViewById<Chip>(id)
                chip?.text?.let { selectedCategories.add(it.toString()) }
            }

            val priceSlider = navView.findViewById<RangeSlider>(R.id.price_range_slider)
            val values = priceSlider?.values ?: listOf(0f, 1000f)

            when (navController.currentDestination?.id) {
                R.id.homeFragment -> viewModel.applyFilters(selectedCategories, values[0], values[1])
                R.id.sellerFragment -> sellerViewModel.applyFilters(selectedCategories, values[0], values[1])
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Logout Button
        navView.findViewById<Button>(R.id.btn_logout)?.setOnClickListener {
            authViewModel.signOut()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun observeCategories() {
        val chipGroup = binding.navView.findViewById<ChipGroup>(R.id.chip_group_category)
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    if (navController.currentDestination?.id == R.id.homeFragment) {
                        updateCategoryChips(chipGroup, categories)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sellerViewModel.categories.collect { categories ->
                    if (navController.currentDestination?.id == R.id.sellerFragment) {
                        updateCategoryChips(chipGroup, categories)
                    }
                }
            }
        }
    }

    private fun updateCategoryChips(chipGroup: ChipGroup?, categories: List<String>) {
        chipGroup?.removeAllViews()
        categories.forEach { category ->
            val chip = Chip(this@MainActivity).apply {
                id = View.generateViewId() // Fix: dynamically assign a unique ID so checkedChipIds works!
                text = category
                isCheckable = true
                isCloseIconVisible = false
                setOnCheckedChangeListener { buttonView, isChecked ->
                    (buttonView as? Chip)?.isCloseIconVisible = isChecked
                }
                setOnCloseIconClickListener {
                    isChecked = false
                }
            }
            chipGroup?.addView(chip)
        }
    }

    private fun observeAuthEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authEvent.collect { event ->
                    when (event) {
                        is AuthEvent.NavigateToLogin -> {
                            val navOptions = androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true)
                                .build()
                            navController.navigate(R.id.loginFragment, null, navOptions)
                        }
                        is AuthEvent.NavigateToMain -> {
                            // Only navigate if we're on an auth screen
                            val currentId = navController.currentDestination?.id
                            if (currentId == R.id.loginFragment || currentId == R.id.registerFragment) {
                                if (event.isSeller) {
                                    navController.navigate(R.id.sellerFragment)
                                } else {
                                    navController.navigate(R.id.homeFragment)
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else if (binding.searchView.isShowing) {
            binding.searchView.hide()
        } else {
            super.onBackPressed()
        }
    }
}