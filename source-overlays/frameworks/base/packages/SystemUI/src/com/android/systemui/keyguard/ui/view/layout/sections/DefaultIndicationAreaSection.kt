/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.android.systemui.keyguard.ui.view.layout.sections

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.android.systemui.keyguard.shared.model.KeyguardSection
import com.android.systemui.keyguard.ui.binder.KeyguardIndicationAreaBinder
import com.android.systemui.keyguard.ui.view.KeyguardIndicationArea
import com.android.systemui.keyguard.ui.viewmodel.DeviceEntryIconViewModel
import com.android.systemui.keyguard.ui.viewmodel.KeyguardIndicationAreaViewModel
import com.android.systemui.res.R
import com.android.systemui.shade.ShadeDisplayAware
import com.android.systemui.statusbar.KeyguardIndicationController
import dagger.Lazy
import javax.inject.Inject
import kotlinx.coroutines.DisposableHandle

class DefaultIndicationAreaSection
@Inject
constructor(
    @ShadeDisplayAware private val context: Context,
    private val keyguardIndicationAreaViewModel: KeyguardIndicationAreaViewModel,
    private val indicationController: KeyguardIndicationController,
    private val windowManager: WindowManager,
    private val deviceEntryIconViewModel: Lazy<DeviceEntryIconViewModel>,
) : KeyguardSection() {
    private val indicationAreaViewId = R.id.keyguard_indication_area
    private var indicationAreaHandle: DisposableHandle? = null

    override fun addViews(constraintLayout: ConstraintLayout) {
        val view = KeyguardIndicationArea(context, null)
        constraintLayout.addView(view)
    }

    override fun bindData(constraintLayout: ConstraintLayout) {
        indicationAreaHandle =
            KeyguardIndicationAreaBinder.bind(
                constraintLayout.requireViewById(R.id.keyguard_indication_area),
                keyguardIndicationAreaViewModel,
                indicationController,
            )
    }

    override fun applyConstraints(constraintSet: ConstraintSet) {
        constraintSet.apply {
            constrainWidth(indicationAreaViewId, ViewGroup.LayoutParams.MATCH_PARENT)
            constrainHeight(indicationAreaViewId, ViewGroup.LayoutParams.WRAP_CONTENT)
            placeVertically(this)
            connect(
                indicationAreaViewId,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START
            )
            connect(
                indicationAreaViewId,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
            )
        }
    }

    private fun placeVertically(constraintSet: ConstraintSet) {
        val marginBottom =
            context.resources.getDimensionPixelSize(R.dimen.keyguard_indication_margin_bottom)
        val sensor =
            if (deviceEntryIconViewModel.get().isUdfpsSupported.value) {
                deviceEntryIconViewModel.get().udfpsLocation.value
            } else {
                null
            }
        if (sensor == null) {
            constraintSet.connect(
                indicationAreaViewId,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                marginBottom,
            )
            return
        }

        val metrics = windowManager.currentWindowMetrics
        val screenHeight = metrics.bounds.bottom
        val navBarHeight =
            metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom
        val gap =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    4f,
                    context.resources.displayMetrics,
                )
                .toInt()
        val areaHeight =
            context.resources.getDimensionPixelSize(R.dimen.keyguard_indication_text_min_height)
        val sensorTop = (sensor.centerY - sensor.radius).toInt()
        val sensorBottom = (sensor.centerY + sensor.radius).toInt()
        val defaultBottom = screenHeight - marginBottom

        when {
            defaultBottom <= sensorTop || defaultBottom - areaHeight >= sensorBottom ->
                constraintSet.connect(
                    indicationAreaViewId,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    marginBottom,
                )
            sensorBottom + gap + areaHeight <= screenHeight - navBarHeight ->
                constraintSet.connect(
                    indicationAreaViewId,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                    sensorBottom + gap,
                )
            else ->
                constraintSet.connect(
                    indicationAreaViewId,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    screenHeight - sensorTop + gap,
                )
        }
    }

    override fun removeViews(constraintLayout: ConstraintLayout) {
        indicationAreaHandle?.dispose()
        constraintLayout.removeView(indicationAreaViewId)
    }
}
