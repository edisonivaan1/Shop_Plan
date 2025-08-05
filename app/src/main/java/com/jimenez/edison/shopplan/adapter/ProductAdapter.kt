package com.jimenez.edison.shopplan.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jimenez.edison.shopplan.R
import com.jimenez.edison.shopplan.data.Product

class ProductAdapter(
    private var products: MutableList<Product>,
    private val onProductChecked: (Product, Boolean) -> Unit,
    private val onEditProduct: (Product) -> Unit,
    private val onDeleteProduct: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // Set para mantener track de productos seleccionados
    private val selectedProducts = mutableSetOf<String>()

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryText: TextView = itemView.findViewById(R.id.product_category)
        val productText: TextView = itemView.findViewById(R.id.product_name)
        val switchText: TextView = itemView.findViewById(R.id.product_weekly)
        val quantityUnitText: TextView = itemView.findViewById(R.id.product_quantity_unit)
        val priceText: TextView = itemView.findViewById(R.id.product_price)
        val checkbox: CheckBox = itemView.findViewById(R.id.product_checkbox)
        val editButton: ImageButton = itemView.findViewById(R.id.product_edit_button)
        val deleteButton: ImageButton = itemView.findViewById(R.id.product_delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        // Mostrar información del producto
        holder.categoryText.text = product.category
        holder.productText.text = product.name
        holder.switchText.text = if (product.addWeekly) "Weekly" else "Once"

        // Formatear cantidad y unidad
        val quantityFormatted = if (product.quantity % 1.0 == 0.0) {
            product.quantity.toInt().toString()
        } else {
            String.format("%.1f", product.quantity)
        }
        holder.quantityUnitText.text = "$quantityFormatted${product.unit}"

        // Formatear precio
        holder.priceText.text = if (product.estimatedPrice > 0) {
            String.format("$%.2f", product.estimatedPrice)
        } else {
            "$0.00"
        }

        // Estado del checkbox
        holder.checkbox.isChecked = selectedProducts.contains(product.id)

        // Listeners
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedProducts.add(product.id)
            } else {
                selectedProducts.remove(product.id)
            }
            onProductChecked(product, isChecked)
        }

        holder.editButton.setOnClickListener {
            onEditProduct(product)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteProduct(product)
        }
    }

    override fun getItemCount(): Int = products.size

    // Métodos para actualizar la lista
    fun updateProducts(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }

    fun removeProduct(product: Product) {
        val position = products.indexOfFirst { it.id == product.id }
        if (position != -1) {
            products.removeAt(position)
            selectedProducts.remove(product.id)
            notifyItemRemoved(position)
        }
    }

    // Métodos para obtener información de selección
    fun getSelectedProducts(): List<Product> {
        return products.filter { selectedProducts.contains(it.id) }
    }

    fun getSelectedCount(): Int = selectedProducts.size

    fun getTotalPrice(): Double {
        return products.filter { selectedProducts.contains(it.id) }
            .sumOf { it.estimatedPrice }
    }

    fun clearSelections() {
        selectedProducts.clear()
        notifyDataSetChanged()
    }
}