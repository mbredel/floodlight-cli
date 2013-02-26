package net.floodlightcontroller.cli.commands;

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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.cli.IConsole;
import net.floodlightcontroller.cli.utils.StringTable;

/**
 * The "show host" command shows information about hosts
 * that are connected to switches controlled by Floodlight.
 * 
 * The "show host" command uses the Floodlight.context service
 * to directly address the corresponding Floodlight module to
 * retrieve the needed information.
 * 
 * @author Michael Bredel <michael.bredel@cern.ch>
 */
public class ShowHostCmd implements ICommand  {
	/** Floodlight Context Service. */
	private FloodlightModuleContext context;
	/** The command string. */
	private String commandString = "show host";
	/** The command's arguments. */
	private String arguments = null;
	/** The command's help text. */
	private String help = null;
	
	/**
	 * Constructor.
	 * 
	 * @param context The Floodlight context service.
	 */
	public ShowHostCmd(FloodlightModuleContext context) {
		this.context = context;
	}
	
	@Override
	public String getCommandString() {
		return commandString;
	}
	
	@Override
	public String getArguments() {
		return arguments;
	}

	@Override
	public String getHelpText() {
		return help;
	}

	@Override
	public String execute(IConsole console, String arguments) {
		/* The resulting string. */
		String result = "";
		/* The Floodlight device manager to access Floodlight's device information. */
		IDeviceService deviceManager = context.getServiceImpl(IDeviceService.class);
		/* All devices connected to Floodlight-controlled switches. */
		Collection<? extends IDevice> devices = deviceManager.getAllDevices();
		
		try {
			result = this.devicesToTableString(devices);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * Creates a string table and returns a formated string that
	 * shows the device information as a table.
	 * 
	 * @param devices A collection of devices.
	 * @return A formated string that shows the device information as a table.
	 * @throws IOException
	 */
	private String devicesToTableString(Collection<? extends IDevice> devices) throws IOException {
		/* The string table that contains all the device information as strings. */
        StringTable stringTable = new StringTable();
        
        // Generate header data.
        List<String> header = new LinkedList<String>();
        header.add("MAC Address");
        header.add("VLAN");
        header.add("Vendor");
        header.add("IP Address");
        header.add("Switch/OF Port (Physical Port)");
        header.add("Tag");
        header.add("Last Seen");
        
        // Add header to string table.
        stringTable.setHeader(header);

		// Generate table entries and add them to string table.
		for (IDevice entry : devices) {
			List<String> row = new LinkedList<String>();
			row.add(entry.getMACAddressString());
			row.add(entry.getVlanId().toString());
			row.add("unknown");
			row.add(entry.getIPv4Addresses()[0].toString());
			row.add(entry.getAttachmentPoints().toString());
			row.add("");
			row.add(this.parseDate(entry.getLastSeen()));

			stringTable.addRow(row);
		}
        
		// Return string table as a string.
        return stringTable.toString();
	}
	
	/**
	 * Parses a date and returns a formated date string in the
	 * form: "yyyy-MM-dd HH:mm:ss z", where z is the time zone.
	 * 
	 * @param date The date that needs to be converted into a formated string.
	 * @return A formated date string.
	 */
	private String parseDate(Date date) {
		SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		return dateformat.format(date);
	}

}
