package com.fatihonur.ssh.jsch;

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Fatih ONUR 2015
 *
 * Enjoy with the open source codes :) Help the community help the yourself...
 *******************************************************************************
 *----------------------------------------------------------------------------*/

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fatihonur.ssh.util.FileUtils;

/**
 * Verifies the SshClient class.
 *
 * You have to edit host details based on your environment.
 *
 * Due to test execution occurs in parallel, priorities allows us to execute
 * test cases in specific order.
 *
 * @author qfatonu
 *
 */
public class SshClientTest {

	private final static Logger logger = LoggerFactory.getLogger(SshClientTest.class);

	// TODO: Change user details accordingly your enviroment.

	// Non-direct access from local windows machines
	private static String USER_1 = "user";
	private static String USER_1_PWD = "password";
	private static String USER_1_HOST = "hostname";

	// Server that being used for tunneling both from Windows and Jenkins in
	// order to access netsim servers
	private static String USER_2 = "user";
	private static String USER_2_PWD = "password";
	private static String USER_2_HOST = "hostname";

	private static String COMMAND = "hostname; pwd ";

	private SshClient sshClient;

	@BeforeTest
	public void beforeTest() {
		sshClient = new SshClient();
	}

	@AfterTest
	public void afterTest() {
		sshClient.close();
	}

	@Test(priority = 1)
	public void authUserPasswordAndConnect() throws IOException {
		sshClient.authUserPassword(USER_2, USER_2_PWD);
		sshClient.connect(USER_2_HOST);
	}

	@Test(priority = 1)
	public void executeCommand() throws IOException {
		sshClient.authUserPassword(USER_2, USER_2_PWD);
		sshClient.connect(USER_2_HOST);

		sshClient.executeCommand(COMMAND);
	}

	@Test(priority = 1)
	public void copyFiles() throws IOException {
		sshClient.authUserPassword(USER_2, USER_2_PWD);
		sshClient.connect(USER_2_HOST);
		final Path parentFolder = Paths.get("src/test/resources/");

		final Path remoteDestPath = Paths.get("/tmp/fatihonur/");

		final List<Path> paths = FileUtils.getSourceFiles(parentFolder);

		sshClient.copyFiles(paths, remoteDestPath);

	}

	@Test(priority = 2)
	public void authUserPasswordAndConnectThroughDefaultTunnel() throws IOException {
		sshClient.authUserPassword(USER_1, USER_1_PWD);
		sshClient.connectThroughDefaultTunnel(USER_1_HOST);
	}

	@Test(priority = 2)
	public void executeCommandThroughDefaultTunnel() throws IOException {
		sshClient.authUserPassword(USER_1, USER_1_PWD);
		sshClient.connectThroughDefaultTunnel(USER_1_HOST);

		sshClient.executeCommand(COMMAND);
	}

	@Test(priority = 2)
	public void copyFilesThroughDefaultTunnel() throws IOException {
		sshClient.authUserPassword(USER_1, USER_1_PWD);
		sshClient.connectThroughDefaultTunnel(USER_1_HOST);

		final Path parentFolder = Paths.get("src/test/resources/");

		final Path remoteDestPath = Paths.get("/tmp/fatihonur/");

		final List<Path> paths = FileUtils.getSourceFiles(parentFolder);

		sshClient.copyFiles(paths, remoteDestPath);

	}

	@Test(priority = 2)
	public void executeScriptThroughDefaultTunnel() throws IOException {
		sshClient.authUserPassword(USER_1, USER_1_PWD);
		sshClient.connectThroughDefaultTunnel(USER_1_HOST);

		final Path scriptPath1 = Paths.get("src/test/resources/simple_script.sh");

		final Path remoteDestPath1 = Paths.get("/tmp/script/fatihonur");

		final List<Path> paths = new ArrayList<>();
		paths.add(scriptPath1);

		sshClient.copyFiles(paths, remoteDestPath1);

		final String script = remoteDestPath1.toString().replace("\\", "/") + "/"
				+ scriptPath1.getFileName().toString();
		logger.debug("script:{}", script);

		// 1) When you copy files from windows you will get extra character at
		// the end of each line. Perl command will delete that.
		// 2) Also you have to make the executable the script
		final String cmd1 = "chmod +x " + script + "; perl -i -pe 's/\\r//g' " + script + ";" + script;
		sshClient.executeCommand(cmd1);
	}

}
