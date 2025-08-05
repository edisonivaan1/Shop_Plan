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

class Register : AppCompatActivity() {
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var buttonLogin: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        editTextEmail = findViewById(R.id.register_email)
        editTextPhone = findViewById(R.id.register_phone)
        editTextPassword = findViewById(R.id.register_password)
        buttonRegister = findViewById(R.id.register_button)
        buttonLogin = findViewById(R.id.register_login)
        auth = Firebase.auth

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
        val email = editTextEmail.text.toString()
        val password = editTextPassword.text.toString()
        // El teléfono no se usa en Firebase Auth por defecto, pero puedes guardarlo en tu base de datos si lo necesitas

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validateFields(): Boolean {
        val email = editTextEmail.text.toString()
        val phone = editTextPhone.text.toString()
        val password = editTextPassword.text.toString()

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
        if (!phone.matches(Regex("^0\\d{9}\$"))) {
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