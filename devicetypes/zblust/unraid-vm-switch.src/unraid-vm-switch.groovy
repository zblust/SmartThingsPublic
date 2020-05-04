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
	input("dest_ip", "text", required: true, title: "IP", description: "The device IP")
    input("dest_port", "text", required: true, title: "Port", description: "The port")
    input("vm_guid", "text", required: true, title: "Guid", description: "The vm guid")
	input("password", "text", required: true, title: "Password", description: "The password")
    input("off_action", "text", required: true, title: "Off Action", description: "The action to take for off")
}
 
metadata {
	definition (name: "Unraid VM Switch", namespace: "zblust", author: "Zach Blust") {
		capability "Polling"
		capability "Refresh"
		capability "Switch"
		
		attribute "status", "string"
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
        standardTile("refresh", "device.status", inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
        valueTile("status", "device.status", inactiveLabel: false, decoration: "flat") {
            state "status", label:'${currentValue}'
        }
		main "switch"
        	details(["switch", "refresh","status"])
		}
	}
def updated() {
	unschedule()
	runEvery10Minutes(poll)
	runIn(2, poll)
}
// parse events into attributes
def parse(description) {
    //log.debug "Parsing '${description}'"
    //def map = stringToMap(description)
    def msg = parseLanMessage(description);
    //log.debug msg
    //log.debug "zach"
    //log.debug msg.json
    //def results = new groovy.json.JsonSlurper().parseText(msg.json)
    def stat = msg.json.servers["${dest_ip}"].vm.details["${vm_guid}"].status
    sendEvent(name: 'status', value: stat)
    switch (stat){
			case "stopped":
            case "paused":
            	sendEvent(name:"switch",value:'off')
            	break
            case "started":
            	sendEvent(name:"switch",value:'on')
            	break
            default:
            	break
            }
}
def GetVMStatus(){
	def hosthex = convertIPToHex(dest_ip).toUpperCase()
    def porthex = Long.toHexString(Long.parseLong(dest_port)).toUpperCase()
    if (porthex.length() < 2) { porthex = "000" + porthex }
    if (porthex.length() < 3) { porthex = "00" + porthex }
    if (porthex.length() < 4) { porthex = "0" + porthex }
		device.deviceNetworkId = "$hosthex:$porthex" 
    
    //log.debug "The DNI configured is $device.deviceNetworkId"
	//log.debug 'get status'
    try {
        def result = new physicalgraph.device.HubAction(
            method: "GET",
            path: "/api/getServers",
            headers: [
                HOST: "$dest_ip:$dest_port"
            ],
            body: []
        )
        log.debug(result)

        //def msg = parseLanMessage(result)
        //log.debug msg.json
        //log.debug 'enter return'
        return result
    }
    catch (Exception e) {
    	log.debug "Hit Exception $e on $hubAction"
    }
}
// handle commands
def poll() {
    log.debug "Poll"
    GetVMStatus()
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


def on() {
	log.debug "Executing 'on'" 
    def lastStat=device.latestValue("status")
    def action = "domain-start"
    if(lastStat == "paused"){
    	action = "domain-resume"
    }
	sendEvent(name:"switch",value:'on')
	def result = new physicalgraph.device.HubAction(
		method: "POST",
		path: "/api/vmStatus",
		headers: [
			HOST: "$dest_ip:$dest_port"
		],
		body: [ id: "${vm_guid}", action: "${action}", server: "${dest_ip}",auth: "${password}"]
	)
    GetVMStatus()
	return result

}

def off() {
	log.debug "Executing 'off'"
    sendEvent(name:"switch",value:'off')
	// TODO: handle 'off' command
	def result = new physicalgraph.device.HubAction(
		method: "POST",
		path: "/api/vmStatus",
		headers: [
			HOST: "$dest_ip:$dest_port"
		],
		body: [ id: "${vm_guid}", action: "${off_action}", server: "${dest_ip}",auth: "${password}"]
	)
    GetVMStatus()
	return result
}

