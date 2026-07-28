@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.ngrok:ngrok:1.4.1")

import com.ngrok.Session

fun main() {
    println(Session.withAuthtoken("test").toString())
}
