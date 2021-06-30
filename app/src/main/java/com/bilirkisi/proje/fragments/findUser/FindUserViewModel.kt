package com.bilirkisi.proje.fragments.findUser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bilirkisi.proje.fragments.incoming_requests.FRIENDS
import com.google.firebase.firestore.EventListener
import com.bilirkisi.proje.models.User
import com.bilirkisi.proje.util.AuthUtil
import com.bilirkisi.proje.util.FirestoreUtil

class FindUserViewModel : ViewModel() {


    private val userDocumentsMutableLiveData = MutableLiveData<MutableList<User?>>()


    fun loadUsers(): LiveData<MutableList<User?>> {


        val docRef = FirestoreUtil.firestoreInstance.collection("users")
        docRef.get()
            .addOnSuccessListener { querySnapshot ->
                //Oturum açmamış herhangi bir kullanıcıyı ekleyin
                val result = mutableListOf<User?>()
                for (document in querySnapshot.documents) {
                    if (!document.get("uid").toString().equals(AuthUtil.getAuthId())) {
                        val user = document.toObject(User::class.java)
                        result.add(user)
                    }

                }


                // giriş yapan kullanıcının arkadaşlarını sonuç listesinden kaldır
                docRef.whereArrayContains(FRIENDS, AuthUtil.getAuthId())
                    .addSnapshotListener(
                        EventListener { querySnapshot, firebaseFirestoreException ->
                            if (firebaseFirestoreException == null) {
                                val documents = querySnapshot?.documents
                                if (documents != null) {
                                    for (document in documents) {
                                        val user = document.toObject(User::class.java)
                                        result.remove(user)

                                    }

                                    userDocumentsMutableLiveData.value = result


                                }
                            } else {
                                userDocumentsMutableLiveData.value = null
                            }
                        })





            }
            .addOnFailureListener { exception ->
                userDocumentsMutableLiveData.value = null
            }

        return userDocumentsMutableLiveData
    }


}
