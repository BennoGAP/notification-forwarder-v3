package org.groebl.sms.feature.bluetooth.device

import androidx.lifecycle.ViewModel
import org.groebl.sms.injection.ViewModelKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
class BluetoothDeviceActivityModule {

    @Provides
    @IntoMap
    @ViewModelKey(BluetoothDeviceViewModel::class)
    fun provideBluetoothDeviceViewModel(viewModel: BluetoothDeviceViewModel): ViewModel = viewModel

}