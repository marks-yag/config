package com.github.yag.config

class Demo {

    private lateinit var title: String

    private lateinit var owner: Owner

    private lateinit var database: Database

    private lateinit var servers: Map<String, Server>

    class Owner {
        private lateinit var name: String
        private lateinit var dob: String
    }

    class Database {
        private var enabled = false
        private var ports = ArrayList<Int>()
        private var data = ArrayList<ArrayList<Any>>()
        private var tempTargets = Temp()
    }

    class Temp {
        private var cpu = 0.0
        private var case = 0.0
    }

    class Server {
        private lateinit var ip: String
        private lateinit var role: String
    }
}
