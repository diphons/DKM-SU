package com.diphons.dkmsu.ui.screen

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.diphons.dkmsu.ui.util.IconResource
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.generated.destinations.KernelScreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import com.diphons.dkmsu.R

enum class BottomBarDestination(
    val direction: DirectionDestinationSpec,
    @StringRes val label: Int,
    val iconSelected: IconResource,
    val iconNotSelected: IconResource,
    val rootRequired: Boolean,
) {
    Home(HomeScreenDestination, R.string.home, IconResource.fromImageVector(Icons.Filled.Home), IconResource.fromImageVector(Icons.Outlined.Home), false),
    SuperUser(SuperUserScreenDestination, R.string.superuser, IconResource.fromImageVector(Icons.Filled.AdminPanelSettings), IconResource.fromImageVector(Icons.Outlined.AdminPanelSettings), true),
    Module(ModuleScreenDestination, R.string.module, IconResource.fromImageVector(Icons.Filled.Layers), IconResource.fromImageVector(Icons.Outlined.Layers), true),
    Kernel(KernelScreenDestination, R.string.home_kernel, IconResource.fromImageVector(Icons.Filled.DeveloperMode), IconResource.fromImageVector(Icons.Outlined.DeveloperMode), true)
}