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
	input("on_code", "text", required: true, title: "On Code", description: "The on code")
	input("off_code", "text", required: true, title: "Off Code", description: "The off code")
	input("password", "text", required: true, title: "Password", description: "The password")
}
 
metadata {
	definition (name: "Network IR Switch", namespace: "zblust", author: "Zach Blust") {
		capability "Switch"		
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
		main "switch"
        	details(["switch"])
		}
	}
def updated() {
	unschedule()
}
// parse events into attributes
def parse(description) {
	log.debug "Parsing '${description}'"
    //def map = stringToMap(description)
    //def c = new GregorianCalendar()
    sendEvent(name:"switch",value:'on')
}
// handle commands
def on() {
	log.debug "Executing 'on'" 
	sendEvent(name:"switch",value:'on')
	def result = new physicalgraph.device.HubAction(
		method: "GET",
		path: "/msg",
		headers: [
			HOST: "$dest_ip:$dest_port"
		],
		query: [code: "${on_code}", pass: "${password}",pulse:3,repeat:3,simple:"1"]
	)
	return result

}

def off() {
	log.debug "Executing 'off'"
    sendEvent(name:"switch",value:'off')
	// TODO: handle 'off' command
	def result = new physicalgraph.device.HubAction(
		method: "GET",
		path: "/msg",
		headers: [
			HOST: "$dest_ip:$dest_port"
		],
		query: [code: "${off_code}", pass: "${password}",pulse:3,repeat:3,simple:"1"]
	)
	return result
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