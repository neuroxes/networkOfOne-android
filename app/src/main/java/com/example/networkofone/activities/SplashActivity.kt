package com.example.networkofone.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.example.networkofone.SchedulerMainActivity
import com.example.networkofone.R
import com.example.networkofone.mvvm.models.UserType
import com.example.networkofone.utils.ActivityNavigatorUtil
import com.example.networkofone.utils.SharedPrefManager
import com.google.android.material.imageview.ShapeableImageView

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val img by lazy { findViewById<ShapeableImageView>(R.id.img) }
    private val tvAppName by lazy { findViewById<TextView>(R.id.tvAppName) }
    private var countDownTimer: CountDownTimer? = null
    private var timerStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Restore timer state
        timerStarted = savedInstanceState?.getBoolean("TIMER_STARTED", false) == true

        img.animate().alpha(1f).translationY(0F).setDuration(1500)
            .setInterpolator(FastOutSlowInInterpolator()).start()
        tvAppName.animate().alpha(1f).translationY(-10F).setDuration(1500)
            .setInterpolator(FastOutSlowInInterpolator()).setStartDelay(200).start()

        if (!timerStarted) {
            startCountDown()
            timerStarted = true
        }
    }

    private fun startCountDown() {
        countDownTimer = object : CountDownTimer(3000, 3000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                val status = userOnBoardingStatus()
                when (status) {
                    1 -> {
                        val userType = getUserType()
                        userType?.let {
                            when (it) {
                                UserType.SCHOOL -> {
                                    ActivityNavigatorUtil.startActivity(
                                        this@SplashActivity,
                                        SchedulerMainActivity::class.java
                                    )
                                }

                                UserType.REFEREE -> {
                                    ActivityNavigatorUtil.startActivity(
                                        this@SplashActivity,
                                        RefereeMainActivity::class.java
                                    )
                                }

                                UserType.ADMIN -> {
                                    ActivityNavigatorUtil.startActivity(
                                        this@SplashActivity,
                                        AdminMainActivity::class.java
                                    )
                                }

                                UserType.UNKNOWN -> {}
                            }
                        }
                    }

                    else -> ActivityNavigatorUtil.startActivity(
                        this@SplashActivity, AuthenticationActivity::class.java
                    )
                }
                finish()
            }
        }.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("TIMER_STARTED", timerStarted)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun userOnBoardingStatus(): Int {
        return getSharedPreferences("Logged", MODE_PRIVATE).getInt("isLogged", 0)
    }

    private fun getUserType(): UserType? {
        return SharedPrefManager(this).getUser()?.userType
    }
}