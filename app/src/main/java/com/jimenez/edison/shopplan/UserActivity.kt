package com.jimenez.edison.shopplan

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class UserActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var profileName: TextView
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var contentContainer: View

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        profileName = findViewById(R.id.user_profile_name)
        emailInput = findViewById(R.id.user_email_input)
        phoneInput = findViewById(R.id.user_phone_input)
        passwordInput = findViewById(R.id.user_password_input)

        // Agregar ProgressBar
        loadingProgressBar = findViewById(R.id.loading_progress_bar)

        val signOutButton = findViewById<Button>(R.id.user_sign_out_button)

        // Mostrar datos básicos inmediatamente desde Firebase Auth
        showBasicUserDataInstantly()

        // Luego cargar datos completos de Firestore
        loadUserDataFromFirestore()

        // Configurar botón de cerrar sesión
        signOutButton.setOnClickListener {
            // Cerrar sesión de Firebase
            auth.signOut()

            // Limpiar SharedPreferences si existen
            val sharedPref = getSharedPreferences("loginPrefs", MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.clear()
            editor.apply()

            // Redireccionar a Login
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
        }

        // Opcional: Agregar botón para actualizar datos
        // updateButton.setOnClickListener {
        //     updateUserData()
        // }
    }

    private fun showBasicUserDataInstantly() {
        // Mostrar datos inmediatamente desde Firebase Auth (no requiere consulta adicional)
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val email = user.email ?: ""
            val name = if (user.displayName.isNullOrEmpty()) {
                email.substringBefore("@").replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            } else {
                user.displayName!!
            }

            // Mostrar datos básicos inmediatamente
            profileName.text = name.uppercase()
            emailInput.setText(email)
            phoneInput.setText("Cargando...") // Placeholder mientras carga
            passwordInput.setText("********")
        }
    }

    private fun loadUserDataFromFirestore() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Intentar cargar desde cache primero (más rápido)
            db.collection("users").document(userId)
                .get(Source.CACHE)
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        updateUIWithFirestoreData(document.data)
                    } else {
                        // Si no hay cache, cargar desde servidor
                        loadFromServer(userId)
                    }
                }
                .addOnFailureListener {
                    // Si falla el cache, cargar desde servidor
                    loadFromServer(userId)
                }
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }
    }

    private fun loadFromServer(userId: String) {
        // showLoading(true)

        db.collection("users").document(userId)
            .get(Source.SERVER)
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document != null && document.exists()) {
                    updateUIWithFirestoreData(document.data)
                    Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No se encontraron datos adicionales", Toast.LENGTH_SHORT).show()
                    // Mantener los datos básicos que ya se mostraron
                    phoneInput.setText("") // Limpiar el "Cargando..."
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                phoneInput.setText("") // Limpiar el "Cargando..."
            }
    }

    private fun updateUIWithFirestoreData(data: Map<String, Any>?) {
        data?.let {
            val name = it["name"] as? String ?: profileName.text.toString()
            val email = it["email"] as? String ?: emailInput.text.toString()
            val phone = it["phone"] as? String ?: ""
            val password = it["password"] as? String ?: ""

            // Actualizar solo si hay nuevos datos
            if (name.isNotEmpty()) profileName.text = name.uppercase()
            if (email.isNotEmpty()) emailInput.setText(email)
            phoneInput.setText(phone)

            // Para la contraseña, mostrar asteriscos por seguridad
            if (password.isNotEmpty()) {
                passwordInput.setText("*".repeat(password.length.coerceAtMost(8)))
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (::loadingProgressBar.isInitialized) {
            loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun updateUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val updatedData = hashMapOf(
                "email" to emailInput.text.toString().trim(),
                "phone" to phoneInput.text.toString().trim()
                // No actualizar la contraseña desde aquí por seguridad
            )

            db.collection("users").document(userId)
                .update(updatedData as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}