package com.example.unkill.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.unkill.R
import com.example.unkill.preferences.NonNullListPreference

class SettingsActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Set default values before loading preferences
            // true means to set defaults even if they were already set before
            PreferenceManager.setDefaultValues(requireContext(), R.xml.preferences, true)

            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Force set the ListPreference value to ensure it's never null
            forceSetListPreferenceDefaults()


        }

        private fun forceSetListPreferenceDefaults() {
            val serviceInstancesPref = findPreference<com.example.unkill.preferences.NonNullListPreference>("service_instances")
            serviceInstancesPref?.let { pref ->
                if (pref.value == null || pref.value.isEmpty() || !pref.entryValues.contains(pref.value)) {
                    pref.value = "3"
                }
            }
        }




    }
}
