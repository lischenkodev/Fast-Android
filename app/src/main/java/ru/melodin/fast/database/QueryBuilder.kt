package ru.melodin.fast.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class QueryBuilder private constructor() {

    private val buffer: StringBuilder = StringBuilder()


    fun select(column: String): QueryBuilder {
        this.buffer.append("SELECT ")
                .append(column)
                .append(" ")
        return this
    }


    fun from(table: String): QueryBuilder {
        this.buffer.append("FROM ")
                .append(table)
                .append(" ")
        return this
    }

    fun where(clause: String): QueryBuilder {
        this.buffer.append("WHERE ")
                .append(clause)
                .append(" ")
        return this
    }

    fun leftJoin(table: String): QueryBuilder {
        this.buffer.append("LEFT JOIN ")
                .append(table)
                .append(" ")
        return this
    }

    fun on(where: String): QueryBuilder {
        this.buffer.append("ON ")
                .append(where)
                .append(" ")
        return this
    }


    fun and(): QueryBuilder {
        this.buffer.append("AND ")
        return this
    }


    fun or(): QueryBuilder {
        this.buffer.append("OR ")
        return this
    }


    fun asCursor(db: SQLiteDatabase): Cursor {
        return db.rawQuery(toString(), null)
    }

    override fun toString(): String {
        return buffer.toString().trim()
    }

    companion object {

        fun query(): QueryBuilder {
            return QueryBuilder()
        }
    }
}
