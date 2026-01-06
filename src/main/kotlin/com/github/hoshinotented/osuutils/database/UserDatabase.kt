package com.github.hoshinotented.osuutils.database

import com.github.hoshinotented.osuutils.data.User
import com.github.hoshinotented.osuutils.io.DefaultFileIO
import com.github.hoshinotented.osuutils.io.FileIO
import java.nio.file.Path

class UserDatabase(profileDir: Path) {
  private val userCache = JsonCache(profileDir.resolve("user_data.json"), User.serializer(), DefaultFileIO)
  
  fun load(): User? {
    return userCache.get()
  }
  
  fun save(user: User) {
    userCache.set(user)
  }
  
  fun save() {
    // unlike other structure, User has internal mutability
    userCache.save(userCache.get() ?: return)
  }
}