package com.bilirkisi.proje.fragments.incoming_requests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bilirkisi.proje.fragments.different_user_profile.RECEIVED_REQUEST_ARRAY
import com.bilirkisi.proje.fragments.different_user_profile.SENT_REQUEST_ARRAY
import com.google.firebase.firestore.FieldValue
import com.bilirkisi.proje.models.User
import com.bilirkisi.proje.util.FirestoreUtil

const val FRIENDS = "friends"


class IncomingRequestsViewModel : ViewModel() {

    private val usersRef = FirestoreUtil.firestoreInstance.collection("users")
    private val friendRequestersMutableLiveData = MutableLiveData<MutableList<User>?>()


    //arkadaşlık isteği gönderen kullanıcılar hakkında bilgi alın
    fun downloadRequests(receivedRequests: List<String>): LiveData<MutableList<User>?> {

        val friendRequesters = mutableListOf<User>()

        for (receivedRequest in receivedRequests) {
            usersRef.document(receivedRequest).get().addOnSuccessListener {
                val user = it?.toObject(User::class.java)
                user?.let { it1 -> friendRequesters.add(it1) }
                friendRequestersMutableLiveData.value = friendRequesters

            }.addOnFailureListener {
                friendRequestersMutableLiveData.value = null
            }
        }
        return friendRequestersMutableLiveData
    }

    // Arkadaş ekleyince Kimlikleri Liste ekle
    fun addToFriends(
        requesterId: String,
        loggedUserId: String
    ) {

        deleteRequest(requesterId, loggedUserId)

        //Oturum açmış kullanıcı için ; gönderilen istek dizisine (sentRequest) kimlik ekle


        FirestoreUtil.firestoreInstance.collection("users").document(requesterId)
            .update(FRIENDS, FieldValue.arrayUnion(loggedUserId)).addOnSuccessListener {
                        //Oturum açmış kullanıcı için ; alınan istek dizine kimlik ekle
                FirestoreUtil.firestoreInstance.collection("users").document(loggedUserId)
                    .update(FRIENDS, FieldValue.arrayUnion(requesterId))
                            .addOnSuccessListener {

                            }.addOnFailureListener {

                            }
                    }.addOnFailureListener {

                    }
        }



    // Arkadaş ekleme işlemi bitince ;  Kimliği listeden kaldır
    fun deleteRequest(
        requesterId: String,
        loggedUserId: String
    ) {

        //Oturum açmış kullanıcı için ;gönderilen istek dizisinden (sentRequest) kimlik kaldır
                FirestoreUtil.firestoreInstance.collection("users").document(loggedUserId)
                    .update(RECEIVED_REQUEST_ARRAY, FieldValue.arrayRemove(requesterId))
                    .addOnSuccessListener {
                        //Oturum açmış kullanıcı için ; alınan istek dizine kimlik kaldır
                        FirestoreUtil.firestoreInstance.collection("users").document(requesterId)
                            .update(
                                SENT_REQUEST_ARRAY,
                                FieldValue.arrayRemove(loggedUserId)
                            )
                            .addOnSuccessListener {

                            }.addOnFailureListener {

                            }
                    }.addOnFailureListener {

                    }
    }

}




