package com.jimenez.edison.shopplan

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddProduct : AppCompatActivity() {
    private lateinit var spinnerUnit: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var productNameInput: TextInputEditText
    private lateinit var quantityInput: TextInputEditText
    private lateinit var priceInput: TextInputEditText
    private lateinit var weeklySwitch: Switch
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        firestore = FirebaseFirestore.getInstance()

        initializeViews()
        setupSpinners()
        setupButtonListeners()
    }

    private fun initializeViews() {
        spinnerUnit = findViewById(R.id.spinner2)
        spinnerCategory = findViewById(R.id.spinner)
        productNameInput = findViewById(R.id.productName)
        quantityInput = findViewById(R.id.editText2)
        priceInput = findViewById(R.id.editText3)
        weeklySwitch = findViewById(R.id.switchWeekly)
        saveButton = findViewById(R.id.buttonSave)
        cancelButton = findViewById(R.id.buttonCancel)
    }

    private fun setupSpinners() {
        // Setup Unit Spinner
        val unitValues = listOf(
            "piezas", "kg", "g", "lb", "litros", "ml", "paquetes",
            "cajas", "botellas", "latas", "docenas", "bolsas", "frascos", "tubos"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitValues)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnit.adapter = adapter

        // Setup Category Spinner
        val categories = listOf(
            "Frutas", "Verduras", "Lácteos", "Carnes", "Pescados y mariscos",
            "Panadería", "Cereales y granos", "Bebidas", "Snacks", "Limpieza",
            "Congelados", "Huevos", "Enlatados", "Salsas y condimentos", "Otros"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter
    }

    private fun setupButtonListeners() {
        saveButton.setOnClickListener {
            saveProductToFirebase()
        }

        cancelButton.setOnClickListener {
            finish() // Cierra la actividad y regresa a la anterior
        }
    }

    private fun saveProductToFirebase() {
        val productName = productNameInput.text.toString().trim()
        if (productName.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el nombre del producto", Toast.LENGTH_SHORT).show()
            return
        }

        val quantityText = quantityInput.text.toString().trim()
        if (quantityText.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa la cantidad", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = try {
            quantityText.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Por favor ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
            return
        }

        val priceText = priceInput.text.toString().trim()
        val estimatedPrice = if (priceText.isNotEmpty()) {
            try {
                priceText.replace("$", "").toDouble()
            } catch (e: NumberFormatException) {
                0.0
            }
        } else {
            0.0
        }

        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val productData = hashMapOf(
            "name" to productName,
            "category" to spinnerCategory.selectedItem.toString(),
            "quantity" to quantity,
            "unit" to spinnerUnit.selectedItem.toString(),
            "estimatedPrice" to estimatedPrice,
            "addWeekly" to weeklySwitch.isChecked,
            "userId" to userId,
            "createdAt" to System.currentTimeMillis()
        )

        saveButton.isEnabled = false

        firestore.collection("products")
            .add(productData)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto guardado exitosamente", Toast.LENGTH_SHORT).show()
                // Ya no es necesario navegar explícitamente si usamos finish(),
                // porque la lista se recargará en onResume(). Pero para asegurar, lo dejamos.
                val intent = Intent(this, ShoppingListActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish() // Cierra esta actividad
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar producto: ${e.message}", Toast.LENGTH_LONG).show()
                saveButton.isEnabled = true
                android.util.Log.e("AddProduct", "Error saving product", e)
            }
    }

    private fun getCurrentUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser?.uid ?: "" // Forma más concisa de devolver el uid o un string vacío
    }
}