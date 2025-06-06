package org.groebl.sms.feature.conversationinfo

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding2.view.clicks
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.base.QkAdapter
import org.groebl.sms.common.base.QkViewHolder
import org.groebl.sms.common.util.Colors
import org.groebl.sms.common.util.extensions.setVisible
import org.groebl.sms.extensions.isVideo
import org.groebl.sms.feature.conversationinfo.ConversationInfoItem.*
import org.groebl.sms.util.Preferences
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.conversation_info_settings.*
import kotlinx.android.synthetic.main.conversation_media_list_item.*
import kotlinx.android.synthetic.main.conversation_recipient_list_item.*
import org.groebl.sms.common.util.extensions.setTint
import javax.inject.Inject

class ConversationInfoAdapter @Inject constructor(
    private val context: Context,
    private val navigator: Navigator,
    private val colors: Colors,
    private val prefs: Preferences
) : QkAdapter<ConversationInfoItem, QkViewHolder>() {

    val recipientClicks: Subject<Long> = PublishSubject.create()
    val recipientLongClicks: Subject<Long> = PublishSubject.create()
    val themeClicks: Subject<Long> = PublishSubject.create()
    val nameClicks: Subject<Unit> = PublishSubject.create()
    val notificationClicks: Subject<Unit> = PublishSubject.create()
    val archiveClicks: Subject<Unit> = PublishSubject.create()
    val blockClicks: Subject<Unit> = PublishSubject.create()
    val deleteClicks: Subject<Unit> = PublishSubject.create()
    val mediaClicks: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> QkViewHolder(inflater.inflate(R.layout.conversation_recipient_list_item, parent, false)).apply {
                itemView.setOnClickListener {
                    val item = getItem(adapterPosition) as? ConversationInfoRecipient
                    item?.value?.id?.run(recipientClicks::onNext)
                }

                itemView.setOnLongClickListener {
                    val item = getItem(adapterPosition) as? ConversationInfoRecipient
                    item?.value?.id?.run(recipientLongClicks::onNext)
                    true
                }

                theme.setOnClickListener {
                    val item = getItem(adapterPosition) as? ConversationInfoRecipient
                    item?.value?.id?.run(themeClicks::onNext)
                }
            }

            1 -> QkViewHolder(inflater.inflate(R.layout.conversation_info_settings, parent, false)).apply {
                groupName.clicks().subscribe(nameClicks)
                notifications.clicks().subscribe(notificationClicks)
                archive.clicks().subscribe(archiveClicks)
                block.clicks().subscribe(blockClicks)
                delete.clicks().subscribe(deleteClicks)
            }

            2 -> QkViewHolder(inflater.inflate(R.layout.conversation_media_list_item, parent, false)).apply {
                itemView.setOnClickListener {
                    val item = getItem(adapterPosition) as? ConversationInfoMedia
                    item?.value?.id?.run(mediaClicks::onNext)
                }
            }

            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ConversationInfoRecipient -> {
                val recipient = item.value
                holder.avatar.setRecipient(recipient)

                holder.name.text = recipient.contact?.name ?: recipient.address

                holder.address.text = recipient.address
                holder.address.setVisible(recipient.contact != null)

                holder.add.setVisible(recipient.contact == null)

                val theme = colors.theme(recipient)
                holder.theme.setTint(theme.theme)
            }

            is ConversationInfoSettings -> {
                holder.groupName.summary = item.name

                holder.notifications.isEnabled = !item.blocked

                holder.archive.isEnabled = !item.blocked
                holder.archive.title = context.getString(when (item.archived) {
                    true -> R.string.info_unarchive
                    false -> R.string.info_archive
                })

                holder.block.title = context.getString(when (item.blocked) {
                    true -> R.string.info_unblock
                    false -> R.string.info_block
                })
            }

            is ConversationInfoMedia -> {
                val part = item.value

                Glide.with(context)
                    .load(part.getUri())
                    .fitCenter()
                    .into(holder.thumbnail)

                holder.video.isVisible = part.isVideo()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is ConversationInfoRecipient -> 0
            is ConversationInfoSettings -> 1
            is ConversationInfoMedia -> 2
        }
    }

    override fun areItemsTheSame(old: ConversationInfoItem, new: ConversationInfoItem): Boolean {
        return when {
            old is ConversationInfoRecipient && new is ConversationInfoRecipient -> {
                old.value.id == new.value.id
            }

            old is ConversationInfoSettings && new is ConversationInfoSettings -> {
                true
            }

            old is ConversationInfoMedia && new is ConversationInfoMedia -> {
                old.value.id == new.value.id
            }

            else -> false
        }
    }

    fun Drawable.applyColorFilter(color: Int) = mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN)

}
