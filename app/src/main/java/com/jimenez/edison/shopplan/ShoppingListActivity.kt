package com.jimenez.edison.shopplan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jimenez.edison.shopplan.adapter.ProductAdapter
import com.jimenez.edison.shopplan.data.Product

class ShoppingListActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var summaryLayout: LinearLayout
    private lateinit var productAdapter: ProductAdapter
    private lateinit var addFab: FloatingActionButton

    private lateinit var totalText: TextView
    private lateinit var purchasedText: TextView
    private lateinit var progressBar: ProgressBar

    private val productsList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_list)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initializeViews()
        setupRecyclerView()
        setupListeners()
        setupAuthStateListener()
    }

    // Adjuntamos el oyente cuando la app se inicia
    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    // Lo quitamos cuando la app se detiene para ahorrar recursos
    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewProducts)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        summaryLayout = findViewById(R.id.summaryLayout)
        addFab = findViewById(R.id.addFab)
        totalText = findViewById(R.id.totalText)
        purchasedText = findViewById(R.id.purchasedText)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Si hay un usuario, cargamos sus productos
                Log.d("AUTH_STATE", "Usuario detectado: ${user.uid}. Cargando productos.")
                loadProductsFromFirebase(user.uid)
            } else {
                // Si no hay usuario, mostramos la lista vacía y podríamos redirigir al Login
                Log.d("AUTH_STATE", "No hay usuario. Mostrando estado vacío.")
                toggleViews(isListEmpty = true)
            }
        }
    }

    private fun setupListeners() {
        addFab.setOnClickListener {
            startActivity(Intent(this, AddProduct::class.java))
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            products = productsList,
            onProductChecked = { _, _ -> updateTotalAndProgress() },
            onEditProduct = { product ->
                Toast.makeText(this, "Editar: ${product.name}", Toast.LENGTH_SHORT).show()
            },
            onDeleteProduct = { product ->
                showDeleteConfirmationDialog(product)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = productAdapter
    }

    private fun loadProductsFromFirebase(userId: String) {
        if (userId.isEmpty()) {
            toggleViews(isListEmpty = true)
            return
        }

        firestore.collection("products")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                productsList.clear()
                if (documents.isEmpty) {
                    Log.d("FIRESTORE_DATA", "La consulta tuvo éxito pero no encontró documentos para el usuario: $userId")
                }
                for (document in documents) {
                    val product = document.toObject(Product::class.java).copy(id = document.id)
                    productsList.add(product)
                }
                productAdapter.updateProducts(productsList)
                toggleViews(productsList.isEmpty())
                updateTotalAndProgress()
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE_ERROR", "Error al cargar productos", exception)
                Toast.makeText(this, "Error al cargar productos: ${exception.message}", Toast.LENGTH_LONG).show()
                toggleViews(isListEmpty = true)
            }
    }

    private fun toggleViews(isListEmpty: Boolean) {
        if (isListEmpty) {
            recyclerView.visibility = View.GONE
            summaryLayout.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            summaryLayout.visibility = View.VISIBLE
        }
    }

    private fun updateTotalAndProgress() {
        val total = productAdapter.getTotalPrice()
        val selectedCount = productAdapter.getSelectedCount()
        val totalProducts = productsList.size

        totalText.text = String.format("Total: $%.2f", total)
        purchasedText.text = "$selectedCount / $totalProducts purchased"

        progressBar.progress = if (totalProducts > 0) {
            (selectedCount * 100 / totalProducts)
        } else {
            0
        }
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que quieres eliminar '${product.name}'?")
            .setPositiveButton("Sí, eliminar") { _, _ ->
                deleteProductFromFirebase(product)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteProductFromFirebase(product: Product) {
        firestore.collection("products").document(product.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                productAdapter.removeProduct(product)
                updateTotalAndProgress()
                toggleViews(productsList.isEmpty())
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}