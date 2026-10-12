package com.example.bloomday.ui.profile

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.bloomday.R
import com.example.bloomday.ui.mood.MoodStorage
import com.google.android.material.button.MaterialButton
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private val PREFS = "BloomDayPrefs"
    private val KEY_NAME = "profile_name"
    private val KEY_STUDENT_ID = "profile_student_id"
    private val KEY_EMAIL = "profile_email"
    private val KEY_BIO = "profile_bio"
    private val KEY_REMINDERS = "profile_reminders_enabled"
    private val KEY_PIC_URI = "profile_pic_uri"
    private val KEY_DARK_MODE = "profile_dark_mode"
    private val KEY_NOTIFICATIONS = "profile_notifications_enabled"

    // Views
    private lateinit var ivProfile: ImageView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var tvProfileTitle: TextView
    private lateinit var tvProfileSubtitle: TextView
    private lateinit var etName: EditText
    private lateinit var etStudentId: EditText
    private lateinit var etEmail: EditText
    private lateinit var etBio: EditText
    private lateinit var switchRemindersTop: Switch
    private lateinit var switchDarkMode: Switch
    private lateinit var switchNotifications: Switch
    private lateinit var btnChange: MaterialButton
    private lateinit var btnClear: MaterialButton
    private lateinit var btnClearAppData: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var tvSavedNote: TextView
    private lateinit var cardForm: View
    private lateinit var scroll: ScrollView
    private lateinit var layoutRecentMoods: LinearLayout
    private lateinit var tvNoMoods: TextView

    private var currentPicUri: Uri? = null

    // image picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                currentPicUri = uri
                ivProfile.setImageURI(uri)
                prefs.edit().putString(KEY_PIC_URI, uri.toString()).apply()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        prefs = requireActivity().getSharedPreferences(PREFS, 0)

        // find views
        scroll = view.findViewById(R.id.scrollProfile)
        ivProfile = view.findViewById(R.id.ivProfilePic)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        tvProfileTitle = view.findViewById(R.id.tvProfileTitle)
        tvProfileSubtitle = view.findViewById(R.id.tvProfileSubtitle)

        etName = view.findViewById(R.id.etProfileName)
        etStudentId = view.findViewById(R.id.etProfileStudentId)
        etEmail = view.findViewById(R.id.etProfileEmail)
        etBio = view.findViewById(R.id.etProfileBio)
        switchRemindersTop = view.findViewById(R.id.switchRemindersTop)
        switchDarkMode = view.findViewById(R.id.switchDarkMode)
        switchNotifications = view.findViewById(R.id.switchNotifications)

        btnChange = view.findViewById(R.id.btnProfileChange)
        btnClear = view.findViewById(R.id.btnProfileClear)
        btnClearAppData = view.findViewById(R.id.btnClearAppData)
        btnLogout = view.findViewById(R.id.btnLogout)
        tvSavedNote = view.findViewById(R.id.tvProfileSavedNote)
        cardForm = view.findViewById(R.id.cardForm)

        layoutRecentMoods = view.findViewById(R.id.layoutRecentMoods)
        tvNoMoods = view.findViewById(R.id.tvNoMoods)

        // populate UI
        loadProfile()
        loadSettingsToUi()
        loadRecentMoods()

        // click to pick image (avatar tap)
        ivProfile.setOnClickListener { pickImageLauncher.launch("image/*") }

        // open the edit form
        btnEditProfile.setOnClickListener { openEditForm() }

        // save / clear profile
        btnChange.setOnClickListener {
            saveProfile()
            persistSettings()
        }
        btnClear.setOnClickListener { clearProfile() }

        // clear all app data button
        btnClearAppData.setOnClickListener { confirmClearAppData() }

        // logout button
        btnLogout.setOnClickListener { confirmLogout() }

        // toggles persist immediately
        switchRemindersTop.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_REMINDERS, isChecked).apply()
        }
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()
            // apply App theme change if you implement it in your app
        }
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply()
            // tie into notification scheduling if you have one
        }

        return view
    }

    private fun openEditForm() {
        if (cardForm.visibility != View.VISIBLE) {
            cardForm.visibility = View.VISIBLE
            cardForm.post {
                scroll.smoothScrollTo(0, cardForm.top)
                etName.requestFocus()
            }
        } else {
            scroll.smoothScrollTo(0, cardForm.top)
        }
    }

    private fun loadProfile() {
        val name = prefs.getString(KEY_NAME, "") ?: ""
        etName.setText(name)
        etStudentId.setText(prefs.getString(KEY_STUDENT_ID, ""))
        etEmail.setText(prefs.getString(KEY_EMAIL, ""))
        etBio.setText(prefs.getString(KEY_BIO, ""))
        tvSavedNote.isVisible = false

        updateGreeting(name)

        val pic = prefs.getString(KEY_PIC_URI, null)
        if (!pic.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(pic)
                currentPicUri = uri
                ivProfile.setImageURI(uri)
            } catch (e: Exception) {
                ivProfile.setImageResource(R.drawable.ic_profile)
            }
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile)
        }
    }

    private fun updateGreeting(name: String?) {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isNotEmpty()) {
            tvProfileTitle.text = "Hello, $trimmed"
            tvProfileSubtitle.text = "Welcome back — keep your profile up to date"
        } else {
            tvProfileTitle.text = "Hello"
            tvProfileSubtitle.text = "Personalize your BloomDay profile"
        }
    }

    private fun saveProfile() {
        val name = etName.text.toString().trim()
        val studentId = etProfileStudentIdSafe()
        val email = etEmail.text.toString().trim()
        val bio = etBio.text.toString().trim()

        if (TextUtils.isEmpty(name)) {
            etName.error = "Please enter your name"
            etName.requestFocus()
            return
        }

        prefs.edit().apply {
            putString(KEY_NAME, name)
            putString(KEY_STUDENT_ID, studentId)
            putString(KEY_EMAIL, email)
            putString(KEY_BIO, bio)
            apply()
        }

        updateGreeting(name)
        tvSavedNote.text = "Profile updated"
        tvSavedNote.isVisible = true
        tvSavedNote.postDelayed({ tvSavedNote.isVisible = false }, 1400)
    }

    private fun etProfileStudentIdSafe(): String {
        return etStudentId.text.toString().trim()
    }

    private fun clearProfile() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear profile")
            .setMessage("This will erase saved profile information and remove the profile picture. Continue?")
            .setPositiveButton("Clear") { _, _ ->
                prefs.edit()
                    .remove(KEY_NAME)
                    .remove(KEY_STUDENT_ID)
                    .remove(KEY_EMAIL)
                    .remove(KEY_BIO)
                    .remove(KEY_PIC_URI)
                    .putBoolean(KEY_REMINDERS, true)
                    .apply()
                currentPicUri = null
                loadProfile()
                Toast.makeText(requireContext(), "Profile cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmClearAppData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear App Data")
            .setMessage("This will remove all saved settings and stored profiles (does not uninstall app). Continue?")
            .setPositiveButton("Clear") { _, _ ->
                // Clear SharedPreferences
                prefs.edit().clear().apply()
                // reset UI
                loadProfile()
                loadSettingsToUi()
                layoutRecentMoods.removeAllViews()
                tvNoMoods.isVisible = true
                Toast.makeText(requireContext(), "App data cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Do you want to log out?")
            .setPositiveButton("Logout") { _, _ ->
                // Simple logout: clear stored profile prefs and finish activity.
                prefs.edit().remove(KEY_NAME).remove(KEY_PIC_URI).apply()
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadSettingsToUi() {
        switchRemindersTop.isChecked = prefs.getBoolean(KEY_REMINDERS, true)
        switchDarkMode.isChecked = prefs.getBoolean(KEY_DARK_MODE, false)
        switchNotifications.isChecked = prefs.getBoolean(KEY_NOTIFICATIONS, true)
    }

    private fun persistSettings() {
        prefs.edit()
            .putBoolean(KEY_REMINDERS, switchRemindersTop.isChecked)
            .putBoolean(KEY_DARK_MODE, switchDarkMode.isChecked)
            .putBoolean(KEY_NOTIFICATIONS, switchNotifications.isChecked)
            .apply()
    }

    /**
     * loadRecentMoods: builds rows dynamically.
     * NOTE: colors changed here:
     *  - emoji color = purple "#9C27B0"
     *  - detail text color = muted gray "#6D6D6D"
     */
    private fun loadRecentMoods() {
        try {
            val moodStorage = MoodStorage(requireContext())
            val all = moodStorage.loadMoods()
            layoutRecentMoods.removeAllViews()
            if (all.isEmpty()) {
                tvNoMoods.isVisible = true
                return
            }
            tvNoMoods.isVisible = false
            val toShow = all.take(3)
            for (m in toShow) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.setMargins(0, 8, 0, 8) }
                }

                val tvEmoji = TextView(requireContext()).apply {
                    text = m.emoji
                    textSize = 22f
                    // themed emoji color
                    setTextColor(Color.parseColor("#9C27B0"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val tvDetail = TextView(requireContext()).apply {
                    text = (m.note.ifEmpty { "—" } + " • " + moodStorage.timestampToString(m.timestamp))
                    textSize = 14f
                    // muted detail color
                    setTextColor(Color.parseColor("#6D6D6D"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.setMargins(12, 0, 0, 0) }
                }

                row.addView(tvEmoji)
                row.addView(tvDetail)
                layoutRecentMoods.addView(row)
            }
        } catch (e: Exception) {
            layoutRecentMoods.removeAllViews()
            tvNoMoods.isVisible = true
        }
    }
}
