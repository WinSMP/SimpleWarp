package org.winlogon.simplewarp

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class Warp(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Warp>(WarpTable)

    var name: String by WarpTable.name
    var x: Double by WarpTable.x
    var y: Double by WarpTable.y
    var z: Double by WarpTable.z
    var world: String by WarpTable.world
}