/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.groebl.sms.feature.main

import android.Manifest
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import org.groebl.sms.R
import org.groebl.sms.common.Navigator
import org.groebl.sms.common.androidxcompat.drawerOpen
import org.groebl.sms.common.base.QkThemedActivity
import org.groebl.sms.common.util.extensions.autoScrollToStart
import org.groebl.sms.common.util.extensions.dismissKeyboard
import org.groebl.sms.common.util.extensions.resolveThemeColor
import org.groebl.sms.common.util.extensions.scrapViews
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.common.util.extensions.setTint
import org.groebl.sms.common.util.extensions.setVisible
import org.groebl.sms.feature.blocking.BlockingDialog
import org.groebl.sms.feature.conversations.ConversationItemTouchCallback
import org.groebl.sms.feature.conversations.ConversationsAdapter
import org.groebl.sms.repository.SyncRepository
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import dagger.android.AndroidInjection
import org.groebl.sms.common.widget.TextInputDialog
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.drawer_view.*
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.main_permission_hint.*
import kotlinx.android.synthetic.main.main_syncing.*
import org.groebl.sms.common.util.BluetoothHelper
import javax.inject.Inject

class MainActivity : QkThemedActivity(), MainView {

    @Inject lateinit var blockingDialog: BlockingDialog
    @Inject lateinit var disposables: CompositeDisposable
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var conversationsAdapter: ConversationsAdapter
    @Inject lateinit var searchAdapter: SearchAdapter
    @Inject lateinit var itemTouchCallback: ConversationItemTouchCallback
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    override val onNewIntentIntent: Subject<Intent> = PublishSubject.create()
    override val activityResumedIntent: Subject<Boolean> = PublishSubject.create()
    override val queryChangedIntent by lazy { toolbarSearch.textChanges() }
    override val composeIntent by lazy { compose.clicks() }
    override val drawerToggledIntent: Observable<Boolean> by lazy {
        drawerLayout
                .drawerOpen(Gravity.START)
                .doOnNext { dismissKeyboard() }
    }
    override val homeIntent: Subject<Unit> = PublishSubject.create()
    override val navigationIntent: Observable<NavItem> by lazy {
        Observable.merge(listOf(
                backPressedSubject,
                inbox.clicks().map { NavItem.INBOX },
                archived.clicks().map { NavItem.ARCHIVED },
                backup.clicks().map { NavItem.BACKUP },
                scheduled.clicks().map { NavItem.SCHEDULED },
                blocking.clicks().map { NavItem.BLOCKING },
                settings.clicks().map { NavItem.SETTINGS },
                settings_bluetooth.clicks().map { NavItem.SETTINGS_BLUETOOTH },
                help.clicks().map { NavItem.HELP }))
    }
    override val optionsItemIntent: Subject<Int> = PublishSubject.create()
    override val dismissRatingIntent by lazy { rateDismiss.clicks() }
    override val rateIntent by lazy { rateOkay.clicks() }
    override val rateDonateIntent by lazy { rateDonate.clicks() }
    override val conversationsSelectedIntent by lazy { conversationsAdapter.selectionChanges }
    override val confirmDeleteIntent: Subject<List<Long>> = PublishSubject.create()
    override val renameConversationIntent: Subject<String> = PublishSubject.create()
    override val swipeConversationIntent by lazy { itemTouchCallback.swipes }
    override val undoArchiveIntent: Subject<Unit> = PublishSubject.create()
    override val snackbarButtonIntent: Subject<Unit> = PublishSubject.create()

    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[MainViewModel::class.java]
    }
    private val toggle by lazy {
        ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.main_drawer_open_cd,
            0
        )
    }
    private val itemTouchHelper by lazy { ItemTouchHelper(itemTouchCallback) }
    private val progressAnimator by lazy {
        ObjectAnimator.ofInt(syncingProgress, "progress", 0, 0)
    }
    private val snackbar by lazy { findViewById<View>(R.id.snackbar) }
    private val syncing by lazy { findViewById<View>(R.id.syncing) }
    private val backPressedSubject: Subject<NavItem> = PublishSubject.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        viewModel.bindView(this)
        onNewIntentIntent.onNext(intent)

        (snackbar as? ViewStub)?.setOnInflateListener { _, _ ->
            snackbarButton.clicks()
                    .autoDisposable(scope(Lifecycle.Event.ON_DESTROY))
                    .subscribe(snackbarButtonIntent)
        }

        (syncing as? ViewStub)?.setOnInflateListener { _, _ ->
            syncingProgress?.let {
                it.progressTintList = ColorStateList.valueOf(theme.blockingFirst().theme)
                it.indeterminateTintList = ColorStateList.valueOf(theme.blockingFirst().theme)
            }
        }

        toggle.syncState()
        toolbar.setNavigationOnClickListener {
            dismissKeyboard()
            homeIntent.onNext(Unit)
        }

        itemTouchCallback.adapter = conversationsAdapter
        conversationsAdapter.autoScrollToStart(recyclerView)

        // Don't allow clicks to pass through the drawer layout
        drawer.clicks().autoDisposable(scope()).subscribe()

        // Set the theme color tint to the recyclerView, progressbar, and FAB
        theme
                .autoDisposable(scope())
                .subscribe { theme ->
                    // Set the color for the drawer icons
                    val states = arrayOf(
                            intArrayOf(android.R.attr.state_activated),
                            intArrayOf(-android.R.attr.state_activated))
                            ColorStateList(states, intArrayOf(theme.theme,
                                resolveThemeColor(android.R.attr.textColorSecondary)
                            ))
                            .let { tintList ->
                                inboxIcon.imageTintList = tintList
                                archivedIcon.imageTintList = tintList
                            }

                    // Miscellaneous views
                    syncingProgress?.progressTintList = ColorStateList.valueOf(theme.theme)
                    syncingProgress?.indeterminateTintList = ColorStateList.valueOf(theme.theme)
                    rateIcon.setTint(theme.theme)
                    compose.setBackgroundTint(theme.theme)

                    // Set the FAB compose icon color
                    compose.setTint(theme.textPrimary)
                }

        //Delete BT-Messages
        Thread { BluetoothHelper.deleteBluetoothMessages(this, prefs.bluetooth_realm_hide_message.get(), 6) }.start()


        if (prefs.bluetooth_enabled.get()) {
            val infoMsg = StringBuilder()
            val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

            if (BluetoothHelper.hasNotificationAccess(this) && !BluetoothHelper.isNotificationServiceRunning(this)) {
                infoMsg.append("- " + this.getString(R.string.bluetooth_alert_info_notifications) + "\n")
                BluetoothHelper.checkAndRestartNotificationListener(this)
            }
            if (prefs.bluetooth_only_on_connect.get() && !BluetoothHelper.hasBluetoothPermission(this)) {
                infoMsg.append("- " + this.getString(R.string.bluetooth_alert_info_bt_permission) + "\n")
            }
            if (prefs.bluetooth_only_on_connect.get() && prefs.bluetooth_devices.get().isEmpty()) {
                infoMsg.append("- " + this.getString(R.string.bluetooth_alert_info_device) + "\n")
            }
            if (prefs.bluetooth_apps.get().isEmpty()) {
                infoMsg.append("- " + this.getString(R.string.bluetooth_alert_info_apps) + "\n")
            }
            if(prefs.bluetooth_only_on_connect.get() && !bluetoothManager.adapter.isEnabled) {
                infoMsg.append("- " + this.getString(R.string.bluetooth_alert_info_disabled) + "\n")
            }

            if (infoMsg.isNotEmpty()) {

                AlertDialog.Builder(this)
                    .setTitle("Information")
                    .setMessage(infoMsg.toString().trim())
                    .setPositiveButton(R.string.bluetooth_alert_button_ok, null)
                    .show()

            }
        }

    }

    override fun onNewIntent(intent: Intent?) =
        intent?.let {
            super.onNewIntent(intent)
            it.run(onNewIntentIntent::onNext)
        } ?: Unit

    override fun render(state: MainState) {
        if (state.hasError) {
            finish()
            return
        }

        val addContact = when (state.page) {
            is Inbox -> state.page.addContact
            is Archived -> state.page.addContact
            else -> false
        }

        val markPinned = when (state.page) {
            is Inbox -> state.page.markPinned
            is Archived -> state.page.markPinned
            else -> true
        }

        val markRead = when (state.page) {
            is Inbox -> state.page.markRead
            is Archived -> state.page.markRead
            else -> true
        }

        val selectedConversations = when (state.page) {
            is Inbox -> state.page.selected
            is Archived -> state.page.selected
            else -> 0
        }

        toolbarSearch.setVisible(state.page is Inbox &&
                state.page.selected == 0 ||
                state.page is Searching
        )
        toolbarTitle.setVisible(toolbarSearch.visibility != View.VISIBLE)

        toolbar.menu.apply {
            findItem(R.id.select_all)?.isVisible =
                (conversationsAdapter.itemCount > 1) && selectedConversations != 0
            findItem(R.id.archive)?.isVisible =
                state.page is Inbox && selectedConversations != 0
            findItem(R.id.unarchive)?.isVisible =
                state.page is Archived && selectedConversations != 0
            findItem(R.id.delete)?.isVisible = selectedConversations != 0
            //findItem(R.id.add)?.isVisible = addContact && selectedConversations != 0
            findItem(R.id.pin)?.isVisible = markPinned && selectedConversations != 0
            findItem(R.id.unpin)?.isVisible = !markPinned && selectedConversations != 0
            findItem(R.id.read)?.isVisible = markRead && selectedConversations != 0
            findItem(R.id.unread)?.isVisible = !markRead && selectedConversations != 0
            findItem(R.id.block)?.isVisible = selectedConversations != 0
            findItem(R.id.rename)?.isVisible = selectedConversations == 1
        }

        rateLayout.setVisible(state.showRating)

        compose.setVisible((state.page is Inbox || state.page is Archived) && BluetoothHelper.isDefaultSms(applicationContext))
        conversationsAdapter.emptyView = empty.takeIf {
            state.page is Inbox || state.page is Archived
        }
        searchAdapter.emptyView = empty.takeIf { state.page is Searching }

        when (state.page) {
            is Inbox -> {
                showBackButton(state.page.selected > 0)
                title = getString(R.string.main_title_selected, state.page.selected)
                if (recyclerView.adapter !== conversationsAdapter)
                    recyclerView.adapter = conversationsAdapter
                //Do not show messages when not default sms handler
                if (BluetoothHelper.isDefaultSms(applicationContext)) {
                    conversationsAdapter.updateData(state.page.data)
                    itemTouchHelper.attachToRecyclerView(recyclerView)
                    empty.setText(R.string.inbox_empty_text)
                    toolbarSearch.isEnabled = true
                } else {
                    itemTouchHelper.attachToRecyclerView(null)
                    empty.setVisible(true)
                    empty.setText(R.string.main_default_sms_mainscreen)
                    toolbarSearch.isEnabled = false
                }
            }

            is Searching -> {
                showBackButton(true)
                if (recyclerView.adapter !== searchAdapter) recyclerView.adapter = searchAdapter
                searchAdapter.data = state.page.data ?: listOf()
                itemTouchHelper.attachToRecyclerView(null)
                empty.setText(R.string.inbox_search_empty_text)
            }

            is Archived -> {
                showBackButton(state.page.selected > 0)
                title = when (state.page.selected != 0) {
                    true -> getString(R.string.main_title_selected, state.page.selected)
                    false -> getString(R.string.title_archived)
                }
                if (recyclerView.adapter !== conversationsAdapter)
                    recyclerView.adapter = conversationsAdapter
                conversationsAdapter.updateData(state.page.data)
                itemTouchHelper.attachToRecyclerView(null)
                empty.setText(R.string.archived_empty_text)
            }

            else -> {}
        }

        inbox.isActivated = state.page is Inbox
        archived.isActivated = state.page is Archived

        if (drawerLayout.isDrawerOpen(GravityCompat.START) && !state.drawerOpen)
            drawerLayout.closeDrawer(GravityCompat.START)
        else if (!drawerLayout.isDrawerVisible(GravityCompat.START) && state.drawerOpen)
            drawerLayout.openDrawer(GravityCompat.START)

        when (state.syncing) {
            is SyncRepository.SyncProgress.Idle -> {
                syncing.isVisible = false
                snackbar.isVisible = (!state.defaultSms ||
                        !state.smsPermission ||
                        !state.contactPermission ||
                        !state.notificationPermission)
            }

            is SyncRepository.SyncProgress.Running -> {
                syncing.isVisible = true
                syncingProgress.max = state.syncing.max
                progressAnimator.apply {
                    setIntValues(syncingProgress.progress, state.syncing.progress)
                }.start()
                syncingProgress.isIndeterminate = state.syncing.indeterminate
                snackbar.isVisible = false
            }
        }

        when {
            !state.defaultSms -> {
                snackbarTitle?.setText(R.string.main_default_sms_title)
                snackbarMessage?.setText(R.string.main_default_sms_message_new)
                snackbarButton?.setText(R.string.main_default_sms_change)
            }

            !state.smsPermission -> {
                snackbarTitle?.setText(R.string.main_permission_required)
                snackbarMessage?.setText(R.string.main_permission_sms_new)
                snackbarButton?.setText(R.string.main_permission_allow)
            }

            !state.contactPermission -> {
                snackbarTitle?.setText(R.string.main_permission_required)
                snackbarMessage?.setText(R.string.main_permission_contacts_new)
                snackbarButton?.setText(R.string.main_permission_allow)
            }

            !state.notificationPermission -> {
                snackbarTitle?.setText(R.string.main_permission_required)
                snackbarMessage?.setText(R.string.main_permission_notifications_new)
                snackbarButton?.setText(R.string.main_permission_allow)
            }
        }
    }

    override fun onResume() =
        super.onResume().also { activityResumedIntent.onNext(true) }

    override fun onPause() =
        super.onPause().also { activityResumedIntent.onNext(false) }

    override fun onDestroy() =
        super.onDestroy().also { disposables.dispose() }

    override fun showBackButton(show: Boolean) =
        toggle.let {
            it.onDrawerSlide(drawer, if (show) 1f else 0f)
            it.drawerArrowDrawable.color = when (show) {
                true -> resolveThemeColor(android.R.attr.textColorSecondary)
                false -> resolveThemeColor(android.R.attr.textColorPrimary)
            }
        }

    override fun requestDefaultSms() =
        navigator.showDefaultSmsDialog(this)

    override fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            permissions += Manifest.permission.POST_NOTIFICATIONS

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 0)
    }

    override fun clearSearch() {
        dismissKeyboard()
        toolbarSearch.text = null
    }

    override fun clearSelection() = conversationsAdapter.clearSelection()

    override fun toggleSelectAll() = conversationsAdapter.toggleSelectAll()

    override fun themeChanged() = recyclerView.scrapViews()

    override fun showBlockingDialog(conversations: List<Long>, block: Boolean) {
        blockingDialog.show(this, conversations, block)
    }

    override fun showDeleteDialog(conversations: List<Long>) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(
                resources.getQuantityString(
                    R.plurals.dialog_delete_message,
                    conversations.size,
                    conversations.size
                )
            )
            .setPositiveButton(R.string.button_delete) { _, _ -> confirmDeleteIntent.onNext(conversations) }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun showRenameDialog(conversationName: String) =
        TextInputDialog(
            this,
            getString(R.string.info_name),
            renameConversationIntent::onNext
        )
            .setText(conversationName)
            .show()

    override fun showArchivedSnackbar(countConversationsArchived: Int) =
        Snackbar.make(
            drawerLayout,
            resources.getQuantityString(R.plurals.toast_archived, countConversationsArchived, countConversationsArchived),
            if (countConversationsArchived < 10) Snackbar.LENGTH_LONG
            else Snackbar.LENGTH_INDEFINITE
        ).let {
            it.setAction(R.string.button_undo) { undoArchiveIntent.onNext(Unit) }
            it.setActionTextColor(colors.theme().theme)
            it.show()
        }

    override fun onCreateOptionsMenu(menu: Menu?) =
        menu?.let {
            menuInflater.inflate(R.menu.main, it)
            super.onCreateOptionsMenu(it)
        } ?: false

    override fun onOptionsItemSelected(item: MenuItem) =
        optionsItemIntent.onNext(item.itemId).let { true }

    override fun onBackPressed() = backPressedSubject.onNext(NavItem.BACK)

    override fun drawerToggled(opened: Boolean) {
        if (opened) {
            dismissKeyboard()
            if (!inbox.isInTouchMode)
                inbox.requestFocus()
        } else
            toolbarSearch.requestFocus()
    }
}
