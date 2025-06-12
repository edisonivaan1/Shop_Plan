package com.jimenez.edison.shopplan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obtener referencias a los elementos del diseño
        val emailInput = findViewById<EditText>(R.id.login_email)
        val passwordInput = findViewById<EditText>(R.id.login_password)
        val loginButton = findViewById<Button>(R.id.register_button)
        val rememberMe = findViewById<CheckBox>(R.id.login_remember_me)
        val forgotPassword = findViewById<TextView>(R.id.login_forgot_password)

        // Acción al presionar el botón de Login
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email == "nelson.angulo@epn.edu.ec" && password == "12345") {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Cierra esta actividad al iniciar MainActivity
            } else {
                Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            }
        }

        // Acción al presionar "Forgot Password"
        forgotPassword.setOnClickListener {
            Toast.makeText(this, "Función de recuperación en desarrollo", Toast.LENGTH_SHORT).show()
        }

        // Guardar preferencia de "Remember Me"
        rememberMe.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Recordando usuario...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}