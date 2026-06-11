package com.example.data

import kotlinx.coroutines.flow.Flow

class PasswordRepository(private val passwordDao: PasswordDao) {

    val allPasswords: Flow<List<PasswordEntity>> = passwordDao.getAllPasswords()

    suspend fun insertPassword(password: PasswordEntity): Long {
        return passwordDao.insertPassword(password)
    }

    suspend fun updatePassword(password: PasswordEntity) {
        passwordDao.updatePassword(password)
    }

    suspend fun deletePassword(password: PasswordEntity) {
        passwordDao.deletePassword(password)
    }

    suspend fun deletePasswordById(id: Int) {
        passwordDao.deletePasswordById(id)
    }
}
