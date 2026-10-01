package com.curiouskids.cashapp.di

import android.content.Context
import com.curiouskids.cashapp.usb.UsbSerialManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UsbModule {

    @Provides
    @Singleton
    fun provideUsbSerialManager(@ApplicationContext context: Context): UsbSerialManager {
        return UsbSerialManager(context)
    }
}
