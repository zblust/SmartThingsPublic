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
	input("tennant_id", "text", required: true, title: "Tennant ID", description: "The tennant ID for OAuth")
    input("client_id", "text", required:true, title:"Client ID",description: "The client ID for OAuth")
    input("client_secret", "text", required:true, title:"Client Secret",description: "The client secret for OAuth")
    input("subscription_id", "text", required:true, title:"Subscription ID",description: "Subscription ID")
    input("resource_group", "text", required:true, title:"Resource Group",description: "Resource Group Name")
    input("vn_name", "text", required:true, title:"VM Name",description: "VM Name")
    
}
 
metadata {
	definition (name: "Azure VM Switch", namespace: "zblust", author: "Zach Blust") {
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
	log.debug "Parsing '${description}'"
    def map = stringToMap(description)
    sendEvent(name:"switch",value:'on')
}
// handle commands
def poll() {
    def tok = getToken();
    def params = [
        uri: "https://management.azure.com/subscriptions/$subscription_id/resourceGroups/$resource_group/providers/Microsoft.Compute/virtualMachines/$vn_name/instanceView?api-version=2017-12-01",
        headers:[Authorization:"Bearer "+tok]
    ]
    try {
        //httpPostJson(params) { resp ->
        httpGet(params) { resp ->
        	//log.debug("here")
            def st = resp.data.statuses[1].displayStatus
            switch (st){
			case "VM deallocating":
			case "VM deallocated":
            	sendEvent(name:"switch",value:'off')
            	break
            case "VM running":
            case "VM starting":
            	sendEvent(name:"switch",value:'on')
            	break
            default:
            	break
            }
            sendEvent(name: 'status', value: st)
            return st
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.error(e.response.data)
        // Wait long enough to make this code unreachable.
    }catch (e) {
        log.error "something went wrong: $e"
    }
    return 
}

def on() {
	log.debug "Executing 'on'"
    def tok = getToken();
    def params = [
        uri: "https://management.azure.com/subscriptions/$subscription_id/resourceGroups/$resource_group/providers/Microsoft.Compute/virtualMachines/$vn_name/start?api-version=2017-12-01",
        headers:[Authorization:"Bearer "+tok]
    ]
    try {
        //httpPostJson(params) { resp ->
        httpPost(params) { resp ->
        	//log.debug("here")
            sendEvent(name:"switch",value:'on')
            sendEvent(name: 'status', value: 'starting')
            return
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.error(e.response.data)
        // Wait long enough to make this code unreachable.
    }catch (e) {
        log.error "something went wrong: $e"
    }
    return

    
	// TODO: handle 'on' command
}

def off() {
	log.debug "Executing 'off'"
    def tok = getToken();
    def params = [
        uri: "https://management.azure.com/subscriptions/$subscription_id/resourceGroups/$resource_group/providers/Microsoft.Compute/virtualMachines/$vn_name/deallocate?api-version=2017-12-01",
        headers:[Authorization:"Bearer "+tok]
    ]
    try {
        //httpPostJson(params) { resp ->
        httpPost(params) { resp ->
        	//log.debug("here")
            sendEvent(name:"switch",value:'off')
            sendEvent(name: 'status', value: 'stopping')
            return
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.error(e.response.data)
        // Wait long enough to make this code unreachable.
    }catch (e) {
        log.error "something went wrong: $e"
    }
    return
}

private String getToken(){
	def params = [
        uri: "https://login.microsoftonline.com/$tennant_id/oauth2/token?api-version=1.0",
        body: [
            grant_type :"client_credentials",
            resource : "https://management.core.windows.net/",
            client_id : "$client_id",
            client_secret : "$client_secret"
        ]
    ]
    try {
        //httpPostJson(params) { resp ->
        httpPost("https://login.microsoftonline.com/$tennant_id/oauth2/token?api-version=1.0","grant_type=client_credentials&resource=https://management.core.windows.net/&client_id=$client_id&client_secret=$client_secret") { resp ->
            // iterate all the headers
            // each header has a name and a value
            //log.(resp.data.expires_on)
            return resp.data.access_token
            
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.error(e.response.data)
        // Wait long enough to make this code unreachable.
    }catch (e) {
        log.error "something went wrong: $e"
    }
}
