package com.example.networkofone.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.networkofone.mvvm.models.UserModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class SharedPrefManager(context: Context) {
    private val sharedPref: SharedPreferences =
        context.getSharedPreferences(Constants.PREFERENCE, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPref.edit()
    private val gson = Gson()

    fun saveId(id: String) {
        editor.putString("userId", id).apply()
    }

    fun getId(): String? =
        sharedPref.getString("userId", null)

    fun saveDocId(id: String) {
        editor.putString("docId", id).apply()
    }

    fun saveUser(user: UserModel) {
        editor.putString("user", Gson().toJson(user))
        editor.apply()
    }

    fun getUser(): UserModel? {
        val json = sharedPref.getString("user", null)
        return if (json.isNullOrEmpty()) {
            null
        } else {
            try {
                Gson().fromJson(json, UserModel::class.java)
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                null
            }
        }
    }

    fun getDocId(): String? =
        sharedPref.getString("docId", null)

    fun saveLogin() {
        editor.putString(Constants.LOGIN, "login").apply()
    }

    fun checkLogin(): Boolean =
        sharedPref.getString(Constants.LOGIN, null) == "login"

    fun saveUserName(name: String) {
        editor.putString("userName", name).apply()
    }

    fun getUserId(): String? =
        sharedPref.getString("userId", "")

    fun saveUserEmail(email: String) {
        editor.putString("userEmail", email).apply()
    }

    fun getUserEmail(): String? =
        sharedPref.getString("userEmail", "")

    // ─── NEW for password flow ───
    fun saveUserPassword(password: String) {
        editor.putString("userPassword", password).apply()
    }

    fun getStoredPassword(): String? =
        sharedPref.getString("userPassword", null)

    // ─── CLEAR ALL on logout ───
    fun clearUserData() {
        editor.clear().apply()
    }

    fun clearAllPreferences() {
        editor.clear().apply()
    }
}