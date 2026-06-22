package com.linkly_linksystem.parent

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val auth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val editUsername = findViewById<AppCompatEditText>(R.id.editUsername)
        val editPassword = findViewById<AppCompatEditText>(R.id.editPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val textLoginHeader = findViewById<TextView>(R.id.textLoginHeader)
        val textLoginSubheader = findViewById<TextView>(R.id.textLoginSubheader)
        val textEmailLabel = findViewById<TextView>(R.id.textEmailLabel)
        val textForgotPassword = findViewById<TextView>(R.id.textForgotPassword)
        val btnToggleMode = findViewById<TextView>(R.id.btnToggleMode)

        // Toggle Login vs Sign Up Mode UI
        btnToggleMode.setOnClickListener {
            isSignUpMode = !isSignUpMode
            if (isSignUpMode) {
                textLoginHeader.text = "Create Parent Account"
                textLoginSubheader.text = "Register to establish child nodes and tracking zones."
                textEmailLabel.text = "Register Gmail Address"
                btnLogin.text = "Register Account"
                btnToggleMode.text = "Already have an account? Sign In"
                textForgotPassword.visibility = View.GONE
            } else {
                textLoginHeader.text = "Parent Portal Access"
                textLoginSubheader.text = "Sign in to your parental supervision dashboard."
                textEmailLabel.text = "Gmail Address"
                btnLogin.text = "Sign In"
                btnToggleMode.text = "Don't have an account? Sign Up"
                textForgotPassword.visibility = View.VISIBLE
            }
        }

        // Forgot Access Password Recovery
        textForgotPassword.setOnClickListener {
            val email = editUsername.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your Gmail address first.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (auth == null) {
                Toast.makeText(this, "Firebase Auth not available. Use admin bypass.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth?.sendPasswordResetEmail(email)
                ?.addOnSuccessListener {
                    Toast.makeText(this, "Reset link sent to your Gmail inbox.", Toast.LENGTH_LONG).show()
                }
                ?.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to send reset email: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }

        // Main Login/Register Trigger
        btnLogin.setOnClickListener {
            val email = editUsername.text.toString().trim()
            val password = editPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Failsafe Developer Admin Bypass
            if ((email == "admin@linkly.com" || email == "admin@gmail.com") && password == "admin123") {
                Toast.makeText(this, "Admin Failsafe Bypass Active!", Toast.LENGTH_SHORT).show()
                navigateToMain()
                return@setOnClickListener
            }

            if (auth == null) {
                Toast.makeText(this, "Firebase Auth Offline. Access denied.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isSignUpMode) {
                auth?.createUserWithEmailAndPassword(email, password)
                    ?.addOnSuccessListener {
                        Toast.makeText(this, "Account Registered Successfully!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                    ?.addOnFailureListener { e ->
                        Toast.makeText(this, "Registration Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            } else {
                auth?.signInWithEmailAndPassword(email, password)
                    ?.addOnSuccessListener {
                        Toast.makeText(this, "Authentication Successful!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                    ?.addOnFailureListener { e ->
                        Toast.makeText(this, "Authentication Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        editPassword.text?.clear()
                    }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
