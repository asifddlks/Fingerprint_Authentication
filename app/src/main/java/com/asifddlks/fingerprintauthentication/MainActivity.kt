package com.asifddlks.fingerprintauthentication

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.from
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private var cancellationSignal: CancellationSignal? = null

    private val  authenticationCallback: BiometricPrompt.AuthenticationCallback
    get() =
        @RequiresApi(Build.VERSION_CODES.P)
        object: BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)
                notifyUser("Authentication error: $errString")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                notifyUser("Authentication Success!")
                startActivity(Intent(this@MainActivity, Secret::class.java))

            }
        }

    var createCredentialsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            //doSomeOperations()
            //data.extras
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //checkBiometricSupport()
        //checkBiometricAvailable()
        if(checkBiometricAvailable())prepareBiometricAuthentication()

    }

    private fun prepareBiometricAuthentication() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val button = findViewById<Button>(R.id.btn_authenticate)
            button.setOnClickListener{
                val biometricPrompt : BiometricPrompt = BiometricPrompt.Builder(this)
                    .setTitle("Title")
                    .setSubtitle("Authentication is required")
                    .setDescription("Fingerprint Authentication")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                biometricPrompt.authenticate(getCancellationSignal(), mainExecutor, authenticationCallback)
            }
        }
        else{

        }

    }

    private fun  notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            notifyUser("Authentication was cancelled by the user")
        }
        return cancellationSignal as CancellationSignal
    }

    private fun checkBiometricSupport(): Boolean {

        val keyguardManager : KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if(!keyguardManager.isKeyguardSecure) {
            notifyUser("Fingerprint has not been enabled in settings.")
            return false
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.USE_BIOMETRIC) !=PackageManager.PERMISSION_GRANTED) {
            notifyUser("Fingerprint has not been enabled in settings.")
            return false
        }
        return if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            true
        } else true
    }

    private fun checkBiometricAvailable():Boolean{

        var isAvailable = false
        val biometricManager = androidx.biometric.BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
            {
                Log.d("MY_APP_TAG", "App can authenticate using biometrics.")
                isAvailable = true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
            {
                Log.e("MY_APP_TAG", "No biometric features available on this device.")
                isAvailable = false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
            {
                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.")
                isAvailable = false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Prompts the user to create credentials that your app accepts.
                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                }
                //startActivityForResult(enrollIntent, REQUEST_CODE)
                createCredentialsResultLauncher.launch(enrollIntent)
            }
        }
        return isAvailable

    }

}