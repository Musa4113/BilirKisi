package com.bilirkisi.proje.fragments.groupchat

import android.net.Uri
import android.util.Log.d
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bilirkisi.proje.models.*
import com.bilirkisi.proje.util.FirestoreUtil
import com.bilirkisi.proje.util.StorageUtil
import kotlinx.coroutines.async
import java.io.File
import java.util.*

class ChatViewModel(val senderId: String?, val groupname: String) : ViewModel() {

    private lateinit var mStorageRef: StorageReference
    private val messageCollectionReference = FirestoreUtil.firestoreInstance.collection("messages")
    private val messagesList: MutableList<Message> by lazy { mutableListOf<Message>() }
    private val chatFileMapMutableLiveData = MutableLiveData<Map<String, Any?>>()
    private val messagesMutableLiveData = MutableLiveData<List<Message>>()
    private val chatImageDownloadUriMutableLiveData = MutableLiveData<Uri>()
    val chatRecordDownloadUriMutableLiveData = MutableLiveData<Uri>()


    fun loadMessages(): LiveData<List<Message>> {

        if (messagesMutableLiveData.value != null) return messagesMutableLiveData

        messageCollectionReference.addSnapshotListener(EventListener { querySnapShot, firebaseFirestoreException ->
            if (firebaseFirestoreException == null) {
                messagesList.clear()//mesaj listesini temizle, böylece her yeni mesajda kopyalanmaz
                querySnapShot?.documents?.forEach {
                    if (it.id == groupname) {

                        // Okuyacağımız sohbet belgesidir
                        val messagesFromFirestore =
                            it.get("messages") as List<HashMap<String, Any>>?
                                ?:return@EventListener
                        messagesFromFirestore.forEach { messageHashMap ->

                            val message = when (messageHashMap["type"] as Double?) {
                                0.0 -> {
                                    messageHashMap.toDataClass<TextMessage>()
                                }
                                1.0 -> {
                                    messageHashMap.toDataClass<ImageMessage>()
                                }
                                2.0 -> {
                                    messageHashMap.toDataClass<FileMessage>()
                                }
                                3.0 -> {
                                    messageHashMap.toDataClass<RecordMessage>()
                                }
                                else -> {
                                    throw Exception("unknown type")
                                }
                            }


                            messagesList.add(message)
                        }

                        if (!messagesList.isNullOrEmpty())
                            messagesMutableLiveData.value = messagesList
                    }

                }
            }
        })

        return messagesMutableLiveData
    }
suspend fun getNumberOfGroupMembers() : Int{
    var size = -1
    viewModelScope.async{
        messageCollectionReference.document(groupname).get().addOnCompleteListener {
                documentSnapshot ->
            val document : DocumentSnapshot? = documentSnapshot.getResult()
            val listofchatmembers : List<String> = document?.get("chat_members_in_group") as List<String>
            d("gghh", listofchatmembers.size.toString())
             size = listofchatmembers.size
        } }.join()
    return  size
    }
    fun sendMessage(message: Message) {
        //
        messageCollectionReference.document(groupname).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    //Mesajını gönder
                    messageCollectionReference.document(groupname)
                        .update("messages", FieldValue.arrayUnion(message.serializeToMap()))

                } else {
                    //gönderen_alıcı kimliği mevcut değil
                    messageCollectionReference.document(groupname).get()
                        .addOnSuccessListener { documentSnapshot2 ->

                            if (documentSnapshot2.exists()) {
                                messageCollectionReference.document(groupname)
                                    .update(
                                        "messages",
                                        FieldValue.arrayUnion(message.serializeToMap())
                                    )
                            } else {
                                //önceki sohbet geçmişi yok gönderen_alıcı
                                //Mesaj dçkümanını oluşturduk
                                messageCollectionReference.document(groupname)
                                    .set(
                                        mapOf("messages" to mutableListOf<Message>()),
                                        SetOptions.merge()
                                    ).addOnSuccessListener {
                                        // mesajınızı gönderin
                                        messageCollectionReference.document(groupname)
                                            .update(
                                                "messages",
                                                FieldValue.arrayUnion(message.serializeToMap())
                                            )

                                        //sohbet üyelerinin kimliklerini ekle
                                        messageCollectionReference.document(groupname)
                                            .update(
                                                "chat_members_in_group",
                                                FieldValue.arrayUnion(senderId)
                                            )

                                    }
                            }
                        }
                }
            }

    }

    fun uploadChatFileByUri(filePath: Uri?): LiveData<Map<String, Any?>> {

        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("chat_files/" + filePath.toString())
        var uploadTask = filePath?.let { ref.putFile(it) }

        uploadTask?.continueWithTask { task ->
            if (!task.isSuccessful) {
                //error
                println("SharedViewModel.uploadChatImageByUri:error1 ${task.exception?.message}")
            }
            ref.downloadUrl
        }?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                println("SharedViewModel.uploadChatImageByUri:on complete")
                chatFileMapMutableLiveData.value = mapOf<String, Any?>(
                    "downloadUri" to downloadUri,
                    "fileName" to filePath
                )


            } else {
                //error
                println("SharedViewModel.uploadChatImageByUri:error2 ${task.exception?.message}")
            }
        }
        return chatFileMapMutableLiveData
    }

    fun uploadRecord(filePath: String) {

        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("records/" + Date().time)
        var uploadTask = ref.putFile(Uri.fromFile(File(filePath)))

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                //error
            }
            ref.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                chatRecordDownloadUriMutableLiveData.value = downloadUri
            } else {
                //error
            }
        }
    }

    fun uploadChatImageByUri(data: Uri?): LiveData<Uri> {
        mStorageRef = StorageUtil.storageInstance.reference
        val ref = mStorageRef.child("chat_pictures/" + data?.path)
        var uploadTask = data?.let { ref.putFile(it) }

        uploadTask?.continueWithTask { task ->
            if (!task.isSuccessful) {
                //error
            }
            ref.downloadUrl
        }?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                chatImageDownloadUriMutableLiveData.value = downloadUri

            } else {
                //error
            }
        }
        return chatImageDownloadUriMutableLiveData
    }

}

val gson = Gson()

//convert a data class to a map
fun <T> T.serializeToMap(): Map<String, Any> {
    return convert()
}

//convert a map to a data class
inline fun <reified T> Map<String, Any>.toDataClass(): T {
    return convert()
}

//convert an object of type I to type O
inline fun <I, reified O> I.convert(): O {
    val json = gson.toJson(this)
    return gson.fromJson(json, object : TypeToken<O>() {}.type)
}