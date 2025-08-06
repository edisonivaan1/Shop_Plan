package com.jimenez.edison.shopplan

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class Register : AppCompatActivity() {
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var buttonLogin: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        editTextEmail = findViewById(R.id.register_email)
        editTextPhone = findViewById(R.id.register_phone)
        // El ID sigue siendo el mismo y funciona perfectamente
        editTextPassword = findViewById(R.id.register_password)
        buttonRegister = findViewById(R.id.register_button)
        buttonLogin = findViewById(R.id.register_login)
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        // No se necesita ningún OnTouchListener aquí,
        // TextInputLayout se encarga de todo.

        buttonRegister.setOnClickListener {
            if (validateFields()) {
                registerUser()
            }
        }

        buttonLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser() {
        val email = editTextEmail.text.toString().trim()
        val phone = editTextPhone.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val name = email.substringBefore("@").replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }

                        val userData = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "phone" to phone,
                            // Nota de seguridad: En una app de producción real, NUNCA guardes
                            // contraseñas en texto plano en la base de datos.
                            // Firebase Auth ya la gestiona de forma segura.
                            // Puedes omitir esta línea en Firestore.
                            "password" to password,
                            "userId" to it.uid
                        )

                        db.collection("users").document(it.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this@Register, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@Register, Login::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@Register, "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validateFields(): Boolean {
        val email = editTextEmail.text.toString().trim()
        val phone = editTextPhone.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (email.isEmpty()) {
            editTextEmail.error = "El correo es obligatorio"
            editTextEmail.requestFocus()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.error = "Correo inválido"
            editTextEmail.requestFocus()
            return false
        }
        if (phone.isEmpty()) {
            editTextPhone.error = "El teléfono es obligatorio"
            editTextPhone.requestFocus()
            return false
        }
        if (!phone.matches(Regex("^0\\d{9}$"))) {
            editTextPhone.error = "El teléfono debe tener 10 dígitos y empezar con 0"
            editTextPhone.requestFocus()
            return false
        }
        if (password.isEmpty()) {
            editTextPassword.error = "La contraseña es obligatoria"
            editTextPassword.requestFocus()
            return false
        }
        if (password.length < 8) {
            editTextPassword.error = "La contraseña debe tener al menos 8 caracteres"
            editTextPassword.requestFocus()
            return false
        }
        return true
    }
}