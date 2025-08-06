package com.example.networkofone.home

import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.example.networkofone.MainActivity
import com.example.networkofone.R
import com.example.networkofone.databinding.FragmentHomeBinding
import com.incity.incity_stores.AppFragmentLoader


class HomeFragment(private val context: AppCompatActivity) :
    AppFragmentLoader(R.layout.fragment_store_info_root) {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var base: NestedScrollView

    override fun onCreate() {


    }

}