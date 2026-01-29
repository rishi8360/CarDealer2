package com.humbleSolutions.cardealer2.data

import com.google.firebase.firestore.DocumentReference

/**
 * Represents a product in a catalog with its selling price
 * Used for catalog generation where prices can be modified
 */
data class CatalogProduct(
    val productRef: DocumentReference,
    val sellingPrice: Int
)


