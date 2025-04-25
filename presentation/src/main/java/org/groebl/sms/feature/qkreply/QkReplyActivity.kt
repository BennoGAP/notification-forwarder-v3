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
package org.groebl.sms.feature.qkreply

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import org.groebl.sms.R
import org.groebl.sms.common.base.QkThemedActivity
import org.groebl.sms.common.util.extensions.autoScrollToStart
import org.groebl.sms.common.util.extensions.setVisible
import org.groebl.sms.feature.compose.MessagesAdapter
import dagger.android.AndroidInjection
import org.groebl.sms.common.util.extensions.showKeyboard
import org.groebl.sms.common.widget.QkEditText
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.compose_activity.message
import kotlinx.android.synthetic.main.qkreply_activity.*
import kotlinx.android.synthetic.main.qkreply_activity.sim
import kotlinx.android.synthetic.main.qkreply_activity.simIndex
import org.groebl.sms.common.util.extensions.resolveThemeColor
import org.groebl.sms.common.util.extensions.setBackgroundTint
import org.groebl.sms.common.util.extensions.setTint
import javax.inject.Inject

class QkReplyActivity : QkThemedActivity(), QkReplyView {

    @Inject lateinit var adapter: MessagesAdapter
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    override val menuItemIntent: Subject<Int> = PublishSubject.create()
    override val textChangedIntent by lazy { message.textChanges() }
    override val changeSimIntent by lazy { sim.clicks() }
    override val sendIntent by lazy { send.clicks() }

    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory)[QkReplyViewModel::class.java] }

    private val speechResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK)
            return@registerForActivityResult

        // check returned results are good
        val match = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if ((match === null) || (match.size < 1) || (match[0].isNullOrEmpty()))
            return@registerForActivityResult

        // get the edit text view
        val message = findViewById<QkEditText>(R.id.message)
        if (message === null)
            return@registerForActivityResult

        // populate message box with data returned by STT, set cursor to end, and focus
        message.setText(match[0])
        message.setSelection(message.text?.length ?: 0)
        message.requestFocus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        setFinishOnTouchOutside(prefs.qkreplyTapDismiss.get())
        setContentView(R.layout.qkreply_activity)
        window.setBackgroundDrawable(null)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        viewModel.bindView(this)

        theme
            .doOnNext { send.setBackgroundTint(it.theme) }
            .doOnNext { send.setTint(resolveThemeColor(android.R.attr.windowBackground)) }
            .autoDisposable(scope())
            .subscribe()

        toolbar.clipToOutline = true

        messages.adapter = adapter
        messages.adapter?.autoScrollToStart(messages)
        messages.adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = messages.scrollToPosition(adapter.itemCount - 1)
        })

        message.setOnTouchListener(object : OnTouchListener {
            private val gestureDetector =
                GestureDetector(this@QkReplyActivity, object : SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            // include if want a custom message that the STT can (optionally) display   .putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message")
                        speechResultLauncher.launch(speechRecognizerIntent)
                        return true
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        message.showKeyboard()
                        return true
                    }

                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        return true     // don't show soft keyboard on this event
                    }
                })

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                return gestureDetector.onTouchEvent(e)
            }
        })
    }

    override fun render(state: QkReplyState) {
        if (state.hasError) {
            finish()
        }

        threadId.onNext(state.threadId)

        title = state.title

        toolbar.menu.findItem(R.id.expand)?.isVisible = !state.expanded
        toolbar.menu.findItem(R.id.collapse)?.isVisible = state.expanded

        adapter.data = state.data

        counter.text = state.remaining
        counter.setVisible(counter.text.isNotBlank())

        sim.setVisible(state.subscription != null)
        sim.contentDescription = getString(R.string.compose_sim_cd, state.subscription?.displayName)
        simIndex.text = "${state.subscription?.simSlotIndex?.plus(1)}"

        val simColor = when (state.subscription?.simSlotIndex?.plus(1)?.toString()) {
            "1" -> colors.colorForSim(this, 1)
            "2" -> colors.colorForSim(this, 2)
            "3" -> colors.colorForSim(this, 3)
            else -> colors.colorForSim(this, 1)
        }
        if (prefs.simColor.get()) {
            sim.setTint(simColor)
        }

        send.isEnabled = state.canSend
        send.imageAlpha = if (state.canSend) 255 else 128
    }

    override fun setDraft(draft: String) {
        message.setText(draft)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.qkreply, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        menuItemIntent.onNext(item.itemId)
        return true
    }

    override fun getActivityThemeRes(black: Boolean) = when {
        black -> R.style.AppThemeDialog_Black
        else -> R.style.AppThemeDialog
    }

}