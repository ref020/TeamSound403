package com.alexmercerind.audire.db

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.alexmercerind.audire.models.HistoryItem
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class HistoryItemDao_Impl(
  __db: RoomDatabase,
) : HistoryItemDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfHistoryItem: EntityInsertAdapter<HistoryItem>

  private val __deleteAdapterOfHistoryItem: EntityDeleteOrUpdateAdapter<HistoryItem>
  init {
    this.__db = __db
    this.__insertAdapterOfHistoryItem = object : EntityInsertAdapter<HistoryItem>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `history_item` (`id`,`timestamp`,`title`,`artists`,`cover`,`album`,`label`,`year`,`lyrics`,`liked`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: HistoryItem) {
        val _tmpId: Int? = entity.id
        if (_tmpId == null) {
          statement.bindNull(1)
        } else {
          statement.bindLong(1, _tmpId.toLong())
        }
        statement.bindLong(2, entity.timestamp)
        statement.bindText(3, entity.title)
        statement.bindText(4, entity.artists)
        statement.bindText(5, entity.cover)
        val _tmpAlbum: String? = entity.album
        if (_tmpAlbum == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpAlbum)
        }
        val _tmpLabel: String? = entity.label
        if (_tmpLabel == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpLabel)
        }
        val _tmpYear: String? = entity.year
        if (_tmpYear == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpYear)
        }
        val _tmpLyrics: String? = entity.lyrics
        if (_tmpLyrics == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpLyrics)
        }
        val _tmp: Int = if (entity.liked) 1 else 0
        statement.bindLong(10, _tmp.toLong())
      }
    }
    this.__deleteAdapterOfHistoryItem = object : EntityDeleteOrUpdateAdapter<HistoryItem>() {
      protected override fun createQuery(): String = "DELETE FROM `history_item` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: HistoryItem) {
        val _tmpId: Int? = entity.id
        if (_tmpId == null) {
          statement.bindNull(1)
        } else {
          statement.bindLong(1, _tmpId.toLong())
        }
      }
    }
  }

  public override suspend fun insert(historyItem: HistoryItem): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfHistoryItem.insert(_connection, historyItem)
  }

  public override suspend fun delete(historyItem: HistoryItem): Unit = performSuspending(__db,
      false, true) { _connection ->
    __deleteAdapterOfHistoryItem.handle(_connection, historyItem)
  }

  public override fun getAll(): Flow<List<HistoryItem>> {
    val _sql: String = "SELECT * FROM history_item ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("history_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfArtists: Int = getColumnIndexOrThrow(_stmt, "artists")
        val _columnIndexOfCover: Int = getColumnIndexOrThrow(_stmt, "cover")
        val _columnIndexOfAlbum: Int = getColumnIndexOrThrow(_stmt, "album")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfYear: Int = getColumnIndexOrThrow(_stmt, "year")
        val _columnIndexOfLyrics: Int = getColumnIndexOrThrow(_stmt, "lyrics")
        val _columnIndexOfLiked: Int = getColumnIndexOrThrow(_stmt, "liked")
        val _result: MutableList<HistoryItem> = mutableListOf()
        while (_stmt.step()) {
          val _item: HistoryItem
          val _tmpId: Int?
          if (_stmt.isNull(_columnIndexOfId)) {
            _tmpId = null
          } else {
            _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          }
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpArtists: String
          _tmpArtists = _stmt.getText(_columnIndexOfArtists)
          val _tmpCover: String
          _tmpCover = _stmt.getText(_columnIndexOfCover)
          val _tmpAlbum: String?
          if (_stmt.isNull(_columnIndexOfAlbum)) {
            _tmpAlbum = null
          } else {
            _tmpAlbum = _stmt.getText(_columnIndexOfAlbum)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpYear: String?
          if (_stmt.isNull(_columnIndexOfYear)) {
            _tmpYear = null
          } else {
            _tmpYear = _stmt.getText(_columnIndexOfYear)
          }
          val _tmpLyrics: String?
          if (_stmt.isNull(_columnIndexOfLyrics)) {
            _tmpLyrics = null
          } else {
            _tmpLyrics = _stmt.getText(_columnIndexOfLyrics)
          }
          val _tmpLiked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfLiked).toInt()
          _tmpLiked = _tmp != 0
          _item =
              HistoryItem(_tmpId,_tmpTimestamp,_tmpTitle,_tmpArtists,_tmpCover,_tmpAlbum,_tmpLabel,_tmpYear,_tmpLyrics,_tmpLiked)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun search(term: String): List<HistoryItem> {
    val _sql: String =
        "SELECT * FROM history_item WHERE LOWER(title) LIKE '%' || ? || '%' ORDER BY timestamp DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, term)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfArtists: Int = getColumnIndexOrThrow(_stmt, "artists")
        val _columnIndexOfCover: Int = getColumnIndexOrThrow(_stmt, "cover")
        val _columnIndexOfAlbum: Int = getColumnIndexOrThrow(_stmt, "album")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfYear: Int = getColumnIndexOrThrow(_stmt, "year")
        val _columnIndexOfLyrics: Int = getColumnIndexOrThrow(_stmt, "lyrics")
        val _columnIndexOfLiked: Int = getColumnIndexOrThrow(_stmt, "liked")
        val _result: MutableList<HistoryItem> = mutableListOf()
        while (_stmt.step()) {
          val _item: HistoryItem
          val _tmpId: Int?
          if (_stmt.isNull(_columnIndexOfId)) {
            _tmpId = null
          } else {
            _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          }
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpArtists: String
          _tmpArtists = _stmt.getText(_columnIndexOfArtists)
          val _tmpCover: String
          _tmpCover = _stmt.getText(_columnIndexOfCover)
          val _tmpAlbum: String?
          if (_stmt.isNull(_columnIndexOfAlbum)) {
            _tmpAlbum = null
          } else {
            _tmpAlbum = _stmt.getText(_columnIndexOfAlbum)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpYear: String?
          if (_stmt.isNull(_columnIndexOfYear)) {
            _tmpYear = null
          } else {
            _tmpYear = _stmt.getText(_columnIndexOfYear)
          }
          val _tmpLyrics: String?
          if (_stmt.isNull(_columnIndexOfLyrics)) {
            _tmpLyrics = null
          } else {
            _tmpLyrics = _stmt.getText(_columnIndexOfLyrics)
          }
          val _tmpLiked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfLiked).toInt()
          _tmpLiked = _tmp != 0
          _item =
              HistoryItem(_tmpId,_tmpTimestamp,_tmpTitle,_tmpArtists,_tmpCover,_tmpAlbum,_tmpLabel,_tmpYear,_tmpLyrics,_tmpLiked)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getFilterArtistChoices(): Flow<List<String>> {
    val _sql: String = "SELECT DISTINCT artists FROM history_item"
    return createFlow(__db, false, arrayOf("history_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getFilterYearChoices(): Flow<List<String>> {
    val _sql: String = "SELECT DISTINCT year FROM history_item"
    return createFlow(__db, false, arrayOf("history_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun like(id: Int) {
    val _sql: String = "UPDATE history_item SET liked = 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun unlike(id: Int) {
    val _sql: String = "UPDATE history_item SET liked = 0 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
