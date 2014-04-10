package com.hypersocket.client.hosts;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.hypersocket.client.util.CommandExecutor;

public class WindowsSocketRedirector extends AbstractSocketRedirector implements
		SocketRedirector {

	File installCommand;
	File redirectCommand;
	File driverFile;
	
	public WindowsSocketRedirector() {
		
		String windowsDir = System.getenv("SystemRoot");
		String arch = System.getProperty("os.arch");
	
		File currentDriverFile = new File("bin"
				+ File.separator
				+ "windows"
				+ File.separator
				+ arch
				+ File.separator
				+ "ip_redirect_driver.sys");
		
		installCommand = new File("bin"
				+ File.separator
				+ "windows"
				+ File.separator
				+ arch
				+ File.separator
				+ "installer.exe");
		
		redirectCommand = new File("bin"
				+ File.separator
				+ "windows"
				+ File.separator
				+ arch
				+ File.separator
				+ "redirect.exe");
		
		driverFile = new File(windowsDir, "System32"
				+ File.separator
				+ "drivers"
				+ File.separator
				+ "ip_redirect_driver.sys");
		
		if(!driverFile.exists() || currentDriverFile.lastModified()!=driverFile.lastModified()) {
			try {
				FileUtils.copyFile(currentDriverFile, driverFile);
				driverFile.setLastModified(currentDriverFile.lastModified());
		
				CommandExecutor cmd = new CommandExecutor(installCommand.getAbsolutePath());
				
				cmd.addArg("ip_redirect_driver");
				cmd.addArg(driverFile.getAbsolutePath());
				
				int exit = cmd.execute();
				
				if(exit!=0) {
					throw new IllegalStateException("Could not install redirect driver exitCode=" + exit);
				}
			} catch (IOException e) {
				throw new IllegalStateException("Could not update redirect driver", e);
			}
		}
		
		CommandExecutor startCommand = new CommandExecutor(installCommand.getAbsolutePath());
		startCommand.addArg("ip_redirect_driver");
		startCommand.addArg("start");
		
		int exit;
		try {
			exit = startCommand.execute();
			if(exit!=0 && exit!=1056) {
				throw new IllegalStateException("Could not install redirect driver exitCode=" + exit);
			}	
		} catch (IOException e) {
			throw new IllegalStateException("Could not execute driver start command", e);
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				
				CommandExecutor stopCommand = new CommandExecutor(installCommand.getAbsolutePath());
				stopCommand.addArg("ip_redirect_driver");
				stopCommand.addArg("stop");
				
				int exit;
				try {
					exit = stopCommand.execute();
					if(exit!=0) {
						log.error("Could not stop redirect driver exitCode=" + exit);
					}	
				} catch (IOException e) {
					log.error("Could not execute driver stop command", e);
				}
			}
		});
		
	}
	
	@Override
	protected String[] getLoggingArguments(boolean enabled) {
		return new String[] { };
	}

	@Override
	protected String getCommand() {
		return redirectCommand.getAbsolutePath();
	}

	@Override
	protected String[] getStartArguments(String sourceAddr, int sourcePort,
			String destAddr, int destPort) {
		return new String[] { "add", sourceAddr, String.valueOf(sourcePort), destAddr, String.valueOf(destPort) };
	}

	@Override
	protected String[] getStopArguments(String sourceAddr, int sourcePort, String destAddr, int destPort) {
		return new String[] { "remove", sourceAddr, String.valueOf(sourcePort)};
	}

	public static void main(String[] args) {
		new WindowsSocketRedirector();
	}
}
