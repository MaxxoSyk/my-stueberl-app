package de.landstueberl.mystueberlapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class OrderDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val creationDate: Long,
    val dueDate: Long,
    val isCompleted: Boolean = false
)
