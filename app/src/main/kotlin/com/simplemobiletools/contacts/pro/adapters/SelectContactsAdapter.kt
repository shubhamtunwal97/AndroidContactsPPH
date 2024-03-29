package com.simplemobiletools.contacts.pro.adapters

import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.helpers.Config
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.item_add_favorite_with_number.view.*
import java.util.*

class SelectContactsAdapter(val activity: SimpleActivity, val contacts: List<Contact>, private val selectedContacts: ArrayList<Contact>, private val allowPickMultiple: Boolean,
                            private val recyclerView: MyRecyclerView, private val itemClick: ((Contact) -> Unit)? = null) : RecyclerView.Adapter<SelectContactsAdapter.ViewHolder>() {
    private val itemViews = SparseArray<View>()
    private val selectedPositions = HashSet<Int>()
    private val config = activity.config
    private val textColor = config.textColor
    private val contactDrawable = activity.resources.getColoredDrawableWithColor(R.drawable.ic_person_vector, textColor)
    private val showContactThumbnails = config.showContactThumbnails
    private val itemLayout = if (config.showPhoneNumbers) R.layout.item_add_favorite_with_number else R.layout.item_add_favorite_without_number

    private var smallPadding = activity.resources.getDimension(R.dimen.small_margin).toInt()
    private var bigPadding = activity.resources.getDimension(R.dimen.normal_margin).toInt()

    init {
        contacts.forEachIndexed { index, contact ->
            if (selectedContacts.asSequence().map { it.id }.contains(contact.id)) {
                selectedPositions.add(index)
            }
        }

        if (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }

        DividerItemDecoration(activity, DividerItemDecoration.VERTICAL).apply {
            setDrawable(activity.resources.getDrawable(R.drawable.divider))
            recyclerView.addItemDecoration(this)
        }
    }

    private fun toggleItemSelection(select: Boolean, pos: Int) {
        if (select) {
            if (itemViews[pos] != null) {
                selectedPositions.add(pos)
            }
        } else {
            selectedPositions.remove(pos)
        }

        itemViews[pos]?.contact_checkbox?.isChecked = select
    }

    fun getSelectedItemsSet(): HashSet<Contact> {
        val selectedItemsSet = HashSet<Contact>(selectedPositions.size)
        selectedPositions.forEach { selectedItemsSet.add(contacts[it]) }
        return selectedItemsSet
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(itemLayout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = contacts[position]
        itemViews.put(position, holder.bindView(eventType, contactDrawable, config, showContactThumbnails, smallPadding, bigPadding))
        toggleItemSelection(selectedPositions.contains(position), position)
    }

    override fun getItemCount() = contacts.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(contact: Contact, contactDrawable: Drawable, config: Config, showContactThumbnails: Boolean,
                     smallPadding: Int, bigPadding: Int): View {
            itemView.apply {
                contact_checkbox.beVisibleIf(allowPickMultiple)
                contact_checkbox.setColors(config.textColor, context.getAdjustedPrimaryColor(), config.backgroundColor)
                val textColor = config.textColor

                contact_name.text = contact.getNameToDisplay()
                contact_name.setTextColor(textColor)
                contact_name.setPadding(if (showContactThumbnails) smallPadding else bigPadding, smallPadding, smallPadding, 0)

                contact_number?.text = contact.phoneNumbers.firstOrNull()?.value ?: ""
                contact_number?.setTextColor(textColor)
                contact_number?.setPadding(if (showContactThumbnails) smallPadding else bigPadding, 0, smallPadding, 0)

                contact_frame.setOnClickListener {
                    if (itemClick != null) {
                        itemClick.invoke(contact)
                    } else {
                        viewClicked(!contact_checkbox.isChecked)
                    }
                }

                contact_tmb.beVisibleIf(showContactThumbnails)
                if (showContactThumbnails) {
                    if (contact.photoUri.isNotEmpty()) {
                        val options = RequestOptions()
                                .signature(ObjectKey(contact.photoUri))
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .error(contactDrawable)
                                .centerCrop()

                        if (activity.isDestroyed) {
                            Glide.with(activity).load(contact.photoUri).transition(DrawableTransitionOptions.withCrossFade()).apply(options).into(contact_tmb)
                        }
                    } else {
                        contact_tmb.setImageDrawable(contactDrawable)
                    }
                }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean) {
            toggleItemSelection(select, adapterPosition)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed) {
            Glide.with(activity).clear(holder.itemView.contact_tmb)
        }
    }
}
