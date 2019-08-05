package ru.melodin.fast.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class QueryBuilder private constructor() {

    private val builder: StringBuilder = StringBuilder()

    fun select(column: String): QueryBuilder {
        this.builder.append("SELECT ")
            .append(column)
            .append(" ")
        return this
    }


    fun from(table: String): QueryBuilder {
        this.builder.append("FROM ")
            .append(table)
            .append(" ")
        return this
    }

    fun where(clause: String): QueryBuilder {
        this.builder.append("WHERE ")
            .append(clause)
            .append(" ")
        return this
    }

    fun orderBy(table: String): QueryBuilder {
        this.builder.append("ORDER BY ")
            .append(table)
            .append(" ")
        return this
    }

    fun leftJoin(table: String): QueryBuilder {
        this.builder.append("LEFT JOIN ")
            .append(table)
            .append(" ")
        return this
    }

    fun on(where: String): QueryBuilder {
        this.builder.append("ON ")
            .append(where)
            .append(" ")
        return this
    }


    fun and(): QueryBuilder {
        this.builder.append("AND ")
        return this
    }


    fun or(): QueryBuilder {
        this.builder.append("OR ")
        return this
    }


    fun asCursor(db: SQLiteDatabase): Cursor {
        return db.rawQuery(toString(), null)
    }

    override fun toString(): String {
        return builder.toString().trim()
    }

    companion object {

        fun query(): QueryBuilder {
            return QueryBuilder()
        }
    }
}
