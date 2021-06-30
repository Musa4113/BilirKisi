package com.bilirkisi.proje.services


import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.bilirkisi.proje.util.AuthUtil
import com.bilirkisi.proje.util.FirestoreUtil

class MyFirebaseMessagingService : FirebaseMessagingService() {
    val TAG = "gghh"

    companion object {
        //FCM cihazları tanımlamak için token'ları kullanır
        fun getInstanceId(): Unit {
            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        println("MyFirebaseMessagingService.getInstanceId:${task.exception}")
                        return@OnCompleteListener
                    }

                    // Yeni Örnek al : ID token
                    val token = task.result?.token
                    println("MyFirebaseMessagingService.s:${token}")
                    if (token != null) {
                        addTokenToUserDocument(token)
                    }

                })

        }

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        println("MyFirebaseMessagingService.onNewToken:${token}")
        addTokenToUserDocument(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        //  mesaj gelmiyor mu?? Bunun neden olabileceğini görün: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Mesajın bir veri yükü içerip içermediğini kontrol edin.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            // Handle message within 10 seconds
            //      handleNow()

        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

}

fun addTokenToUserDocument(token: String) {
    val loggedUserID = AuthUtil.firebaseAuthInstance.currentUser?.uid
    if (loggedUserID != null) {
        FirestoreUtil.firestoreInstance.collection("users").document(loggedUserID)
            .update("token", token)
    }

}