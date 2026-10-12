package com.cockroachcommunity.app.core.database.di

import android.content.Context
import androidx.room.Room
import com.cockroachcommunity.app.core.database.CockroachCommunityDatabase
import com.cockroachcommunity.app.core.database.dao.CockroachCommunityDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CockroachCommunityDatabase {
        return Room.databaseBuilder(
            context,
            CockroachCommunityDatabase::class.java,
            "cockroach_community_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideDao(database: CockroachCommunityDatabase): CockroachCommunityDao {
        return database.cockroachCommunityDao()
    }
}
