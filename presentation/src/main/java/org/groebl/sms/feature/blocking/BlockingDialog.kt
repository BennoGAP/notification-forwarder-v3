package org.groebl.sms.feature.blocking

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import org.groebl.sms.R
import org.groebl.sms.blocking.BlockingClient
import org.groebl.sms.interactor.MarkBlocked
import org.groebl.sms.interactor.MarkUnblocked
import org.groebl.sms.repository.ConversationRepository
import org.groebl.sms.util.Preferences
import javax.inject.Inject

// TODO: Once we have a custom dialog based on conductor, turn this into a controller
class BlockingDialog @Inject constructor(
    private val blockingManager: BlockingClient,
    private val context: Context,
    private val conversationRepo: ConversationRepository,
    private val prefs: Preferences,
    private val markBlocked: MarkBlocked,
    private val markUnblocked: MarkUnblocked
) {

    fun show(activity: Activity, conversationIds: List<Long>, block: Boolean) {
        val addresses = conversationIds.toLongArray()
                .let(conversationRepo::getConversations)
                .flatMap { conversation -> conversation.recipients }
                .map { it.address }
                .distinct()

        // If we can block/unblock in the external manager, then just fire that off and exit
        if (block) {
            markBlocked.execute(conversationIds)
            if (blockingManager.canBlock()) {
                blockingManager.block(addresses).subscribe()
                return
            }
        } else {
            markUnblocked.execute(conversationIds)
            if (blockingManager.canUnblock()) {
                blockingManager.unblock(addresses).subscribe()
                return
            }
        }

        val res = when (block) {
            true -> R.plurals.blocking_block_external
            false -> R.plurals.blocking_unblock_external
        }

        val manager = context.getString(when (prefs.blockingManager.get()) {
            Preferences.BLOCKING_MANAGER_SIA -> R.string.blocking_manager_sia_title
            Preferences.BLOCKING_MANAGER_CC -> R.string.blocking_manager_call_control_title
            else -> R.string.blocking_manager_android
        })

        val message = context.resources.getQuantityString(res, addresses.size, manager)

        // Otherwise, show a dialog asking the user if they want to be directed to the external
        // blocking manager
        AlertDialog.Builder(activity)
                .setTitle(R.string.main_menu_block)
                .setMessage(message)
                .setPositiveButton(R.string.button_yes) { _, _ ->
                    when (block) {
                        true -> blockingManager.block(addresses)
                        false -> blockingManager.unblock(addresses)
                    }.subscribe()
                }
                .setNegativeButton(R.string.button_cancel) { _, _ -> }
                .create()
                .show()
    }

}