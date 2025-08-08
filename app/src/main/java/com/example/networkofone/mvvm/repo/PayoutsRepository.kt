package com.example.networkofone.mvvm.repo

import android.util.Log
import com.example.networkofone.mvvm.models.PaymentRequestData
import com.example.networkofone.mvvm.models.PaymentStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PayoutsRepository {
    private val database =
        Firebase.database("https://networkofone-3b9c4-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val payoutsRef = database.getReference("paymentRequests")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "null"

    suspend fun getPayoutsBySchedulerId(): List<PaymentRequestData> = withContext(
        Dispatchers.IO
    ) {
        try {
            // Query payment requests by schedulerId
            val query = payoutsRef.orderByChild("schedularId").equalTo(userId)
            val snapshot = query.get().await()

            // Convert snapshot to list of PaymentRequestData objects
            val payouts = snapshot.children.mapNotNull { child ->
                child.getValue(PaymentRequestData::class.java)
            }

            if (payouts.isEmpty()) {
                Log.e("Payout Repo", "No payouts found for schedulerId: $userId")
                emptyList()
            } else {
                (payouts)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }


    fun acceptPayout(gameId: String, callback: (Boolean) -> Unit) {
        val updates = mapOf(
            "status" to PaymentStatus.APPROVED,
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
            "status" to PaymentStatus.REJECTED
        )

        payoutsRef.child(gameId).updateChildren(updates).addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}