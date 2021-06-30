package com.bilirkisi.proje.fragments.different_user_profile

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bilirkisi.proje.fragments.incoming_requests.FRIENDS
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.bilirkisi.proje.R
import com.bilirkisi.proje.models.User
import com.bilirkisi.proje.util.AuthUtil

const val SENT_REQUEST_ARRAY = "sentRequests"
const val RECEIVED_REQUEST_ARRAY = "receivedRequests"

class DifferentUserProfileFragmentViewModel(val app: Application) : AndroidViewModel(app) {


    private val friendRequestStateMutableLiveData = MutableLiveData<FriendRequestState>()

    var loadedImage = MutableLiveData<RequestBuilder<Drawable>>()

    fun downloadProfilePicture(profilePictureUrl: String?) {
        println("DifferentUserProfileFragmentViewModel.downloadProfilePicture:$profilePictureUrl")
        if (profilePictureUrl == "null") return
        val load: RequestBuilder<Drawable> =
            Glide.with(app).load(profilePictureUrl).placeholder(R.drawable.anonymous_profile)
        loadedImage.value = load
    }

    fun updateSentRequestsForSender(uid: String?) {


        //oturum açmış kullanıcı için sentRequest(istek gönder) dizisine kimlik ekleyin
        val db = FirebaseFirestore.getInstance()
        if (uid != null) {
            AuthUtil.getAuthId().let {
                db.collection("users").document(it)
                    .update(SENT_REQUEST_ARRAY, FieldValue.arrayUnion(uid)).addOnSuccessListener {
                        //add loggedInUserId in receivedRequest array for other user
                        //diğer kullanıcı için receivedRequest (alınan İstek) dizisine logInUserId (Oturum Açmış Kullanıcı Kimliği) ekleyin
                        updateReceivedRequestsForReceiver(db, uid, AuthUtil.getAuthId())
                    }.addOnFailureListener {
                        throw it
                    }
            }
        }


    }

    private fun updateReceivedRequestsForReceiver(
        db: FirebaseFirestore,
        uid: String,
        loggedInUserId: String?
    ) {
        db.collection("users").document(uid)
            .update(RECEIVED_REQUEST_ARRAY, FieldValue.arrayUnion(loggedInUserId))
            .addOnSuccessListener {
            }.addOnFailureListener {
                throw it
            }
    }


    enum class FriendRequestState { SENT, NOT_SENT, ALREADY_FRIENDS }


    //kullanıcı oturum açtıysa belgeyi alın ve diğer kullanıcı kimliğinin sentRequest listesinde olup olmadığını kontrol edin
    fun checkIfFriends(recepiantId: String?): LiveData<FriendRequestState> {
        val db = FirebaseFirestore.getInstance()
        if (recepiantId != null) {
            AuthUtil.getAuthId().let {
                db.collection("users").document(it)
                    .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->

                        if (firebaseFirestoreException == null) {
                            val user =
                                documentSnapshot?.toObject(User::class.java)

                            // Kullanıcı Arkadaşlarının zaten olup olmadığını kontrol et
                            val friendsList = user?.friends
                            if (!friendsList.isNullOrEmpty()) {
                                //kullanıcının arkadaşları var
                                for (friendId in friendsList) {
                                    if (friendId == recepiantId) {
                                        friendRequestStateMutableLiveData.value =
                                            FriendRequestState.ALREADY_FRIENDS
                                        return@addSnapshotListener
                                    }
                                }
                            }

                            val sentRequests = user?.sentRequests
                            if (sentRequests != null) {
                                for (sentRequest in sentRequests) {
                                    if (sentRequest == recepiantId) {
                                        friendRequestStateMutableLiveData.value =
                                            FriendRequestState.SENT
                                        return@addSnapshotListener
                                    }
                                }
                                friendRequestStateMutableLiveData.value =
                                    FriendRequestState.NOT_SENT
                            }
                        } else {
                            println("DifferentUserProfileFragmentViewModel.checkIfFriends:${firebaseFirestoreException.message}")
                        }
                    }

            }


        }
        return friendRequestStateMutableLiveData
    }

    fun cancelFriendRequest(uid: String?) {

        //oturum açmış kullanıcı için ; sendRequest dizisinden kimliği kaldır
        val db = FirebaseFirestore.getInstance()
        if (uid != null) {
            AuthUtil.getAuthId().let {
                db.collection("users").document(it)
                    .update(SENT_REQUEST_ARRAY, FieldValue.arrayRemove(uid)).addOnSuccessListener {
                        //oturum açmış kullanıcı için ; diğer kullanıcı için ReceiveRequest dizisinden kaldır
                        db.collection("users").document(uid)
                            .update(
                                RECEIVED_REQUEST_ARRAY,
                                FieldValue.arrayRemove(AuthUtil.getAuthId())
                            )
                            .addOnSuccessListener {
                            }.addOnFailureListener {
                            }
                    }.addOnFailureListener {
                    }
            }
        }


    }

    fun removeFromFriends(uid: String?) {

        //oturum açmış kullanıcı için ; sendRequest dizisinden kimliği kaldır
        val db = FirebaseFirestore.getInstance()
        if (uid != null) {
            AuthUtil.getAuthId().let {
                db.collection("users").document(it)
                    .update(FRIENDS, FieldValue.arrayRemove(uid)).addOnSuccessListener {
                        //oturum açmış kullanıcı için ;  diğer kullanıcı için ReceiveRequest dizisinden kaldır
                        db.collection("users").document(uid)
                            .update(
                                FRIENDS,
                                FieldValue.arrayRemove(AuthUtil.getAuthId())
                            )
                            .addOnSuccessListener {
                            }.addOnFailureListener {
                            }
                    }.addOnFailureListener {
                    }
            }
        }


    }

}
