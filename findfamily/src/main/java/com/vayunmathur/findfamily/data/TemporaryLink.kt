package com.vayunmathur.findfamily.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable
import com.vayunmathur.library.util.DatabaseItem
import kotlinx.serialization.Serializable
import kotlin.time.Instant


@Immutable
@Serializable
@Entity
data class TemporaryLink(
    val name: String,
    val key: String,
    val publicKey: String,
    val deleteAt: Instant,

    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
): DatabaseItem
