package com.jimenez.edison.shopplan.data

data class Product(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val estimatedPrice: Double = 0.0,
    val addWeekly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = "" // Para asociar productos con usuarios específicos
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "", 0.0, "", 0.0, false, 0L, "")
}