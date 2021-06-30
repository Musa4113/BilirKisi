package com.bilirkisi.proje.fragments.register

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.google.firebase.auth.FirebaseAuth
import com.bilirkisi.proje.models.User
import com.bilirkisi.proje.util.ErrorMessage
import com.bilirkisi.proje.util.FirestoreUtil
import com.bilirkisi.proje.util.LoadState


class SignupViewModel : ViewModel() {

    val navigateToHomeMutableLiveData = MutableLiveData<Boolean?>()
    val loadingState = MutableLiveData<LoadState>()


    fun registerEmail(
        auth: FirebaseAuth,
        email: String,
        password: String,
        username: String,
        expertUser: Boolean        //***
    ) {

        loadingState.value = LoadState.LOADING

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                storeUserInFirestore(User(it.user?.uid, username, email,expertUser))
            }.addOnFailureListener {
                ErrorMessage.errorMessage = it.message
                loadingState.value = LoadState.FAILURE
            }

    }


    fun storeUserInFirestore(user: User) {
        val db = FirestoreUtil.firestoreInstance
        user.uid?.let { uid ->
            db.collection("users").document(uid).set(user).addOnSuccessListener {
                navigateToHomeMutableLiveData.value = true
            }.addOnFailureListener {
                loadingState.value = LoadState.FAILURE
                ErrorMessage.errorMessage = it.message
            }
        }

    }


    fun doneNavigating() {
        navigateToHomeMutableLiveData.value = null
    }

}