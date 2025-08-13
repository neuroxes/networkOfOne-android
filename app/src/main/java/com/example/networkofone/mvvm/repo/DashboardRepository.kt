package com.example.networkofone.mvvm.repo

import android.util.Log
import com.example.networkofone.mvvm.models.GameData
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class DashboardRepository(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth,
    private val databaseUrl: String,
) {

    private val gamesRef get() = database.getReference("games")
    private val paymentsRef get() = database.getReference("paymentRequests")
    private val usersRef get() = database.getReference("users")

    fun observeCurrentUser(): Flow<UserModel?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val ref = usersRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserModel::class.java)
                trySend(user)
            }

            override fun onCancelled(error: DatabaseError) {
                // Still send null; ViewModel can decide how to handle
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeGames(): Flow<List<GameData>> = callbackFlow {
        val ref = gamesRef
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<GameData>()
                for (child in snapshot.children) {
                    child.getValue(GameData::class.java)?.let { list.add(it) }
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                // On cancel, emit empty list to indicate "no data"
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observePaymentRequests(): Flow<List<PaymentRequestData>> = callbackFlow {
        val ref = paymentsRef
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<PaymentRequestData>()
                for (child in snapshot.children) {
                    child.getValue(PaymentRequestData::class.java)?.let {
                        list.add(it)
                        Log.e("TAG", "onDataChange Dashboard Rep: ${it.amount}")
                    }
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}