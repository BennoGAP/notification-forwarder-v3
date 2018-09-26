package org.groebl.sms.feature.bluetooth.donate

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import org.groebl.sms.injection.ViewModelKey

@Module
class BluetoothDonateActivityModule {

    @Provides
    @IntoMap
    @ViewModelKey(BluetoothDonateViewModel::class)
    fun provideBluetoothDonateViewModel(viewModel: BluetoothDonateViewModel): ViewModel = viewModel

}