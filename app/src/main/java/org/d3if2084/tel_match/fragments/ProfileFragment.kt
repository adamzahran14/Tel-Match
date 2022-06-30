package org.d3if2084.tel_match.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_profile.*
import org.d3if2084.tel_match.R
import org.d3if2084.tel_match.activities.TelmatchCallback
import org.d3if2084.tel_match.util.*

class ProfileFragment : Fragment() {

    private lateinit var userId: String
    private lateinit var userDatabase: DatabaseReference
    private var callback: TelmatchCallback? = null

    fun setCallback(callback: TelmatchCallback) {
        this.callback = callback
        userId = callback.onGetUserId()
        userDatabase = callback.getUserDatabase().child(userId)
    }

    //fungsi untuk menampilkan fragment dari profile
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    //fungsi untuk menampilkan spinner progress loading
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //mencegah user untuk tidak mengklik saat loading
        progressLayout.setOnTouchListener {view, event -> true}

        populateInfo()

        //untuk memanggil sistem agar bisa membuka file untuk foto
        photoIV.setOnClickListener { callback?.startActivityForPhoto() }

        applyButton.setOnClickListener { onApply() }
        signOutButton.setOnClickListener { callback?.onSignout() }
    }

    fun populateInfo() {
        progressLayout.visibility = View.VISIBLE
        userDatabase.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(isAdded) {
                    val user = snapshot.getValue(User::class.java)
                    nameET.setText(user?.name, TextView.BufferType.EDITABLE)
                    emailET.setText(user?.email, TextView.BufferType.EDITABLE)
                    ageET.setText(user?.age, TextView.BufferType.EDITABLE)
                    majorET.setText(user?.major, TextView.BufferType.EDITABLE)

                    if (user?.gender == GENDER_MALE) {
                        radioMan1.isChecked = true
                    }
                    if(user?.gender == GENDER_FEMALE) {
                        radioWoman1.isChecked = true
                    }
                    if (user?.preferredGender == GENDER_MALE) {
                        radioMan2.isChecked = true
                    }
                    if(user?.preferredGender == GENDER_FEMALE) {
                        radioWoman2.isChecked = true
                    }
                    if(!user?.imageUrl.isNullOrEmpty()) {
                        populateImage(user?.imageUrl!!)
                    }
                    progressLayout.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressLayout.visibility = View.GONE
            }

        })
    }

    //fungsi untuk apply profile ke realtime database
    fun onApply() {
       if(nameET.text.toString().isNullOrEmpty() ||
                emailET.text.toString().isNullOrEmpty() ||
                radioGroup1.checkedRadioButtonId == -1 ||
                radioGroup2.checkedRadioButtonId == -1 ) {
            Toast.makeText(context, getString(R.string.error_profile_incomplete), Toast.LENGTH_SHORT).show()
       } else {
           val name = nameET.text.toString()
           val age = ageET.text.toString()
           val email = emailET.text.toString()
           val major = majorET.text.toString()
           val gender =
               if(radioMan1.isChecked) GENDER_MALE
               else GENDER_FEMALE
           val preferredGender =
               if(radioMan2.isChecked) GENDER_MALE
               else GENDER_FEMALE

           userDatabase.child(DATA_NAME).setValue(name)
           userDatabase.child(DATA_AGE).setValue(age)
           userDatabase.child(DATA_EMAIL).setValue(email)
           userDatabase.child(DATA_MAJOR).setValue(major)
           userDatabase.child(DATA_GENDER).setValue(gender)
           userDatabase.child(DATA_GENDER_PREFERENCE).setValue(preferredGender)

           callback?.profileComplete()
       }
    }
    fun updateImageUri(uri: String) {
        userDatabase.child(DATA_IMAGE_URL).setValue(uri)
        populateImage(uri)
    }

    fun populateImage(uri: String) {
        Glide.with(this)
            .load(uri)
            .into(photoIV)
    }


}