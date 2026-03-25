package fumi.day.literalmemo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fumi.day.literalmemo.data.repository.MemoRepository
import fumi.day.literalmemo.data.repository.MemoRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMemoRepository(
        @ApplicationContext context: Context
    ): MemoRepository {
        return MemoRepositoryImpl(context)
    }
}
