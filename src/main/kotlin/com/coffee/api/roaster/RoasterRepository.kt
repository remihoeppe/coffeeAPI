package com.coffee.api.roaster

import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.lowerCase


interface RoasterRepository {
    fun allRoasters(): List<Roaster>
    fun roasterByName(name: String): Roaster?
    fun roasterById(id: String): Roaster?
    fun addRoaster(newRoaster: Roaster)
    fun removeRoaster(name: String): Boolean
}

class PostgresRoasterRepository : RoasterRepository {
    override fun allRoasters(): List<Roaster> = dbTransaction { RoasterDAO.all().map(::daoToModel) }

    override fun roasterByName(name: String): Roaster? =
        dbTransaction {
            RoasterDAO.find { RoasterTable.name.lowerCase() eq name.lowercase() }
                .limit(1)
                .map(::daoToModel)
                .firstOrNull()
        }


    override fun roasterById(id: String): Roaster? = dbTransaction {
        RoasterDAO.find { RoasterTable.id eq id.toInt() }
            .limit(1)
            .map(::daoToModel)
            .firstOrNull()
    }

    override fun addRoaster(newRoaster: Roaster): Unit = dbTransaction {
        RoasterDAO.new {
            name = newRoaster.name
            url = newRoaster.url
            address = newRoaster.address
        }
    }

    override fun removeRoaster(name: String): Boolean = dbTransaction {
        val rowsDeleted = RoasterTable.deleteWhere { RoasterTable.name.lowerCase() eq name.lowercase() }
        rowsDeleted == 1;
    }
}

class InternalMemoryRepository : RoasterRepository {
    private var roasters = mutableListOf(
        Roaster("Monmouth Coffee Company", "https://www.monmouthcoffee.co.uk/", "123 Street"),
        Roaster("Square Mile Coffee Roasters", "https://shop.squaremilecoffee.com/", "123 Street"),
        Roaster("Skylark Coffee", "https://skylark.coffee/", "123 Street"),
        Roaster("Grindsmith", "https://grindsmith.com/", "123 Street"),
        Roaster("Curve Coffee", "https://www.curveroasters.co.uk/", "123 Street"),
    )

    override fun allRoasters(): List<Roaster> = roasters

    override fun roasterByName(name: String) = roasters.find {
        it.name.equals(name, ignoreCase = true)
    }

    override fun roasterById(id: String): Roaster? {
        TODO("Not yet implemented")
    }

    override fun addRoaster(newRoaster: Roaster) {
        if (roasterByName(newRoaster.name) != null) {
            throw IllegalStateException("This roaster already exists")
        } else {
            roasters.add(newRoaster)
        }
    }

    override fun removeRoaster(name: String): Boolean {
        return roasters.removeIf { it.name == name }
    }
}
