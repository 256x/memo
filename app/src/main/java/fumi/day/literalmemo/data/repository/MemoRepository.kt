package fumi.day.literalmemo.data.repository

import fumi.day.literalmemo.domain.model.Memo
import kotlinx.coroutines.flow.Flow

interface MemoRepository {
    fun observeAll(): Flow<List<Memo>>
    fun observeTrash(): Flow<List<Memo>>
    suspend fun getByFileName(fileName: String): Memo?
    suspend fun save(memo: Memo)
    suspend fun trash(fileName: String)
    suspend fun restore(fileName: String)
    suspend fun deletePermanently(fileName: String)
    fun search(query: String): Flow<List<Memo>>
}
