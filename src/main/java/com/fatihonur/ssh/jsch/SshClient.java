package com.fatihonur.ssh.jsch;

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Fatih ONUR 2015
 *
 * Enjoy with the open source codes :) Help the community help the yourself...
 *******************************************************************************
 *----------------------------------------------------------------------------*/

// Resources
// http://stackoverflow.com/questions/28850188/ssh-tunneling-not-working-via-jsch
// http://bitfish.eu/java/control-your-server-over-ssh-with-java/
// http://www.beanizer.org/site/index.php/en/Articles/Java-ssh-tunneling-with-jsch.html
// http://www.jcraft.com/jsch/examples/Exec.java.html

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Generic ssh client provides copying files and running commands remotely.
 * <p>
 * Usage details: // @formatter:off <blockquote>
 *
 * <pre>
 * SshClient sshClient = new SshClient();
 * try{
 *     sshClient.authUserPassword(user, pass);
 *     sshClient.connect(remoteHost);
 * ...// do your job
 * } finally{
 *     sshClient.close();
 * }
 * </pre>
 *
 * </blockquote> // @formatter:off
 */
public class SshClient {

	private final static Logger logger = LoggerFactory.getLogger(SshClient.class);

	/**
	 * TODO: Change tunneling servers accordingly your gateway server.
	 *
	 * In order to reach remote server we need following server details while
	 * using tunneling
	 */
	private static String TUNNNELING_HOST = "hostname";
	private static String TUNELLING_USER_NAME = "user";
	private static String TUNNELING_USER_PWD = "password";

	private static int TUNELLING_HOST_PORT_FROM = 2223;
	private static int TUNNELING_HOST_PORT_TO = 22;

	/** Remote host user password */
	private String password;

	/** Remote host user name */
	private String user;

	/** Remote host */
	private String host;

	/** Remote host private key */
	private Path privateKey;

	/** Default connection session */
	private Session firstSession;

	/** Default tunneling connection session */
	private Session secondSession;

	/** Defines that tunneling is used */
	private boolean tunneled = false;

	/**
	 * Authenticates SshClient with given user name and password.
	 *
	 * @param user
	 *            the remote host user name
	 * @param password
	 *            the remote host passowrd
	 */
	public void authUserPassword(final String user, final String password) {
		this.user = user;
		this.password = password;
	}

	/**
	 * Authenticates SshClient with given user and private key. In order to use
	 * this feature end user has to copy the public key onto remote server's
	 * machine. See the below link for more details.
	 *
	 * @param user
	 *            the remote host user name
	 * @param privateKey
	 *            the private key used by user in order to connect without
	 *            password to remote host
	 * @see <a href=
	 *      "https://www.digitalocean.com/community/tutorials/how-to-set-up-ssh-keys--2"
	 *      >ssh keys setup</a>
	 */
	public void authUserPublicKey(final String user, final Path privateKey) {
		this.user = user;
		this.privateKey = privateKey;
	}

	/**
	 * Connects SshClient to remote host.
	 *
	 * @param host
	 *            the remote host name
	 * @throws IOException
	 *             if connection fails
	 */
	public void connect(final String host) throws IOException {
		this.host = host;
		if (firstSession == null || isTunneled() == false) {
			try {
				// allow connections to all hosts
				JSch.setConfig("StrictHostKeyChecking", "no");
				final JSch jsch = new JSch();
				firstSession = jsch.getSession(user, host);

				// create a session connected to port 2233 on the local host.
				if (privateKey != null) {
					jsch.addIdentity(privateKey.toString());
				}

				if (password != null) {
					firstSession.setPassword(password);
				} else if (privateKey == null) {
					throw new IOException(
							"Either privateKey nor password is set. Please call one of the authentication method.");
				}

				firstSession.connect();
				logger.debug("Connected directly to:{}", host);
				setTunneled(false);

			} catch (final JSchException ex) {
				throw new IOException(ex);
			}
		}

	}

	/**
	 * Connects the SshClient to remote host via default tunneling host details.
	 *
	 * @param host
	 *            the remote host where user wants to connect to
	 * @throws IOException
	 *             if connection fails
	 */
	public void connectThroughDefaultTunnel(final String host) throws IOException {
		this.host = host;
		if (secondSession == null) {
			try {
				// allow connections to all hosts
				JSch.setConfig("StrictHostKeyChecking", "no");

				final JSch jsch = new JSch();
				firstSession = jsch.getSession(TUNELLING_USER_NAME, TUNNNELING_HOST);
				firstSession.setPassword(TUNNELING_USER_PWD);

				int assigned_port = firstSession.setPortForwardingL(TUNELLING_HOST_PORT_FROM, host,
						TUNNELING_HOST_PORT_TO);
				logger.debug("assigned_port:{}", assigned_port);
				firstSession.connect();
				firstSession.openChannel("direct-tcpip");

				// create a session connected to port 2233 on the local host.
				if (privateKey != null) {
					jsch.addIdentity(privateKey.toString());
				}

				if (password != null) {
					firstSession.setPassword(password);
				} else if (privateKey == null) {
					throw new IOException(
							"Either privateKey nor password is set. Please call one of the authentication method.");
				}

				secondSession = jsch.getSession(user, "localhost", TUNELLING_HOST_PORT_FROM);
				secondSession.setPassword(password);

				secondSession.connect(); // now we're connected to the secondary
				// system

				logger.debug("Connected from:{} to:{}", TUNNNELING_HOST, host);

				setTunneled(true);

			} catch (final JSchException ex) {
				throw new IOException(ex);
			}
		}
	}

