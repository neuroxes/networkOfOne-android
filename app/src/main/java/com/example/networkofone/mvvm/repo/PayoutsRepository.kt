package com.example.networkofone.mvvm.repo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.GameStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class PayoutsRepository {
    private val database =
        Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val payoutsRef = database.getReference("games") // Assuming games is the node name

    fun getPayouts(): LiveData<List<GameData>> {
        val liveData = MutableLiveData<List<GameData>>()

        payoutsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val payoutsList = mutableListOf<GameData>()
                for (child in snapshot.children) {
                    val gameData = child.getValue(GameData::class.java)
                    gameData?.let {
                        it.id = child.key ?: ""
                        payoutsList.add(it)
                    }
                }
                liveData.value = payoutsList
            }

            override fun onCancelled(error: DatabaseError) {
                liveData.value = emptyList()
            }
        })

        return liveData
    }

    fun acceptPayout(gameId: String, callback: (Boolean) -> Unit) {
        val updates = mapOf(
            "status" to GameStatus.ACCEPTED.name,
            "acceptedAt" to System.currentTimeMillis(),
            "acceptedBy" to FirebaseAuth.getInstance().currentUser?.uid
        )

        payoutsRef.child(gameId).updateChildren(updates).addOnSuccessListener {
            callback(true)
            Log.e("Payout Repo", "acceptPayout: true")
        }.addOnFailureListener {
                callback(false)
                Log.e("Payout Repo", "acceptPayout: false")
            }
    }

    fun rejectPayout(gameId: String, callback: (Boolean) -> Unit) {
        val updates = mapOf(
            "status" to GameStatus.REJECTED.name
        )

        payoutsRef.child(gameId).updateChildren(updates).addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun searchPayouts(query: String): LiveData<List<GameData>> {
        val liveData = MutableLiveData<List<GameData>>()

        payoutsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val payoutsList = mutableListOf<GameData>()
                for (child in snapshot.children) {
                    val gameData = child.getValue(GameData::class.java)
                    gameData?.let {
                        it.id = child.key ?: ""
                        // Filter by title or location containing the query
                        if (it.title.contains(query, ignoreCase = true) || it.location.contains(
                                query, ignoreCase = true
                            )
                        ) {
                            payoutsList.add(it)
                        }
                    }
                }
                liveData.value = payoutsList
            }

            override fun onCancelled(error: DatabaseError) {
                liveData.value = emptyList()
            }
        })

        return liveData
    }
}