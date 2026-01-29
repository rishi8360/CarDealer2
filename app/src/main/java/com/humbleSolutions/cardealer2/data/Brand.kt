package com.humbleSolutions.cardealer2.data


data class Brand(
    val vehicle: List<VehicleSummary> = emptyList(),
    val logo: String = "",
    val brandId: String ="",
    val modelNames: List<String> = emptyList()
)
