package com.edgepanel.app.panels

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.provider.ContactsContract
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edgepanel.app.adapter.ContactAdapter
import com.edgepanel.app.model.PeopleContact
import com.edgepanel.app.util.PrefsManager
import kotlinx.coroutines.*

class PeoplePanel(context: Context) : LinearLayout(context) {

    private val prefs = PrefsManager(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        orientation = VERTICAL
        val hasContacts = context.checkSelfPermission(
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContacts) {
            showPermissionPrompt()
        } else {
            setupList()
        }
    }

    private fun showPermissionPrompt() {
        val tv = TextView(context).apply {
            text    = "👥\n\nContacts permission required.\nOpen the app and grant access."

Contacts permission required.
Open the app and grant access."
            textSize = 14f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#757575"))
            setPadding(48, 80, 48, 80)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        addView(tv)
    }

    private fun setupList() {
        val progress = ProgressBar(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        addView(progress)

        val recycler = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            layoutParams  = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(recycler)

        scope.launch {
            val contacts = withContext(Dispatchers.IO) { loadContacts() }
            progress.visibility = GONE
            if (contacts.isEmpty()) {
                addView(TextView(context).apply {
                    text = "No pinned contacts.
Open the Contacts app to manage."
                    textSize = 13f; gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#9E9E9E"))
                    setPadding(48, 80, 48, 48)
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                })
            } else {
                recycler.adapter = ContactAdapter(
                    context, contacts,
                    onCall    = { c -> dialNumber(c.phone) },
                    onMessage = { c -> sendSms(c.phone) }
                )
            }
        }
    }

    private fun loadContacts(): List<PeopleContact> {
        val pinnedIds = prefs.getPinnedContactIds()
        val list = mutableListOf<PeopleContact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val proj = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        context.contentResolver.query(uri, proj, null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cur ->
            while (cur.moveToNext()) {
                val id    = cur.getString(0)
                val name  = cur.getString(1) ?: continue
                val phone = cur.getString(2)
                val photo = cur.getString(3)
                if (pinnedIds.isEmpty() || id in pinnedIds) {
                    list.add(PeopleContact(id = id, name = name, phone = phone, photoUri = photo))
                }
            }
        }
        return list.take(20)
    }

    private fun dialNumber(phone: String?) {
        phone ?: return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun sendSms(phone: String?) {
        phone ?: return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
