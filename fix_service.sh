sed -i 's/startPeriodicDuckDnsUpdate()/\/\/ Removed periodic duckdns update/g' app/src/main/java/com/example/service/SmsGatewayService.kt
sed -i 's/triggerDuckDnsUpdate/restartNgrok/g' app/src/main/java/com/example/service/SmsGatewayService.kt
sed -i 's/private fun restartNgrok/private fun restartNgrok/g' app/src/main/java/com/example/service/SmsGatewayService.kt
