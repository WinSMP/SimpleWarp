package org.winlogon.simplewarp

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.winlogon.asynccraftr.AsyncCraftr
import org.bukkit.plugin.Plugin
import java.io.File
import java.sql.DriverManager
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

class DatabaseHandler(private val dataFolder: File) {
    private val dbUrl: String = "jdbc:sqlite:${dataFolder.absolutePath}/warps.db"
    private var initialized = false

    fun connect(plugin: Plugin): CompletableFuture<Boolean> {
        return AsyncCraftr.runAsync(plugin, Callable {
            try {
                Database.connect({ DriverManager.getConnection(dbUrl) })

                transaction {
                    SchemaUtils.create(WarpTable)
                }
                initialized = true
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        })
    }

    fun close(plugin: Plugin): CompletableFuture<Boolean> {
        return AsyncCraftr.runAsync(plugin, Callable {
            try {
                if (initialized) {
                    // Database connections are managed by Exposed transactions
                    // No explicit close needed
                }
                initialized = false
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        })
    }

    fun createWarp(plugin: Plugin, name: String, x: Double, y: Double, z: Double, world: String): CompletableFuture<Warp> {
        return AsyncCraftr.runAsync(plugin, Callable {
            transaction {
                Warp.new {
                    this.name = name
                    this.x = x
                    this.y = y
                    this.z = z
                    this.world = world
                }
            }
        })
    }

    fun findWarpByName(plugin: Plugin, name: String): CompletableFuture<Warp?> {
        return AsyncCraftr.runAsync(plugin, Callable {
            transaction {
                Warp.find { WarpTable.name eq name }.firstOrNull()
            }
        })
    }

    fun removeWarpByName(plugin: Plugin, name: String): CompletableFuture<Int> {
        return AsyncCraftr.runAsync(plugin, Callable {
            transaction {
                WarpTable.deleteWhere { WarpTable.name eq name }
            }
        })
    }

    fun updateWarpLocation(plugin: Plugin, name: String, x: Double, y: Double, z: Double): CompletableFuture<Int> {
        return AsyncCraftr.runAsync(plugin, Callable {
            transaction {
                WarpTable.update({ WarpTable.name eq name }) {
                    it[WarpTable.x] = x
                    it[WarpTable.y] = y
                    it[WarpTable.z] = z
                }
            }
        })
    }

    fun getAllWarpNames(plugin: Plugin): CompletableFuture<List<String>> {
        return AsyncCraftr.runAsync(plugin, Callable {
            transaction {
                WarpTable.selectAll()
                    .orderBy(WarpTable.name)
                    .map { it[WarpTable.name] }
            }
        })
    }
}