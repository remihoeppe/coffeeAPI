package com.coffee.api

import com.coffee.api.TestUtils.expectNoContent
import com.coffee.api.TestUtils.expectNotFound
import com.coffee.api.TestUtils.expectOK
import com.coffee.api.coffee.Coffee
import com.coffee.api.coffee.CoffeeTable
import com.coffee.api.roaster.RoasterCoffeeTable
import com.coffee.api.roaster.RoasterTable
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.Status
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RoasterTest {

    private val api = coffeeAPI()

    private val grindSmithJson =
        "{\"name\":\"Grindsmith\",\"url\":\"https://grindsmith.com/\",\"address\":\"123 Street\"}"
    private val newRoasterJson =
        "{\"name\":\"Verve Coffee\",\"url\":\"https://vervecoffee.com\",\"address\":\"Santa Cruz, CA\"}"
    val invalidData = "{\"name\":\"\",\"url\":\"\",\"address\":\"\"}"

    @Test
    fun `Testing server root endpoint`() {
        api(Request(GET, "/")).expectOK()
    }

    @Test
    fun `API returns a list of roasters GET request`() {
        val response = api(Request(GET, "/roasters")).expectOK()
        val responseBody = response.bodyString()
        val json = jacksonObjectMapper().readTree(responseBody)
        assertTrue(json.isArray)
    }

    @Test
    fun `API returns a specific roaster when GET requests contains a name parameter`() {
        api(Request(POST, "/roasters").body(grindSmithJson))
        val response = api(Request(GET, "/roasters/byName/grindsmith"))
        assertEquals(grindSmithJson, response.bodyString())
    }

    @Test
    fun `API returns a specific roaster when GET requests contains an ID parameter`() {
        val response = api(Request(GET, "/roasters/byId/4"))
        assertEquals(grindSmithJson, response.bodyString())
    }

    @Test
    fun `API should return 404 when an invalid name is send through a GET request`() {
        val response = api(Request(GET, "/roaster/byName/covfefe"))
        response.expectNotFound()
    }

    @Test
    fun `API should return 400 when invalid roaster data is sent`() {
//        Only testing if any of the data field is empty.
//        TODO: Add supplemental checks re valid URL
        val response = api(Request(POST, "/roasters").body(invalidData))
        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `API should return a 204 when valid data has been sent through a POST request`() {
        api(Request(POST, "/roasters").body(newRoasterJson)).expectNoContent()
    }

    @Test
    fun `API should return 204 when valid name parameter sent through DELETE request`() {
        api(Request(DELETE, "/roasters/grindsmith")).expectNoContent()
    }

    @Test
    fun `API should return 404 when invalid name param sent through DEL request`() {
        api(Request(DELETE, "/roasters/covfefe")).expectNotFound()
    }

    companion object {

        private var coffees = listOf(Coffee("Good Coffee"), Coffee("More Good Coffee"))

        @JvmStatic
        @BeforeAll
        fun dbConnect(): Unit {
            Database.connect(
                url = "jdbc:postgresql://localhost:5432/mycoffeeapp",
                driver = "org.postgresql.Driver",
                user = "remi",
                password = "postgres",
            )
            transaction {
                SchemaUtils.drop(RoasterTable, CoffeeTable, RoasterCoffeeTable)
                SchemaUtils.create(RoasterTable, CoffeeTable, RoasterCoffeeTable)
                val roaster1 = RoasterTable.insertAndGetId {
                    it[name] = "Monmouth Coffee Company"
                    it[url] = "https://www.monmouthcoffee.co.uk/"
                    it[address] = "123 Street"
                }

                val roaster2 = RoasterTable.insertAndGetId {
                    it[name] = "Square Mile Coffee Roasters"
                    it[url] = "https://shop.squaremilecoffee.com/"
                    it[address] = "123 Street"
                }

                val roaster3 = RoasterTable.insertAndGetId {
                    it[name] = "Skylark Coffee"
                    it[url] = "https://skylark.coffee/"
                    it[address] = "123 Street"
                }
                // Insert coffees and associate them with roasters
                val coffeesForRoaster1 = listOf("Good Coffee", "Espresso Blend")
                val coffeesForRoaster2 = listOf("House Blend", "French Roast")
                val coffeesForRoaster3 = listOf("Organic Dark Roast", "Ethiopian Light Roast")

                // Function to add coffees to a roaster
                fun addCoffeesToRoaster(roasterId: EntityID<Int>, coffeeNames: List<String>) {
                    coffeeNames.forEach { coffeeName ->
                        val coffeeId = CoffeeTable.insertAndGetId { it[name] = coffeeName }
                        RoasterCoffeeTable.insert {
                            it[roaster] = roasterId
                            it[coffee] = coffeeId
                        }
                    }
                }
                // Associate coffees with each roaster
                addCoffeesToRoaster(roaster1, coffeesForRoaster1)
                addCoffeesToRoaster(roaster2, coffeesForRoaster2)
                addCoffeesToRoaster(roaster3, coffeesForRoaster3)
            }
            println("Database prepared successfully")
        }
    }
}
