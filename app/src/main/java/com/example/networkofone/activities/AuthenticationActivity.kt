package com.example.networkofone.activities

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.example.networkofone.adapters.AuthenticationPagerAdapter
import com.example.networkofone.databinding.ActivityAuthenticationBinding
import com.example.networkofone.mvvm.viewModels.AuthenticationActivityViewModel
import kotlin.jvm.java

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthenticationBinding
    private lateinit var adapter: AuthenticationPagerAdapter
    private lateinit var viewModel: AuthenticationActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthenticationActivityViewModel::class.java]
        adapter = AuthenticationPagerAdapter(supportFragmentManager)

        Handler().postDelayed({
            setupViewPager()
        }, 100)
        observeViewModel()
        binding.btnAuth.setOnClickListener { switchTab() }
    }

    private fun setupViewPager() {
        binding.viewpager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewpager)
        binding.viewpager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                viewModel.setCurrentTab(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun observeViewModel() {
        viewModel.getAuthButtonText().observe(this) { text ->
            binding.btnAuth.text = text
        }
        viewModel.getAuthMessage().observe(this) { text -> binding.t3.text = text }
        viewModel.getMessage1().observe(this) { text -> binding.t1.text = text }
        viewModel.getMessage2().observe(this) { text -> binding.t2.text = text }
    }

    private fun switchTab() {
        val newPosition = if (viewModel.getCurrentTab().getValue() === 1) 0 else 1
        binding.viewpager.currentItem = newPosition
        viewModel.setCurrentTab(newPosition)
    }

}