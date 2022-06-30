package org.d3if2084.tel_match.activities

import com.google.firebase.database.DatabaseReference

interface TelmatchCallback {
    fun onSignout()
    fun onGetUserId(): String
    fun getUserDatabase(): DatabaseReference
    fun getChatDatabase(): DatabaseReference
    fun profileComplete()
    fun startActivityForPhoto()
}