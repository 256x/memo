package fumi.day.literalmemo.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fumi.day.literalmemo.data.repository.MemoRepository
import fumi.day.literalmemo.data.repository.MemoRepositoryImpl
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindMemoRepository(impl: MemoRepositoryImpl): MemoRepository

    companion object {
        @Provides
        @Singleton
        @PileDir
        fun providePileDir(@ApplicationContext context: Context): File =
            File(context.filesDir, "pile").also { it.mkdirs() }
    }
}
