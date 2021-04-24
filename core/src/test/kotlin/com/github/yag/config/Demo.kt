package com.github.yag.config

class Demo {

    @Value
    private var title: String = ""

    @Value
    private var owner: Owner = Owner()

    @Value
    private var database: Database = Database()

    @Value
    private var servers: Map<String, Server> = HashMap<String, Server>()



    class Owner {
        @Value
        private var name: String = ""
        @Value
        private var dob: String = ""

        override fun toString(): String {
            return "Owner(name='$name', dob='$dob')"
        }
    }

    class Database {
        @Value
        private var enabled = false
        @Value
        private var ports = ArrayList<Int>()
        @Value("temp_targets")
        private var tempTargets = Temp()
        override fun toString(): String {
            return "Database(enabled=$enabled, ports=$ports, tempTargets=$tempTargets)"
        }
    }

    class Temp {
        @Value
        private var cpu = 0.0
        @Value
        private var case = 0.0
        override fun toString(): String {
            return "Temp(cpu=$cpu, case=$case)"
        }
    }

    class Server {
        @Value
        private var ip: String = ""
        @Value
        private var role: String = ""
        override fun toString(): String {
            return "Server(ip='$ip', role='$role')"
        }
    }

    override fun toString(): String {
        return "Demo(title='$title', owner=$owner, database=$database, servers=$servers)"
    }
}
