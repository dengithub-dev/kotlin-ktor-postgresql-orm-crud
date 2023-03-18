package com.example

import kotlinx.coroutines.*
import java.sql.Connection
import java.sql.Statement
import com.example.City

class CityService(private val connection: Connection) {
    companion object {
        private const val CHECKCITIESTABLEIFEXISTSORNOT = "SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename  = 'cities');"
        private const val CREATE_TABLE_CITIES = "CREATE TABLE CITIES (ID SERIAL PRIMARY KEY, NAME VARCHAR(255), POPULATION INT);"
        private const val SELECT_CITY_ALL = "SELECT name, population FROM cities;"
        private const val SELECT_CITY_BY_ID = "SELECT name, population FROM cities WHERE id = ?"
        private const val INSERT_CITY = "INSERT INTO cities (name, population) VALUES (?, ?)"
        private const val UPDATE_CITY = "UPDATE cities SET name = ?, population = ? WHERE id = ?"
        private const val DELETE_CITY = "DELETE FROM cities WHERE id = ?"

    }

    // Enable this if CITIES table does not exist yet
    init {
        val statement = connection.createStatement()
        val checkStatement = statement.executeQuery(CHECKCITIESTABLEIFEXISTSORNOT)
        if (checkStatement.next()) {
            val checkstatementresult = checkStatement.getString("exists") // value is t for true, f for false
            // create a condition if a table does not exist, create one
            if (checkstatementresult.toString() == "f") {
                statement.executeUpdate(CREATE_TABLE_CITIES)
            }
        }
    }

    private var newCityId = 0

    // Create new city
    suspend fun create(city: City): Int = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(INSERT_CITY, Statement.RETURN_GENERATED_KEYS)
        statement.setString(1, city.name)
        statement.setInt(2, city.population)
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) {
            return@withContext generatedKeys.getInt(1)
        } else {
            throw Exception("Unable to retrieve the id of the newly inserted city")
        }
    }

    // Read all city
    suspend fun readAll(): MutableList<City> {
        val getCityAllList = mutableListOf<City>()
        val statement = connection.prepareStatement(SELECT_CITY_ALL)
        val resultSet = statement.executeQuery()

        while (resultSet.next()) {
            val name = resultSet.getString("name")
            val population = resultSet.getInt("population")
            getCityAllList.add(City(name, population))
        }
        return getCityAllList
    }

    // Read a city
    suspend fun read(id: Int): City = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_CITY_BY_ID)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val name = resultSet.getString("name")
            val population = resultSet.getInt("population")
            return@withContext City(name, population)
        } else {
            throw Exception("Record not found")
        }
    }

    // Update a city
    suspend fun update(id: Int, city: City) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(UPDATE_CITY)
        statement.setString(1, city.name)
        statement.setInt(2, city.population)
        statement.setInt(3, id)
        statement.executeUpdate()
    }

    // Delete a city
    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(DELETE_CITY)
        statement.setInt(1, id)
        statement.executeUpdate()
    }
}