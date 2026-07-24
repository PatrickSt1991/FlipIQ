package nl.madebypatrick.flipiq.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import nl.madebypatrick.flipiq.data.db.FlipIQDatabase
import nl.madebypatrick.flipiq.data.db.InventoryDao
import nl.madebypatrick.flipiq.data.db.SavedItemDao
import nl.madebypatrick.flipiq.data.db.ScanDao
import nl.madebypatrick.flipiq.data.repository.CollectionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlipIQDatabase =
        Room.databaseBuilder(context, FlipIQDatabase::class.java, FlipIQDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideScanDao(db: FlipIQDatabase): ScanDao = db.scanDao()

    @Provides
    fun provideInventoryDao(db: FlipIQDatabase): InventoryDao = db.inventoryDao()

    @Provides
    fun provideSavedItemDao(db: FlipIQDatabase): SavedItemDao = db.savedItemDao()

    @Provides
    @Singleton
    fun provideCollectionRepository(
        scanDao: ScanDao,
        inventoryDao: InventoryDao,
        savedItemDao: SavedItemDao,
    ): CollectionRepository = CollectionRepository(scanDao, inventoryDao, savedItemDao)
}
