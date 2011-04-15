package ch.ngiger.comm.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import ch.elexis.util.FileUtility;

/**
 * This is a basic wrapper around the sun.net.ftp.FtpClient class, which is included with Sun Java
 * that allows you to make FTP connections and file transfers.
 * <p>
 * Based on programm version 1.0 of Julian Robichaux, http://www.nsftools.com
 * 
 * @author Immi
 * @version 1.0
 */
public class FtpServer extends FTPClient {
	
	private String fullSemaName;
	
	private void setWorkingDirectory(String serverFile) throws IOException{
		String path = FileUtility.getFilepath(serverFile);
		if (path != null && path.length() > 0) {
			changeWorkingDirectory(path);
		}
	}
	
	/**
	 * Download a file from the server, and save it to the specified local file
	 */
	public void downloadFile(String remoteFilenamePath, String localFilenamePath)
		throws IOException{
		setWorkingDirectory(remoteFilenamePath);
		String remoteFile = FileUtility.getFilename(remoteFilenamePath);
		
		FileOutputStream localFile = null;
		
		try {
			localFile = new FileOutputStream(localFilenamePath);
			if (!retrieveFile(remoteFile, localFile)) {
				throw new IOException("File not received succesfully: " //$NON-NLS-1$
					+ getReplyString());
			}
		} finally {
			if (localFile != null) {
				localFile.close();
			}
		}
	}
	
	/*
	 * Delete file on the FTP server
	 * 
	 * @name of the file to delet
	 */
	public boolean deleteFile(String name) throws IOException{
		return super.deleteFile(name);
	}
	
	/**
	 * Upload a file to the server
	 */
	public boolean uploadFile(String remoteFilenamePath, String localFilenamePath)
		throws IOException{
		setWorkingDirectory(remoteFilenamePath);
		String remoteFile = FileUtility.getFilename(remoteFilenamePath);
		
		FileInputStream localFile = null;
		
		try {
			localFile = new FileInputStream(localFilenamePath);
			
			if (!storeFile(remoteFile, localFile)) {
				throw new IOException("File not sent succesfully: " //$NON-NLS-1$
					+ getReplyString());
			}
		} finally {
			if (localFile != null) {
				localFile.close();
			}
		}
		return true;
	}
	
	/**
	 * Disconnect from Server
	 */
	public void disconnect() throws IOException{
		if (isConnected()) {
			super.disconnect();
		}
	}
	
	/**
	 * List of filenames on ftp server
	 */
	public String[] listNames() throws IOException{
		String[] files = super.listNames();
		if (files == null) {
			return new String[0];
		}
		return files;
	}
	
	/**
	 * List of files on ftp server
	 */
	public FTPFile[] listFiles() throws IOException{
		FTPFile[] files = super.listFiles();
		if (files == null) {
			return new FTPFile[0];
		}
		return files;
	}
	
	public void openConnection(String host, String user, String pwd) throws IOException{
		
		if (!isConnected()) {
			connect(host);
			mode(FtpServer.BINARY_FILE_TYPE);
		}
		
		if (isConnected()) {
			if (!login(user, pwd)) {
				throw new IOException(getReplyStrings()[0]);
			}
		}
		
		enterLocalPassiveMode();
	}
	
	public void closeConnection() throws IOException{
		if (isConnected()) {
			super.disconnect();
		}
	}
	
	private void uploadSemaphore(String semaName) throws IOException{
		fullSemaName = System.getProperty("user.home", "") + //$NON-NLS-1$ 
			System.getProperty("file.separator") + semaName; //$NON-NLS-1$
		File file = new File(fullSemaName); //$NON-NLS-1$
		file.createNewFile();
		uploadFile(file.getName(), file.getPath());
	}
	
	/**
	 * Copy a file to the FTP server as a simple kind of semaphore. We will upload ourFile. Once
	 * this is done, we get the list of all files and verify that their file is not there Not thread
	 * safe in any way!!!!
	 */
	public void addSemaphore(String downloadDir, String ourFile, String theirFile)
		throws FtpSemaException{
		try {
			uploadSemaphore(ourFile);
			// Teamw.sem checken
			String[] filenameList = this.listNames();
			for (String filename : filenameList) {
				if (filename.toLowerCase().equals(theirFile)) {
					throw new FtpSemaException("FtpServer.semaphore.error"); //$NON-NLS-1$
				}
			}
		} catch (IOException e) {
			throw new FtpSemaException(e);
		}
	}
	
	/**
	 * praxis.sem auf FTP Server l�schen
	 */
	public void removeSemaphore() throws IOException{
		super.deleteFile(fullSemaName);
		fullSemaName = null;
	}
	
}
