package com.alexmercerind.audire.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlin.Suppress

@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class HistoryItemDatabase_AutoMigration_1_2_Impl : Migration {
  public constructor() : super(1, 2)

  public override fun migrate(connection: SQLiteConnection) {
    connection.execSQL("ALTER TABLE `history_item` ADD COLUMN `liked` INTEGER NOT NULL DEFAULT 0")
  }
}
