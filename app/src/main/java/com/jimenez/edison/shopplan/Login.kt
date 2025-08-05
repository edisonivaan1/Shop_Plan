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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class Login : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100
    private lateinit var callbackManager: CallbackManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth
        // Referencias UI
        val emailInput = findViewById<EditText>(R.id.login_email)
        val passwordInput = findViewById<EditText>(R.id.login_password)
        val loginButton = findViewById<Button>(R.id.register_button)
        val rememberMe = findViewById<CheckBox>(R.id.login_remember_me)
        val forgotPassword = findViewById<TextView>(R.id.login_forgot_password)
        val signupButton = findViewById<Button>(R.id.login_signup_button)
        val googleButton = findViewById<Button>(R.id.login_google_button)
        val facebookButton = findViewById<Button>(R.id.login_facebook_button)

        // Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        // Facebook Login
        FacebookSdk.sdkInitialize(applicationContext)
        callbackManager = CallbackManager.Factory.create()
        facebookButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
            LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        // Usuario autenticado con Facebook
                        val intent = Intent(this@Login, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    override fun onCancel() {}
                    override fun onError(error: FacebookException) {
                        Toast.makeText(this@Login, "Error al iniciar sesión con Facebook", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // Acción al presionar el botón de Sign Up
        signupButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty()) {
                emailInput.error = "El correo es obligatorio"
                emailInput.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordInput.error = "La contraseña es obligatoria"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            AutenticarUsuario(email, password)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
            }
        }
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    fun AutenticarUsuario(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val exception = task.exception
                    when {
                        exception?.message?.contains("no user record") == true ||
                                exception?.message?.contains("There is no user") == true -> {
                            Toast.makeText(this, "El correo no está registrado", Toast.LENGTH_SHORT).show()
                        }
                        exception?.message?.contains("password is invalid") == true ||
                                exception?.message?.contains("The password is invalid") == true -> {
                            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Error: ${exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }
}