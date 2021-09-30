package com.app.amigo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.app.amigo.databinding.ActivityMainBinding
import com.app.amigo.fragment.HomeFragment
import com.app.amigo.fragment.SettingFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private var TAG = "@!@MainActivity"
    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "HomeClient: ${BuildConfig.VERSION_NAME}"
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "start")

        object : FragmentStateAdapter(this) {
            private val homeFragment = HomeFragment()

            private val settingFragment = SettingFragment()
//            private val mqttFragment = HomeFragment()

            override fun getItemCount() = 2
//            override fun getItemCount() = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> homeFragment
                    1 -> settingFragment
//                    2 -> mqttFragment
                    else -> throw NotImplementedError()
                }
            }
        }.also {
            binding.pager.adapter = it
        }


        TabLayoutMediator(binding.tabBar, binding.pager) { tab, position ->
            tab.text = when (position) {
                0 -> "HOME"
                1 -> "SETTING"
//                2 -> "MQTT"
                else -> throw NotImplementedError()
            }
        }.attach()
    }
}