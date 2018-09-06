package org.groebl.sms.feature.bluetooth.app

import androidx.lifecycle.ViewModel
import org.groebl.sms.injection.ViewModelKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
class BluetoothAppActivityModule {

    @Provides
    @IntoMap
    @ViewModelKey(BluetoothAppViewModel::class)
    fun provideBluetoothAppViewModel(viewModel: BluetoothAppViewModel): ViewModel = viewModel
}