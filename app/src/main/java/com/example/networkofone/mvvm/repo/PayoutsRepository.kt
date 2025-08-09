package com.example.networkofone.mvvm.repo

import android.util.Log
import com.example.networkofone.mvvm.models.GameStatus
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
    private val gameRef = database.getReference("games")
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
            Log.e("PayRepo", "getPayoutsBySchedulerId: ${e.message}")
            emptyList()
        }
    }
    suspend fun getPayoutsByRefereeId(): List<PaymentRequestData> = withContext(
        Dispatchers.IO
    ) {
        try {
            // Query payment requests by schedulerId
            val query = payoutsRef.orderByChild("refereeId").equalTo(userId)
            val snapshot = query.get().await()

            // Convert snapshot to list of PaymentRequestData objects
            val payouts = snapshot.children.mapNotNull { child ->
                child.getValue(PaymentRequestData::class.java)
            }

            if (payouts.isEmpty()) {
                Log.e("Payout Repo", "No payouts found for refereeId: $userId")
                emptyList()
            } else {
                (payouts)
            }
        } catch (e: Exception) {
            Log.e("PayRepo", "getPayoutsByRefereeId: ${e.message}")
            emptyList()
        }
    }


    suspend fun acceptPayout(payoutId: String, gameId: String): Boolean {
        val updates = mapOf(
            "status" to PaymentStatus.APPROVED,
            "paidAt" to System.currentTimeMillis()
        )
        val updatesGame = mapOf(
            "status" to GameStatus.COMPLETED
        )

        return try {
            payoutsRef.child(payoutId).updateChildren(updates).await()
            gameRef.child(gameId).updateChildren(updatesGame).await()
            Log.d("PayoutRepo", "Payout $payoutId approved successfully")
            true
        } catch (e: Exception) {
            Log.e("PayoutRepo", "Error approving payout $payoutId", e)
            false
        }
    }

    suspend fun rejectPayout(payoutId: String,gameId: String): Boolean {
        val updates = mapOf(
            "status" to PaymentStatus.REJECTED
        )
        val updatesGame = mapOf(
            "status" to GameStatus.REJECTED
        )
        return try {
            payoutsRef.child(payoutId).updateChildren(updates).await()
            gameRef.child(gameId).updateChildren(updatesGame).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}