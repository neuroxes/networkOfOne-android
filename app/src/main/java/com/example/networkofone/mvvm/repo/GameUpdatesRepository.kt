package com.example.networkofone.mvvm.repo

import com.example.networkofone.mvvm.models.Notification
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GameUpdatesRepository {

    private val database =
        Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val ref = database.getReference("notifications")

    fun getNotificationsByGameId(gameId: String): Flow<List<Notification>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<Notification>()

                for (child in snapshot.children) {
                    try {
                        val notification = child.getValue(Notification::class.java)
                        notification?.let {
                            // Filter notifications by gameId and userId
                            if (it.gameId == gameId) {
                                notifications.add(it)
                            }
                        }
                    } catch (e: Exception) {
                        // Handle parsing error
                        e.printStackTrace()
                    }
                }

                // Sort notifications by createdAt timestamp (newest first)
                notifications.sortByDescending { it.createdAt }

                trySend(notifications)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }
}