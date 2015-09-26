 *******************************************************************************
 * COPYRIGHT Fatih ONUR 2015
 *
 * Enjoy with the open source codes :) Help the community help the yourself...
 *******************************************************************************

== TOPICS ==
Jsch SSH Client Example (&& Slf4j Log4j Logging Framework Example
 && Testng Example && Maven Project Example)

== NOTE ==
1) Before execute the test cases, make sure that you have updated:
 - Tunneling server details on SshClient
 - User1 and User2 details on SshClientTest

 2) By default test cases are skipped. In order to execute them, after completed
 step-1 above, you can update the skipTest tag in the POM file as below:
 - <skipTests>true</skipTests>

3) You can execute the application via following options;
 - $ mvn clean install
 or
 - $ mvn test
