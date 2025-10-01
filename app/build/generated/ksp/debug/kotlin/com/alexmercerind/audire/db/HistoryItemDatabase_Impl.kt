package com.alexmercerind.audire.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class HistoryItemDatabase_Impl : HistoryItemDatabase() {
  private val _historyItemDao: Lazy<HistoryItemDao> = lazy {
    HistoryItemDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2,
        "2ea2e6c994114db4cc6a40d4382db78c", "e52594eb0de5363436a85ad28cf1b60f") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `history_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `timestamp` INTEGER NOT NULL, `title` TEXT NOT NULL, `artists` TEXT NOT NULL, `cover` TEXT NOT NULL, `album` TEXT, `label` TEXT, `year` TEXT, `lyrics` TEXT, `liked` INTEGER NOT NULL DEFAULT 0)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2ea2e6c994114db4cc6a40d4382db78c')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `history_item`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsHistoryItem: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsHistoryItem.put("id", TableInfo.Column("id", "INTEGER", false, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHistoryItem.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHistoryItem.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHistoryItem.put("artists", TableInfo.Column("artists", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHistoryItem.put("cover", TableInfo.Column("cover", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHistoryItem.put("album", TableInfo.Column("album", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHistoryItem.put("label", TableInfo.Column("label", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHistoryItem.put("year", TableInfo.Column("year", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHistoryItem.put("lyrics", TableInfo.Column("lyrics", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHistoryItem.put("liked", TableInfo.Column("liked", "INTEGER", true, 0, "0",
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysHistoryItem: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesHistoryItem: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoHistoryItem: TableInfo = TableInfo("history_item", _columnsHistoryItem,
            _foreignKeysHistoryItem, _indicesHistoryItem)
        val _existingHistoryItem: TableInfo = read(connection, "history_item")
        if (!_infoHistoryItem.equals(_existingHistoryItem)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |history_item(com.alexmercerind.audire.models.HistoryItem).
              | Expected:
              |""".trimMargin() + _infoHistoryItem + """
              |
              | Found:
              |""".trimMargin() + _existingHistoryItem)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "history_item")
  }

  public override fun clearAllTables() {
    super.performClear(false, "history_item")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(HistoryItemDao::class, HistoryItemDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    _autoMigrations.add(HistoryItemDatabase_AutoMigration_1_2_Impl())
    return _autoMigrations
  }

  public override fun historyItemDao(): HistoryItemDao = _historyItemDao.value
}
