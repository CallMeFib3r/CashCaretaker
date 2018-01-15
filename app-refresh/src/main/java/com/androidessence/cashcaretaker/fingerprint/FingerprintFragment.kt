package com.androidessence.cashcaretaker.fingerprint

import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.androidessence.cashcaretaker.R
import com.androidessence.cashcaretaker.main.MainController
import kotlinx.android.synthetic.main.fragment_fingerprint.*
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Fragment that authenticates the user's fingerprint before moving forward.
 *
 * @property[mainController] The main activity that this fragment sits in. It is needed to change the backstack upon authorization.
 * @property[fingerprintController] The controller used to start and stop listening for authorization.
 * @property[keyStore] The device key store used for crypto objects required by Fingerprint Manager.
 * @property[keyGenerator] Used to generate a key for the fingerprint auth that will get stored in the device keystore.
 */
@RequiresApi(23)
class FingerprintFragment : Fragment(), FingerprintController.Callback {
    private val mainController: MainController by lazy { activity as MainController }
    private val fingerprintController: FingerprintController by lazy {
        FingerprintController(FingerprintManagerCompat.from(context!!), this, fingerprint_text, fingerprint_image)
    }

    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to get an instance of KeyStore", e)
        }

        try {
            keyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        } catch (e: Exception) {
            throw RuntimeException("Failed to get an instance of KeyGenerator", e)
        }


        createKey(DEFAULT_NAME, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_fingerprint, container, false)

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()

        val defaultCipher: Cipher =
                try {
                    Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7)
                } catch (e: Exception) {
                    throw RuntimeException("Failed to get an instance of Cipher", e)
                }

        if (initCipher(defaultCipher, DEFAULT_NAME)) {
            fingerprintController.startListening(FingerprintManagerCompat.CryptoObject(defaultCipher))
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     *
     * @param keyName the name of the key to be created
     * @param invalidatedByBiometricEnrollment if `false` is passed, the created key will not
     * be invalidated even if a new fingerprint is enrolled.
     * The default value is `true`, so passing
     * `true` doesn't change the behavior
     * (the key will be invalidated if a new fingerprint is
     * enrolled.). Note that this parameter is only valid if
     * the app works on Android N developer preview.
     */
    private fun createKey(keyName: String, invalidatedByBiometricEnrollment: Boolean) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            keyStore?.load(null)
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            val builder = KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level +24.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
            }
            keyGenerator?.init(builder.build())
            keyGenerator?.generateKey()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    override fun onPause() {
        (activity as AppCompatActivity).supportActionBar?.show()
        fingerprintController.stopListening()
        super.onPause()
    }

    /**
     * Initialize the [Cipher] instance with the created key in the
     * [.createKey] method.
     *
     * @param keyName the key name to init the cipher
     * @return `true` if initialization is successful, `false` if the lock screen has
     * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
     * the key was generated.
     */
    private fun initCipher(cipher: Cipher, keyName: String): Boolean {
        try {
            keyStore?.load(null)
            val key = keyStore?.getKey(keyName, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return true
        } catch (e: Exception) {
            throw RuntimeException("Failed to init Cipher", e)
        }

    }

    override fun onError() {
        fingerprintController.stopListening()
        fingerprint_text.text = getString(R.string.fingerprint_fatal_error)
    }

    override fun onAuthenticated() {
        fingerprintController.stopListening()
        mainController.showAccounts()
    }

    companion object {
        /**
         * The name of the key to create for authentication.
         */
        private val DEFAULT_NAME = "default_key"

        /**
         * A tag for this fragment to define it in the backstack.
         */
        val FRAGMENT_NAME: String = FingerprintFragment::class.java.simpleName

        /**
         * @return A new instance of the FingerprintFragment.
         */
        fun newInstance(): FingerprintFragment = FingerprintFragment()
    }
}