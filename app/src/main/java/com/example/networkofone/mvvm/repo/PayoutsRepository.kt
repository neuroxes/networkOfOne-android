package com.example.networkofone.mvvm.repo

import android.util.Log
import com.example.networkofone.mvvm.models.GameStatus
import com.example.networkofone.mvvm.models.Notification
import com.example.networkofone.mvvm.models.NotificationData
import com.example.networkofone.mvvm.models.NotificationType
import com.example.networkofone.mvvm.models.NotificationTypeLocal
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
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val notificationRepo = NotificationRepositoryFirebase()
    private val notificationRepoLocal = NotificationRepository()

    suspend fun createPaymentRequest(paymentRequestData: PaymentRequestData): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val paymentRequestRef = database.getReference("paymentRequests")
                val id = paymentRequestRef.push().key
                    ?: return@withContext Result.failure(Exception("ID generation failed"))

                val requestWithId = paymentRequestData.copy(id = id)
                paymentRequestRef.child(id).setValue(requestWithId).await()

                // Update game status
                gameRef.child(paymentRequestData.gameId).updateChildren(
                    mapOf("status" to GameStatus.PAYMENT_REQUESTED)
                ).await()

                // Send notification to school
                sendPaymentRequestNotification(requestWithId)

                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

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

    suspend fun getPayoutById(payoutId: String): PaymentRequestData? = withContext(Dispatchers.IO) {
        try {
            val snapshot = payoutsRef.child(payoutId).get().await()
            snapshot.getValue(PaymentRequestData::class.java)?.also {
                Log.d("PayRepo", "Found payout with ID: $payoutId")
            } ?: run {
                Log.e("PayRepo", "No payout found with ID: $payoutId")
                null
            }
        } catch (e: Exception) {
            Log.e("PayRepo", "getPayoutById: ${e.message}")
            null
        }
    }

    suspend fun getPayoutBySchedulerId(schedulerId: String): PaymentRequestData? = withContext(Dispatchers.IO) {
        try {
            val query = payoutsRef.orderByChild("schedularId").equalTo(schedulerId).limitToFirst(1)
            val snapshot = query.get().await()

            snapshot.children.firstOrNull()?.getValue(PaymentRequestData::class.java)?.also {
                Log.d("PayRepo", "Found payout for schedulerId: $schedulerId")
            } ?: run {
                Log.e("PayRepo", "No payout found for schedulerId: $schedulerId")
                null
            }
        } catch (e: Exception) {
            Log.e("PayRepo", "getPayoutBySchedulerId: ${e.message}")
            null
        }
    }

    suspend fun getPayoutByRefereeId(refereeId: String): PaymentRequestData? = withContext(Dispatchers.IO) {
        try {
            val query = payoutsRef.orderByChild("refereeId").equalTo(refereeId).limitToFirst(1)
            val snapshot = query.get().await()

            snapshot.children.firstOrNull()?.getValue(PaymentRequestData::class.java)?.also {
                Log.d("PayRepo", "Found payout for refereeId: $refereeId")
            } ?: run {
                Log.e("PayRepo", "No payout found for refereeId: $refereeId")
                null
            }
        } catch (e: Exception) {
            Log.e("PayRepo", "getPayoutByRefereeId: ${e.message}")
            null
        }
    }
    suspend fun acceptPayout(payout: PaymentRequestData): Boolean {
        val updates = mapOf(
            "status" to PaymentStatus.APPROVED, "paidAt" to System.currentTimeMillis()
        )
        val updatesGame = mapOf(
            "status" to GameStatus.COMPLETED
        )

        return try {
            payoutsRef.child(payout.id).updateChildren(updates).await()
            gameRef.child(payout.gameId).updateChildren(updatesGame).await()

            // Get payment request data and send notification
            val paymentRequest = getPaymentRequestById(payout.id)
            paymentRequest?.let { request ->
                sendPaymentApprovedNotification(request)
            }

            notificationRepoLocal.createNotification(
                Notification(
                    userId = payout.schedularId,
                    userName = payout.schedularName,
                    gameId = payout.gameId,
                    gameName = payout.gameName,
                    refereeId = payout.refereeId,
                    refereeName = payout.refereeName,
                    title = "Payout Accepted",
                    message = "Payout for game \"${payout.gameName}\" involving referee \"${payout.refereeName}\" for the amount of $${payout.amount} has been accepted by scheduler \"${payout.schedularName}\".",
                    type = NotificationTypeLocal.ACCEPTED,
                )
            )

            Log.d("PayoutRepo", "Payout ${payout.id} approved successfully")
            true
        } catch (e: Exception) {
            Log.e("PayoutRepo", "Error approving payout ${payout.id}", e)
            false
        }
    }

    suspend fun rejectPayout(payout: PaymentRequestData): Boolean {
        val updates = mapOf(
            "status" to PaymentStatus.REJECTED
        )
        val updatesGame = mapOf(
            "status" to GameStatus.REJECTED
        )

        return try {
            payoutsRef.child(payout.id).updateChildren(updates).await()
            gameRef.child(payout.gameId).updateChildren(updatesGame).await()

            // Get payment request data and send notification
            val paymentRequest = getPaymentRequestById(payout.id)
            paymentRequest?.let { request ->
                sendPaymentRejectedNotification(request)
            }

            notificationRepoLocal.createNotification(
                Notification(
                    userId = payout.schedularId,
                    userName = payout.schedularName,
                    gameId = payout.gameId,
                    gameName = payout.gameName,
                    refereeId = payout.refereeId,
                    refereeName = payout.refereeName,
                    title = "Payout Rejected",
                    message = "Unfortunately, Payout for game \"${payout.gameName}\" involving referee \"${payout.refereeName}\" for the amount of $${payout.amount} has been rejected by scheduler \"${payout.schedularName}\".",
                    type = NotificationTypeLocal.REJECTED,
                )
            )

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Notification methods
    private suspend fun sendPaymentRequestNotification(paymentRequest: PaymentRequestData) {
        val notification = NotificationData(
            title = "Payment Request",
            body = "${paymentRequest.refereeName} has requested payment of $${paymentRequest.amount} for ${paymentRequest.gameName}",
            type = NotificationType.PAYMENT_REQUESTED,
            targetUserId = paymentRequest.schedularId,
            paymentRequestId = paymentRequest.id,
            gameId = paymentRequest.gameId
        )
        notificationRepo.sendNotification(notification)
    }

    private suspend fun sendPaymentApprovedNotification(paymentRequest: PaymentRequestData) {
        val notification = NotificationData(
            title = "Payment Approved",
            body = "Your payment request of $${paymentRequest.amount} for ${paymentRequest.gameName} has been approved",
            type = NotificationType.PAYMENT_APPROVED,
            targetUserId = paymentRequest.refereeId,
            paymentRequestId = paymentRequest.id,
            gameId = paymentRequest.gameId
        )
        notificationRepo.sendNotification(notification)
    }

    private suspend fun sendPaymentRejectedNotification(paymentRequest: PaymentRequestData) {
        val notification = NotificationData(
            title = "Payment Rejected",
            body = "Your payment request of $${paymentRequest.amount} for ${paymentRequest.gameName} has been rejected",
            type = NotificationType.PAYMENT_REJECTED,
            targetUserId = paymentRequest.refereeId,
            paymentRequestId = paymentRequest.id,
            gameId = paymentRequest.gameId
        )
        notificationRepo.sendNotification(notification)
    }

    private suspend fun getPaymentRequestById(id: String): PaymentRequestData? {
        return try {
            val snapshot = payoutsRef.child(id).get().await()
            snapshot.getValue(PaymentRequestData::class.java)
        } catch (e: Exception) {
            Log.e("PayoutRepo", "Error fetching payment request $id", e)
            null
        }
    }
}

/*
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
}*/
