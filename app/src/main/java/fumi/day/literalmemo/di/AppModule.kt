package fumi.day.literalmemo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fumi.day.literalmemo.data.repository.MemoRepository
import fumi.day.literalmemo.data.repository.MemoRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindMemoRepository(impl: MemoRepositoryImpl): MemoRepository
}
