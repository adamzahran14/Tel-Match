package org.d3if2084.tel_match.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.lorentzos.flingswipe.SwipeFlingAdapterView
import kotlinx.android.synthetic.main.fragment_swipe.*
import org.d3if2084.tel_match.R
import org.d3if2084.tel_match.activities.TelmatchCallback
import org.d3if2084.tel_match.adapter.CardsAdapter
import org.d3if2084.tel_match.util.*

class SwipeFragment : Fragment() {

    private var callback: TelmatchCallback? = null
    private lateinit var userId: String
    private lateinit var userDatabase: DatabaseReference
    private lateinit var chatDatabase: DatabaseReference
    private var cardsAdapter: ArrayAdapter<User>? = null
    private var rowItems = ArrayList<User>()
    private var preferredGender: String? = null
    private var userName: String? = null
    private var imageUrl: String? = null


    fun setCallback(callback: TelmatchCallback) {
        this.callback = callback
        userId = callback.onGetUserId()
        userDatabase = callback.getUserDatabase()
        chatDatabase = callback.getChatDatabase()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_swipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userDatabase.child(userId).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                preferredGender = user?.preferredGender
                userName = user?.name
                imageUrl = user?.imageUrl
                populateItems()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        cardsAdapter = CardsAdapter(context, R.layout.item, rowItems)

        frame.adapter = cardsAdapter
        frame.setFlingListener(object: SwipeFlingAdapterView.onFlingListener {
            override fun removeFirstObjectInAdapter() {
                rowItems.removeAt(0)
                cardsAdapter?.notifyDataSetChanged()
            }

            // ketika user melakukan left swipe, maka user id yang di swipe left akan muncul di database user id yanng melakukan swipe left
            override fun onLeftCardExit(p0: Any?) {
                var user = p0 as User
                userDatabase.child(user.uid.toString()).child(DATA_SWIPES_LEFT).child(userId).setValue(true)
            }

            //ketika user melakukan right swipe, maka user id yang di swipe right akan muncul di database user id yanng melakukan swipe right
            override fun onRightCardExit(p0: Any?) {
                var selectedUser = p0 as User
                val selectedUserId = selectedUser.uid
                if (!selectedUserId.isNullOrEmpty()) {
                    userDatabase.child(userId).child(DATA_SWIPES_RIGHT).addListenerForSingleValueEvent(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.hasChild(selectedUserId)) {
                                Toast.makeText(context, "Match!", Toast.LENGTH_SHORT).show()

                                // generate key for database
                                val chatKey = chatDatabase.push().key

                                if (chatKey != null) {
                                    userDatabase.child(userId).child(DATA_SWIPES_RIGHT)
                                        .child(selectedUserId).removeValue()
                                    userDatabase.child(userId).child(DATA_MATCHES)
                                        .child(selectedUserId).setValue(chatKey)
                                    userDatabase.child(selectedUserId).child(DATA_MATCHES)
                                        .child(userId).setValue(chatKey)

                                    chatDatabase.child(chatKey).child(userId).child(DATA_NAME)
                                        .setValue(userName)
                                    chatDatabase.child(chatKey).child(userId).child(DATA_IMAGE_URL)
                                        .setValue(imageUrl)

                                    chatDatabase.child(chatKey).child(selectedUserId).child(DATA_NAME)
                                        .setValue(selectedUser.name)
                                    chatDatabase.child(chatKey).child(selectedUserId).child(DATA_IMAGE_URL)
                                        .setValue(selectedUser.imageUrl)

                                }
                            } else {
                                userDatabase.child(selectedUserId).child(DATA_SWIPES_RIGHT).child(userId).setValue(true)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
                }
            }

            override fun onAdapterAboutToEmpty(p0: Int) {

            }

            override fun onScroll(p0: Float) {

            }
        })

        frame.setOnItemClickListener { position, data -> }

        likeButton.setOnClickListener{
            if(!rowItems.isEmpty()) {
                frame.topCardListener.selectRight()
            }
        }

        dislikeButton.setOnClickListener{
            if (!rowItems.isEmpty()) {
                frame.topCardListener.selectLeft()
            }
        }
    }

    //fungsi untuk menjalankan swipe fragment
    fun populateItems() {
        noUsersLayout.visibility = View.GONE
        progressLayout.visibility = View.VISIBLE

        val cardsQuery = userDatabase.orderByChild(DATA_GENDER).equalTo(preferredGender)
        cardsQuery.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    val user = child.getValue(User::class.java)
                    if(user != null) {
                        var showUser = true
                        if (child.child(DATA_SWIPES_LEFT).hasChild(userId) ||
                                child.child(DATA_SWIPES_RIGHT).hasChild(userId) ||
                                child.child(DATA_MATCHES).hasChild(userId)) {
                            showUser = false
                        }
                        if (showUser) {
                            rowItems.add(user)
                            cardsAdapter?.notifyDataSetChanged()
                        }
                    }
                    progressLayout.visibility = View.GONE
                    if (rowItems.isEmpty()) {
                        noUsersLayout.visibility = View.VISIBLE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}