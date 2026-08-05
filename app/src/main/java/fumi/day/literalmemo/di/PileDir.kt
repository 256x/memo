package fumi.day.literalmemo.di

import javax.inject.Qualifier

/** The directory holding one `.md` file per memo. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PileDir
