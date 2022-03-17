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
package org.groebl.sms.util

import android.content.Context
import android.net.Uri
import com.bumptech.glide.Glide
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun getScaledGif(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int, quality: Int = 90): ByteArray {
        val gif = Glide
                .with(context)
                .asGif()
                .load(uri)
                .centerInside()
                .encodeQuality(quality)
                .submit(maxWidth, maxHeight)
                .get()

        val outputStream = ByteArrayOutputStream()
        GifEncoder(context, Glide.get(context).bitmapPool).encodeTransformedToStream(gif, outputStream)
        return outputStream.toByteArray()
    }

    fun getScaledImage(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int, quality: Int = 90): ByteArray {
        return Glide
                .with(context)
                .`as`(ByteArray::class.java)
                .load(uri)
                .centerInside()
                .encodeQuality(quality)
                .submit(maxWidth, maxHeight)
                .get()
    }

}