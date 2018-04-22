package com.dubedivine.tracker.data.remote

import android.content.Context
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot


class FireStorePersitanceHelper(private val context: Context,
                                private val successListener: OnSuccessListener<DocumentReference>,
                                private val onFailureListener: OnFailureListener) {

    var db = FirebaseFirestore.getInstance()

    fun persistNumberPlateToCloud(numberPlate: String) {
        numberPlateCollection()
                .add(
                        mapOf("number_plate" to numberPlate)
                ).addOnSuccessListener(successListener).addOnFailureListener(onFailureListener)
    }

   private fun numberPlateCollection(): CollectionReference {
       return db.collection(NUMBER_PLATES_COLLECTION)
    }

    fun getAllPlates(onCompleteListener: OnCompleteListener<QuerySnapshot>) {
        numberPlateCollection().get().addOnCompleteListener(onCompleteListener)
    }

    private companion object {
        private const val NUMBER_PLATES_COLLECTION = "NumberPlatesCollection"
    }
}