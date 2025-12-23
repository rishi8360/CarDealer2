package com.example.cardealer2.data

import com.google.firebase.firestore.DocumentReference

data class Product(
    val brandId: String = "",
    val productId: String = "",
    val colour : String="",
    val chassisNumber: String = "",
    val condition: String = "",
    val images: List<String> = emptyList(),
    val kms: Int = 0,
    val lastService: String = "",

    val previousOwners: Int=0,
    val price: Int = 0,
    val year: Int = 0,
    val type:String ="",
    val noc: List<String> = emptyList(),
    val rc: List<String> = emptyList(),
    val insurance: List<String> = emptyList(),
    val brokerOrMiddleMan: String = "",
    val owner: String = "",
    val brokerOrMiddleManRef: DocumentReference? = null,
    val ownerRef: DocumentReference? = null,
    val sold: Boolean = false
)
