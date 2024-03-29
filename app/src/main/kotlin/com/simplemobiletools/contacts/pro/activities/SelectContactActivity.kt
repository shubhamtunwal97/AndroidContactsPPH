package com.simplemobiletools.contacts.pro.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CONTACTS
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.SelectContactsAdapter
import com.simplemobiletools.contacts.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.contacts.pro.dialogs.FilterContactSourcesDialog
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.getContactPublicUri
import com.simplemobiletools.contacts.pro.extensions.getVisibleContactSources
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.activity_select_contact.*

class SelectContactActivity : SimpleActivity() {
    private var specialMimeType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_contact)

        if (checkAppSideloading()) {
            return
        }

        setupPlaceholders()

        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    if (it) {
                        specialMimeType = when (intent.data) {
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI -> ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI -> ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            else -> null
                        }
                        initContacts()
                    } else {
                        toast(R.string.no_contacts_permission)
                        finish()
                    }
                }
            } else {
                toast(R.string.no_contacts_permission)
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_select_activity, menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.filter -> showFilterDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            initContacts()
        }
    }

    private fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
            initContacts()
        }
    }

    private fun initContacts() {
        ContactsHelper(this).getContacts {
            if (isDestroyed) {
                return@getContacts
            }

            var contacts = it.filter {
                if (specialMimeType != null) {
                    val hasRequiredValues = when (specialMimeType) {
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> it.emails.isNotEmpty()
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> it.phoneNumbers.isNotEmpty()
                        else -> true
                    }
                    !it.isPrivate() && hasRequiredValues
                } else {
                    true
                }
            } as ArrayList<Contact>

            val contactSources = getVisibleContactSources()
            contacts = contacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>

            runOnUiThread {
                updatePlaceholderVisibility(contacts)
                SelectContactsAdapter(this, contacts, ArrayList(), false, select_contact_list) {
                    confirmSelection(it)
                }.apply {
                    select_contact_list.adapter = this
                }

                select_contact_fastscroller.allowBubbleDisplay = baseConfig.showInfoBubble
                select_contact_fastscroller.setViews(select_contact_list) {
                    select_contact_fastscroller.updateBubbleText(contacts[it].getBubbleText())
                }
            }
        }
    }

    private fun confirmSelection(contact: Contact) {
        Intent().apply {
            data = getResultUri(contact)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, this)
        }
        finish()
    }

    private fun getResultUri(contact: Contact): Uri {
        return when {
            specialMimeType != null -> {
                val contactId = ContactsHelper(this).getContactMimeTypeId(contact.id.toString(), specialMimeType!!)
                Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, contactId)
            }
            else -> getContactPublicUri(contact)
        }
    }

    private fun setupPlaceholders() {
        select_contact_placeholder.setTextColor(config.textColor)
        select_contact_placeholder_2.setTextColor(getAdjustedPrimaryColor())
        select_contact_placeholder_2.underlineText()
        select_contact_placeholder_2.setOnClickListener {
            FilterContactSourcesDialog(this) {
                initContacts()
            }
        }
    }

    private fun updatePlaceholderVisibility(contacts: ArrayList<Contact>) {
        select_contact_list.beVisibleIf(contacts.isNotEmpty())
        select_contact_placeholder_2.beVisibleIf(contacts.isEmpty())
        select_contact_placeholder.beVisibleIf(contacts.isEmpty())
        select_contact_placeholder.setText(when (specialMimeType) {
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> R.string.no_contacts_with_emails
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> R.string.no_contacts_with_phone_numbers
            else -> R.string.no_contacts_found
        })
    }
}
