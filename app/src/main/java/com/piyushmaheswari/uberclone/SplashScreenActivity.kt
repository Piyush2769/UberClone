package com.piyushmaheswari.uberclone

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.piyushmaheswari.uberclone.Model.DriverInfo
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_splash_screen.*
import kotlinx.android.synthetic.main.layout_register.*
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {


    companion object {
        private val LOGIN_REQUEST_CODE = 7171
    }

    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseListener: FirebaseAuth.AuthStateListener


    private lateinit var database: FirebaseDatabase
    private lateinit var driverInfoRef: DatabaseReference


    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if (firebaseAuth != null && firebaseListener != null) firebaseAuth.removeAuthStateListener(
            firebaseListener
        )
        super.onStop()
    }

    @SuppressLint("CheckResult")
    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth.addAuthStateListener(firebaseListener)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        init()

    }

    private fun init() {

        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE)

        providers = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseListener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser
            if (user != null)
                checkUserFromFirebase()
            else
                showLoginLayout()
        }
    }

    private fun checkUserFromFirebase() {
        driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, error.message, Toast.LENGTH_LONG)
                        .show()
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "User already exists",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } else {
                        showRegisterLayout()
                    }
                }
            })
    }

    private fun showRegisterLayout() {

        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val phoneNumber=itemView.findViewById<View>(R.id.phone_number) as TextInputEditText
        val firstName=itemView.findViewById<View>(R.id.first_name) as TextInputEditText
        val lastName=itemView.findViewById<View>(R.id.last_name) as TextInputEditText

        val btnContinue=itemView.findViewById<Button>(R.id.btn_continue) as Button

        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
            !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
        )
            phoneNumber.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        btnContinue.setOnClickListener {
            if (TextUtils.isDigitsOnly(firstName.text.toString())) {
                Toast.makeText(
                    this@SplashScreenActivity,
                    "Please Enter First Name",
                    Toast.LENGTH_LONG
                )
                return@setOnClickListener
            } else if (TextUtils.isDigitsOnly(lastName.text.toString())) {
                Toast.makeText(
                    this@SplashScreenActivity,
                    "Please Enter Last Name",
                    Toast.LENGTH_LONG
                )
                return@setOnClickListener
            } else if (TextUtils.isDigitsOnly(phoneNumber.text.toString())) {
                Toast.makeText(
                    this@SplashScreenActivity,
                    "Please Enter Phone Number",
                    Toast.LENGTH_LONG
                )
                return@setOnClickListener
            } else {
                val model = DriverInfo()
                model.firstName = firstName.text.toString()
                model.lastName = lastName.text.toString()
                model.phoneNumber = phoneNumber.text.toString()
                model.rating = 0.0

                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener {
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "" + it.message,
                            Toast.LENGTH_LONG
                        )
                        dialog.dismiss()
                        progress_bar.visibility = View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "Registered Successfully",
                            Toast.LENGTH_LONG
                        )
                        dialog.dismiss()
                        progress_bar.visibility = View.GONE
                    }
            }
        }

    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.phone_button_sign_in)
            .setGoogleButtonId(R.id.google_button_sign_in)
            .build()

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
            , LOGIN_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            } else {
                Toast.makeText(
                    this@SplashScreenActivity,
                    "" + response!!.error!!.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

