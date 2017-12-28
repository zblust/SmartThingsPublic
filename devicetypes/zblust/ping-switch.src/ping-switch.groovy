/**
 *  Ping Switch
 *
 *  Copyright 2017 Zach Blust
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
preferences {
	input("dest_ip", "text", required: true, title: "IP", description: "The device IP you wish to ping")
    input("dest_port", "text", required: true, title: "Port", description: "The port you wish to connect to to emulate a ping")
    input("macaddress", "text", required: true, title: "Computer MAC Address without :")
    input("secureonpassword", "text", required: false, title: "SecureOn Password (Optional)")
}
 
metadata {
	definition (name: "Ping Switch", namespace: "zblust", author: "Zach Blust") {
		capability "Polling"
		capability "Refresh"
		capability "Switch"
		
		attribute "ttl", "string"
        attribute "last_request", "number"
        attribute "last_live", "number"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "switch.on",
                  icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label: '${currentValue}', action: "switch.off",
                  icon: "st.switches.switch.on", backgroundColor: "#00a0dc"
        }
        standardTile("refresh", "device.ttl", inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
        valueTile("ttl", "device.ttl", inactiveLabel: false, decoration: "flat") {
            state "ttl", label:'${currentValue}'
        }
		main "switch"
        	details(["switch", "refresh","ttl"])
		}
	}
def updated() {
	unschedule()
	runEvery10Minutes(poll)
	runIn(2, poll)
}
// parse events into attributes
def parse(description) {
	log.debug "Parsing '${description}'"
    def map = stringToMap(description)
    def c = new GregorianCalendar()
    sendEvent(name:"switch",value:'on')
    sendEvent(name: 'last_live', value: c.time.time)
    def ping = ttl()
    sendEvent(name: 'ttl', value: ping)
    log.debug "Pinging ${device.deviceNetworkId}: ${ping}"   
}
private ttl() { 
    def last_request = device.latestValue("last_request")
    if(!last_request) {
    	last_request = 0
    }
    def last_alive = device.latestValue("last_live")
    if(!last_alive) { 
    	last_alive = 0
    }
    def last_status = device.latestValue("switch")
    
    def c = new GregorianCalendar()
    def ttl = c.time.time - last_request
    if(ttl > 10000 || last_status == "off") { 
    	ttl = c.time.time - last_alive
    }
    
    def units = "ms"
    if(ttl > 10*52*7*24*60*60*1000) { 
    	return "Never"
    }
    else if(ttl > 52*7*24*60*60*1000) { 
        ttl = ttl / (52*7*24*60*60*1000)
        units = "y"
    }
    else if(ttl > 7*24*60*60*1000) { 
        ttl = ttl / (7*24*60*60*1000)
        units = "w"
    }
    else if(ttl > 24*60*60*1000) { 
        ttl = ttl / (24*60*60*1000)
        units = "d"
    }
    else if(ttl > 60*60*1000) { 
        ttl = ttl / (60*60*1000)
        units = "h"
    }
    else if(ttl > 60*1000) { 
        ttl = ttl / (60*1000)
        units = "m"
    }
    else if(ttl > 1000) { 
        ttl = ttl / 1000
        units = "s"
    }  
	def ttl_int = ttl.intValue()
	"${ttl_int} ${units}"
}
// handle commands
def poll() {
	//log.debug "Poll"
	def hosthex = convertIPToHex(dest_ip).toUpperCase()
    def porthex = Long.toHexString(Long.parseLong(dest_port)).toUpperCase()
    if (porthex.length() < 4) { porthex = "00" + porthex }
		device.deviceNetworkId = "$hosthex:$porthex" 
    
   log.debug "The DNI configured is $device.deviceNetworkId"

    def hubAction = new physicalgraph.device.HubAction(
    	method: "GET",
    	path: "/"
    )       
    
  
	def last_request = device.latestValue("last_request")
    def last_live = device.latestValue("last_live")
    //log.debug "setup?"
    //log.debug last_live
    //log.debug last_request
    if(!last_request) {
    	last_request = 0
    }
    if(!last_live) {
    	last_live = 0
    }

	def c = new GregorianCalendar()
    //log.debug "off?"
    //log.debug last_live
    //log.debug last_request
    if(last_live < last_request) { 
    	sendEvent(name:"switch",value:'off')
        sendEvent(name: 'ttl', value: ttl())
    }
    sendEvent(name: 'last_request', value: c.time.time)

  	log.debug 'hubact'     
  	//log.debug hubAction
    
	//sendHubCommand(hubAction)
    return hubAction
}

def on() {
	log.debug "Executing 'on'"
    sendEvent(name:"switch",value:'on')
    if(secureonpassword){
    	//if a secure password exists
        //creates a new physicalgraph.device.hubaction
        def result = new physicalgraph.device.HubAction (
        	"wake on lan $macaddress",
        	physicalgraph.device.Protocol.LAN,
        	null,
        	[secureCode: "$secureonpassword"]
        )
        //returns the result
    	return result
    } else {
    	//if no secure password exists
        //creates a new physicalgraph.device.hubaction
    	//log.debug "myWOL: SecureOn Password False"
        //log.debug "myWOL: MAC Address $macaddress"
        def result = new physicalgraph.device.HubAction (
        	"wake on lan $macaddress",
        	physicalgraph.device.Protocol.LAN,
        	null
        )    
        //returns the result
        return result
    }

    
	// TODO: handle 'on' command
}

def off() {
	log.debug "Executing 'off'"
    sendEvent(name:"switch",value:'off')
	// TODO: handle 'off' command
}
private Long convertIntToLong(ipAddress) {
	long result = 0
	def parts = ipAddress.split("\\.")
    for (int i = 3; i >= 0; i--) {
        result |= (Long.parseLong(parts[3 - i]) << (i * 8));
    }

    return result & 0xFFFFFFFF;
}

private String convertIPToHex(ipAddress) {
	return Long.toHexString(convertIntToLong(ipAddress));
}
