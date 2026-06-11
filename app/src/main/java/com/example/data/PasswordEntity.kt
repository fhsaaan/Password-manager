package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // e.g. "Work", "Personal", "Financial", "Social"
    val websiteUrl: String,
    
    val encryptedUsername: String,
    val usernameIv: String,
    
    val encryptedPassword: String,
    val passwordIv: String,
    
    val encryptedNotes: String,
    val notesIv: String,
    
    val updatedAt: Long = System.currentTimeMillis()
)
