package com.dkchoi.wetalk.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dkchoi.wetalk.data.User

@Dao
interface UserDao {
    @Query("SELECT * FROM User")
    fun getAll(): List<User>;

    @Insert(onConflict = OnConflictStrategy.REPLACE) // primarykey가 중복되면 덮어씀
    fun insertAll(vararg users: User)
}