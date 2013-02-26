package net.floodlightcontroller.cli;

/*
* Copyright (c) 2013, California Institute of Technology
* ALL RIGHTS RESERVED.
* Based on Government Sponsored Research DE-SC0007346
* Author Michael Bredel <michael.bredel@cern.ch>
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
* "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
* LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
* A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
* HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
* BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
* OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
* AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
* LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
* WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
* 
* Neither the name of the California Institute of Technology
* (Caltech) nor the names of its contributors may be used to endorse
* or promote products derived from this software without specific prior
* written permission.
*/

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.restserver.IRestApiService;

import net.floodlightcontroller.cli.commands.*;

/**
 * Command Line Interface (CLI) to Floodlight. The CLI module 
 * offers a Cisco-like CLI to FLoodlight. You can log on to the
 * CLI using an SSH client.
 * 
 * @author Michael Bredel <michael.bredel@cern.ch>
 */
public class Cli implements IFloodlightModule {
	/** Default port of the SSH config console. */
	private static final int DEFAULT_PORT = 55220;
	/** Default user name: root. */
	private static final String DEFAULT_USERNAME = "root";
	/** Default password: password. */
	private static final String DEFAULT_PASSWORD = "password";	
	/** Default SSH host key location. */
	private static final String DEFAULT_HOSTKEY = "ssh_host_dsa_key.pub";
	/** Logger to log ProactiveFlowPusher events. */
	protected static Logger logger = LoggerFactory.getLogger(Cli.class);
	/** Ports used by the SSH server to offer the console login. */
	protected int port;
	/** User name to log in to the console. */
	protected String username;
	/** Password to log in to the console. */
	protected String password;
	/** Host key file where SSHD stores the host key. */
	protected String hostkey;
	/** The command handler that executes CLI commands. */
	protected CommandHandler commander;
	/** Required Module: Floodlight Provider Service. */
	protected IFloodlightProviderService floodlightProvider;
	/** Required Module: Reference to the REST API service. */
	protected IRestApiService restApi;
	/** Required Module: Floodlight Device Manager Service.*/
	protected IDeviceService deviceManager;

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
	    l.add(IRestApiService.class);
	    l.add(IDeviceService.class);
	    return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		restApi = context.getServiceImpl(IRestApiService.class);
		deviceManager = context.getServiceImpl(IDeviceService.class);
		
		// Read our configuration from properties file.
		this.readConfig(context);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		// Initialize command handler;
		commander = CommandHandler.getInstance();
		// Add commands to handler.
		commander.addCommand(new ExitCmd());
		commander.addCommand(new ShowCmd());
		commander.addCommand(new ShowSwitchCmd());
		commander.addCommand(new ShowHostCmd(context));
		
		// Initialize the SSH server.
		SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setPort(this.port);
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(this.hostkey, "DSA"));
		sshd.setPasswordAuthenticator(new SimplePasswordAuthenticator(this.username, this.password));
		sshd.setShellFactory(new FloodlightShellFactory());
		
		// Start the SSH server.
		try {
			sshd.start();
			Cli.logger.info("Starting config console (via SSH) on port {}", this.port);
		} catch (IOException e) {
			Cli.logger.error("Starting config console (via SSH) on port {} failed", this.port);
			e.printStackTrace();
		}
	}
	
	/**
	 * Reads the configuration for this module from properties file "floodlightdefaults.propertiers".
	 */
	private void readConfig(FloodlightModuleContext context) {
		// Read the configure options.
        Map<String, String> configOptions = context.getConfigParams(this);
        
        this.port = (configOptions.get("port") != null) ? Integer.parseInt(configOptions.get("port")) : DEFAULT_PORT;
        this.username = (configOptions.get("username") != null) ? configOptions.get("username") : DEFAULT_USERNAME;
        this.password = (configOptions.get("password") != null) ? configOptions.get("password") : DEFAULT_PASSWORD;
        this.hostkey = (configOptions.get("hostkey") != null) ? configOptions.get("hostkey") : DEFAULT_HOSTKEY;
	}

}
