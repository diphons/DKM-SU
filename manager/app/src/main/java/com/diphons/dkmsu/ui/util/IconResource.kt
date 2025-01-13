package com.diphons.dkmsu.ui.util

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

class IconResource private constructor(
    @DrawableRes private val resID: Int?,
    private val imageVector: ImageVector?,
    val contentDescription: String
) {

    @Composable
    fun asPainterResource(): Painter {
        resID?.let {
            return painterResource(id = resID)
        }
        return rememberVectorPainter(image = imageVector!!)
    }

    @Composable
    fun ComposeIcon(modifier: Modifier = Modifier) {
        Icon(
            modifier = modifier,
            painter = asPainterResource(),
            contentDescription = contentDescription
        )
    }

    companion object {
        @JvmStatic
        @SuppressLint
        fun fromDrawableResource(
            @DrawableRes resID: Int,
            contentDescription: String = ""
        ): IconResource {
            return IconResource(resID, null, contentDescription)
        }

        @JvmStatic
        fun fromImageVector(
            imageVector: ImageVector?,
            contentDescription: String = ""
        ): IconResource {
            return IconResource(null, imageVector, contentDescription)
        }
    }
}
