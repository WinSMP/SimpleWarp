package org.winlogon.simplewarp

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object WarpTable : IntIdTable("warps") {
    val name = text("name").uniqueIndex()
    val x = double("x")
    val y = double("y")
    val z = double("z")
    val world = text("world")
}