package config

class Demo {

    @Value
    var title: String = ""

    @Value
    var owner: Owner = Owner()

    @Value
    var database: Database = Database()

    @Value
    var servers: Map<String, Server> = HashMap<String, Server>()

    class Owner {
        @Value
        var name: String = ""
        @Value
        var dob: String = ""

        override fun toString(): String {
            return "Owner(name='$name', dob='$dob')"
        }
    }

    class Database {
        @Value
        var enabled = false
        @Value
        var ports = ArrayList<Int>()
        @Value("temp_targets")
        var tempTargets = Temp()
        override fun toString(): String {
            return "Database(enabled=$enabled, ports=$ports, tempTargets=$tempTargets)"
        }
    }

    class Temp {
        @Value
        var cpu = 0.0
        @Value
        var case = 0.0
        override fun toString(): String {
            return "Temp(cpu=$cpu, case=$case)"
        }
    }

    class Server {
        @Value
        var ip: String = ""
        @Value
        var role: String = ""
        override fun toString(): String {
            return "Server(ip='$ip', role='$role')"
        }
    }

    override fun toString(): String {
        return "Demo(title='$title', owner=$owner, database=$database, servers=$servers)"
    }
}