	/**
	 * Executes commands and scripts remotely and displays output through logs.
	 *
	 * @param command
	 *            the command to be executed. For example "ls -la; cd /" or
	 *            "/x/y/z/scriptName.sh"
	 * @return
	 * @throws IOException
	 *             if command fails to execute
	 */
	public int executeCommand(final String command) throws IOException {

		int exitStatus = -100;
		Channel channel = null;
		InputStream stdout = null;
		try {
			if (isTunneled()) {
				channel = secondSession.openChannel("exec");
			} else {
				channel = firstSession.openChannel("exec");
			}
			logger.debug("tunneled:{}", isTunneled());

			((ChannelExec) channel).setCommand(command);

			channel.setInputStream(null);
			stdout = channel.getInputStream();

			// TODO: Redirect to a logger
			((ChannelExec) channel).setErrStream(System.err);

			channel.connect();

			while (true) {
				final byte[] tmpArray = new byte[1024];
				while (stdout.available() > 0) {
					final int i = stdout.read(tmpArray, 0, 1024);
					if (i < 0) {
						break;
					}
					final String stdOutput = new String(tmpArray, 0, i);
					logger.info("\n{}", stdOutput);
				}
				if (channel.isClosed()) {
					if (stdout.available() > 0) {
						continue;
					}
					exitStatus = channel.getExitStatus();

					break;
				}
				try {
					Thread.sleep(1000);
				} catch (final Exception ee) {
					// unimportant time exception error
				}
			}
		} catch (final JSchException ex) {
			throw new IOException(ex);
		} finally {
			if (channel != null && channel.isConnected()) {
				channel.disconnect();
			}

			if (stdout != null) {
				stdout.close();
			}

		}
		logger.info("exitStatus:{}", exitStatus);
		return exitStatus;
	}

	/**
	 * Copies list of files to remote host.
	 *
	 * @param paths
	 *            the list of path of files
	 * @param remoteDestPath
	 *            remote destination folder
	 * @return true if all files are copied successfully
	 * @throws IOException
	 *             if copy operation files
	 */
	public boolean copyFiles(final List<Path> paths, final Path remoteDestPath) throws IOException {

		final int numOfFiles = paths.size();
		int numOfFilesCopiedSuccesfully = 0;

		ChannelSftp sftp = null;
		try {
			if (isTunneled()) {
				sftp = (ChannelSftp) secondSession.openChannel("sftp");
			} else {
				sftp = (ChannelSftp) firstSession.openChannel("sftp");
			}
			System.out.println("tunneled=" + isTunneled());

			sftp.connect();
			logger.debug("Connected remote server:{}", host);

			final String remoteDestPathName = remoteDestPath.toString().replace("\\", "/");
			logger.debug("Trying to access folderName: {}", remoteDestPathName);
			sftp.cd("/");

			final String[] remoteDestFolderNames = remoteDestPathName.split("/");
			for (int i = 1; i < remoteDestFolderNames.length; i++) {
				final String folder = remoteDestFolderNames[i];

				if (folder.length() > 0) {
					try {
						logger.debug("Command to be executed: cd {}", folder);
						sftp.cd(folder);
					} catch (final SftpException e) {
						logger.debug("Command to be executed: mkdir {}; cd {}", folder, folder);
						sftp.mkdir(folder);
						sftp.cd(folder);
					}
				}
			}

			logger.debug("Start uploading");
			for (final Path path : paths) {
				try (final InputStream in = Files.newInputStream(path)) {
					logger.debug("Uploading fileName:{}", path.getFileName());
					sftp.put(in, path.getFileName().toString());
					numOfFilesCopiedSuccesfully++;
				} catch (final IOException ex) {
					logger.info("Error occured while reading file:{}", ex.getMessage());
				}
			}

			// upload the files
			sftp.disconnect();

		} catch (final JSchException | SftpException ex) {
			throw new IOException(ex);
		} finally {
			if (sftp != null) {
				sftp.disconnect();
			}
		}
		return numOfFiles == numOfFilesCopiedSuccesfully;
	}

	/**
	 * Closes the all the open session in order to release the reosurces.
	 */
	public void close() {

		if (secondSession != null) {
			secondSession.disconnect();
			secondSession = null;
		}
		if (firstSession != null) {
			firstSession.disconnect();
			firstSession = null;
		}
	}

	/**
	 * Returns the tunneling session status.
	 *
	 * @return the tunneling session status
	 */
	public boolean isTunneled() {
		return tunneled;
	}

	/**
	 * Sets the tunneling session status
	 *
	 * @param tunneled
	 *            the tunneled to set
	 */
	public void setTunneled(final boolean tunneled) {
		this.tunneled = tunneled;

	}
}
