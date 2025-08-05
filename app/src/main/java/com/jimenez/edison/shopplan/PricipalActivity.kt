package com.jimenez.edison.shopplan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jimenez.edison.shopplan.adapter.ProductAdapter
import com.jimenez.edison.shopplan.data.Product

class PricipalActivity : AppCompatActivity() {

    // Firebase
    private lateinit var firestore: FirebaseFirestore

    // UI Components
    private lateinit var totalText: TextView
    private lateinit var purchasedText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var finishButton: Button

    // Adapter
    private lateinit var productAdapter: ProductAdapter

    // Data
    private var productsList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pricipal)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar Firebase
        firestore = FirebaseFirestore.getInstance()

        // Inicializar vistas
        initializeViews()

        // Configurar RecyclerView
        setupRecyclerView()

        // Cargar productos desde Firebase
        loadProductsFromFirebase()

        // Configurar botón finish
        setupFinishButton()
    }

    private fun initializeViews() {
        totalText = findViewById(R.id.totalText)
        purchasedText = findViewById(R.id.purchasedText)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewProducts)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        finishButton = findViewById(R.id.button)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        productAdapter = ProductAdapter(
            products = productsList,
            onProductChecked = { product, isChecked ->
                updateTotalAndProgress()
                Log.d("PrincipalActivity", "Product ${product.name} checked: $isChecked")
            },
            onEditProduct = { product ->
                editProduct(product)
            },
            onDeleteProduct = { product ->
                deleteProduct(product)
            }
        )

        recyclerView.adapter = productAdapter
    }

    private fun setupFinishButton() {
        finishButton.setOnClickListener {
            val selectedProducts = productAdapter.getSelectedProducts()
            if (selectedProducts.isEmpty()) {
                Toast.makeText(this, "Select at least one product to finish", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val total = productAdapter.getTotalPrice()
            val count = selectedProducts.size

            Toast.makeText(
                this,
                "Shopping completed!\n$count products - Total: $${String.format("%.2f", total)}",
                Toast.LENGTH_LONG
            ).show()

            // Aquí podrías agregar lógica adicional como:
            // - Marcar productos como comprados
            // - Guardar el historial de compras
            // - Limpiar la lista
            // - Navegar a otra pantalla
        }
    }

    private fun loadProductsFromFirebase() {
        val userId = getCurrentUserId()
        Log.d("PrincipalActivity", "=== STARTING LOAD PRODUCTS ===")
        Log.d("PrincipalActivity", "Loading products for userId: $userId")

        if (userId.isEmpty()) {
            Log.e("PrincipalActivity", "UserId is empty!")
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            showEmptyState()
            return
        }

        // Mostrar toast para debug
        Toast.makeText(this, "Loading products for user: $userId", Toast.LENGTH_LONG).show()

        firestore.collection("products")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("PrincipalActivity", "=== QUERY SUCCESS ===")
                Log.d("PrincipalActivity", "Documents found: ${documents.size()}")

                productsList.clear()

                if (documents.isEmpty) {
                    Log.w("PrincipalActivity", "No documents found for userId: $userId")
                    Toast.makeText(this, "No products found for this user", Toast.LENGTH_LONG).show()
                }

                for (document in documents) {
                    try {
                        Log.d("PrincipalActivity", "Processing document: ${document.id}")
                        Log.d("PrincipalActivity", "Document data: ${document.data}")

                        val product = Product(
                            id = document.id,
                            name = document.getString("name") ?: "",
                            category = document.getString("category") ?: "",
                            quantity = document.getDouble("quantity") ?: 0.0,
                            unit = document.getString("unit") ?: "",
                            estimatedPrice = document.getDouble("estimatedPrice") ?: 0.0,
                            addWeekly = document.getBoolean("addWeekly") ?: false,
                            createdAt = document.getLong("createdAt") ?: 0L,
                            userId = document.getString("userId") ?: ""
                        )
                        productsList.add(product)

                        Log.d("PrincipalActivity", "Product loaded: ${product.name} - ${product.category} - ${product.estimatedPrice}")
                    } catch (e: Exception) {
                        Log.e("PrincipalActivity", "Error parsing product: ${e.message}", e)
                    }
                }

                Log.d("PrincipalActivity", "Total products in list: ${productsList.size}")

                // Actualizar UI
                if (productsList.isEmpty()) {
                    Log.d("PrincipalActivity", "Product list is empty, showing empty state")
                    showEmptyState()
                } else {
                    Log.d("PrincipalActivity", "Showing products list")
                    // Ordenar por fecha de creación en el cliente
                    productsList.sortByDescending { it.createdAt }

                    showProductsList()
                    productAdapter.updateProducts(productsList)
                    updateTotalAndProgress()

                    Toast.makeText(this, "Loaded ${productsList.size} products", Toast.LENGTH_SHORT).show()
                }

            }
            .addOnFailureListener { exception ->
                Log.e("PrincipalActivity", "=== QUERY FAILED ===")
                Log.e("PrincipalActivity", "Error loading products: ${exception.message}", exception)
                Toast.makeText(this, "Error loading products: ${exception.message}", Toast.LENGTH_LONG).show()
                showEmptyState()
            }
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE

        // Resetear valores
        totalText.text = "Total: $0.00"
        purchasedText.text = "0 / 0 purchased"
        progressBar.progress = 0
    }

    private fun showProductsList() {
        recyclerView.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }

    private fun updateTotalAndProgress() {
        val total = productAdapter.getTotalPrice()
        val selectedCount = productAdapter.getSelectedCount()
        val totalProducts = productsList.size

        // Actualizar texto total
        totalText.text = String.format("Total: $%.2f", total)

        // Actualizar texto de productos comprados
        purchasedText.text = "$selectedCount / $totalProducts purchased"

        // Actualizar barra de progreso
        val progress = if (totalProducts > 0) {
            ((selectedCount.toFloat() / totalProducts) * 100).toInt()
        } else {
            0
        }
        progressBar.progress = progress
    }

    private fun editProduct(product: Product) {
        // TODO: Implementar navegación a pantalla de edición
        Toast.makeText(this, "Edit ${product.name} - Coming soon!", Toast.LENGTH_SHORT).show()

        // Ejemplo de cómo podrías implementar la edición:
        /*
        val intent = Intent(this, EditProductActivity::class.java)
        intent.putExtra("PRODUCT_ID", product.id)
        intent.putExtra("PRODUCT_NAME", product.name)
        intent.putExtra("PRODUCT_CATEGORY", product.category)
        // ... más campos
        startActivityForResult(intent, EDIT_PRODUCT_REQUEST_CODE)
        */
    }

    private fun deleteProduct(product: Product) {
        // Mostrar diálogo de confirmación
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete '${product.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteProduct(product)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteProduct(product: Product) {
        firestore.collection("products")
            .document(product.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show()

                // Remover del adapter
                productAdapter.removeProduct(product)

                // Actualizar totales
                updateTotalAndProgress()

                // Verificar si la lista quedó vacía
                if (productsList.isEmpty()) {
                    showEmptyState()
                }

                Log.d("PrincipalActivity", "Product ${product.name} deleted successfully")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting product: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("PrincipalActivity", "Error deleting product", e)
            }
    }

    private fun getCurrentUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = if (currentUser != null) {
            Log.d("PrincipalActivity", "Authenticated user found: ${currentUser.uid}")
            currentUser.uid
        } else {
            Log.d("PrincipalActivity", "No authenticated user, using temp_user_id")
            // ID temporal para testing (mismo que en AddProduct)
            "temp_user_id"
        }
        Log.d("PrincipalActivity", "Using userId: $userId")
        return userId
    }

    override fun onResume() {
        super.onResume()
        // Recargar productos cuando se regrese a esta actividad
        // Útil si se agregan productos desde otra pantalla
        loadProductsFromFirebase()
    }
}