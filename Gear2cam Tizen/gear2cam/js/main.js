/*    
 * Copyright (c) 2014 Samsung Electronics Co., Ltd.   
 * All rights reserved.   
 *   
 * Redistribution and use in source and binary forms, with or without   
 * modification, are permitted provided that the following conditions are   
 * met:   
 *   
 *     * Redistributions of source code must retain the above copyright   
 *        notice, this list of conditions and the following disclaimer.  
 *     * Redistributions in binary form must reproduce the above  
 *       copyright notice, this list of conditions and the following disclaimer  
 *       in the documentation and/or other materials provided with the  
 *       distribution.  
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its  
 *       contributors may be used to endorse or promote products derived from  
 *       this software without specific prior written permission.  
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS  
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT  
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR  
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT  
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,  
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT  
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,  
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY  
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT  
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE  
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 
var SAAgent = null;
var SASocket = null;
var CHANNELID = 127;
var ProviderAppName = "Gear2cam (original)";

var error = "";
var timer = 0;
var isClicking = false;

var flashMode = 0;

var firstFrame = false;

function log(text) {
	//console.log(text);
}

function seterror(status, err) {
	if(err === "DEVICE_NOT_CONNECTED") {
		SASocket = null;
		navigator.vibrate(1000);
		alert('Your watch is not connected to your phone');
		disconnect();
		return;
	}
	if(err === "PEER_NOT_FOUND") {
		SASocket = null;
		navigator.vibrate(1000);
		alert('An unexpected error occurred while connecting with your phone. Please launch the app again on exit. If the problem persists, please reboot your Gear 2');
		disconnect();
		return;
	}
	$('#status').html("<a href='#' onclick='showError()'>" + status + "</a>");
	error = "The following error occurred:\n" + err;
	
	navigator.vibrate(1000);
	alert(error);
	disconnect();
	return;
}

var currentHtml;
function settransientmessage(message, duration, backstackMessage) {
	if(backstackMessage) {
		currentHtml = backstackMessage;
	}
	else {
		currentHtml = $('#status').html();
	}
	 
	 $('#status').html(message);
	 if(duration > 1000) {
		 navigator.vibrate(1000);
	 }
	 else {
		 navigator.vibrate(duration);
	 }
	 
	 setTimeout(function() {
		 $('#status').html(currentHtml);
	 }, duration);
}

function onerror(err) {
	log("onerror():" + JSON.stringify(err));
	seterror("Error", err);
}

function ondevicestatus(type, status) 
{
   if (status == "DETACHED") 
   {
	   SASocket = null;
	   navigator.vibrate(1000);
	   alert('Your watch can no longer reach your phone');
	   disconnect();
   }
}

function setFlashMode() {
	flashMode++;
	if(flashMode === 3) {
		flashMode = 0;
	}
	
	switch(flashMode) {
	case 0:
		$('#flash-landscape').attr("src", "flash-auto.png");
		$('#flash-portrait').attr("src", "flash-auto.png");
		break;
	case 1:
		$('#flash-landscape').attr("src", "flash-off.png");
		$('#flash-portrait').attr("src", "flash-off.png");
		break;
	case 2:
		$('#flash-landscape').attr("src", "flash-on.png");
		$('#flash-portrait').attr("src", "flash-on.png");
		break;
	}
	SASocket.sendData(CHANNELID, "FLASH," + flashMode);
}

webapis.sa.setDeviceStatusListener(ondevicestatus);

function showToast(text) {
	log("Show toast called 3");
	$('#toast').html(text);
	$('#toast').fadeIn(400).delay(3000).fadeOut(400);
}

var agentCallback = {
	onconnect : function(socket) {
		log("agentCallback.onconnect()");
		SASocket = socket;
		$('#status').html("Connected");
		SASocket.setSocketStatusListener(function(reason){
			seterror("Connection Lost", err);
			//disconnect();
		});
		fetch();
	},
	onerror : onerror
};

var peerAgentFindCallback = {
	onpeeragentfound : function(peerAgent) {
		log("peerAgentFindCallback.onpeeragentfound()");
		try {
			if (peerAgent.appName === ProviderAppName) {
				SAAgent.setServiceConnectionListener(agentCallback);
				log("requestServiceConnection:" + JSON.stringify(peerAgent));
				SAAgent.requestServiceConnection(peerAgent);
			}
			else {
				seterror("Error", "Unexpected app:" + peerAgent.appName);
			}
		} catch(err) {
			log("peerAgentFindCallback.onpeeragentfound() - Exception:" + err.name + ":" + err.message);
			seterror("Error", err.name + ":" + err.message);
		}
	},
	onerror : function(err) {
		log("peerAgentFindCallback.onerror():" + err);
		seterror("Error", err);
	}
}

function onsuccess(agents) {
	log("onsuccess():" + agents);
	try {
		if (agents.length > 0) {
			SAAgent = agents[0];
			log(JSON.stringify(agents[0]));
			SAAgent.setPeerAgentFindListener(peerAgentFindCallback);
			SAAgent.findPeerAgents();
		} else {
			seterror("Error", "SAAgent not found");
		}
	} catch(err) {
		seterror("Error", err.name + ":" + err.message);
	}
}

var startupTimer;
var startupTimeLeft = 10;
function startCountDown() {
	startupTimeLeft--;
	
	if(startupTimeLeft == 0) {
		return;
	}
	
	document.getElementById("startupCountdown").innerHTML = "" + startupTimeLeft;
	startupTimer = setTimeout(startCountDown, 1000);
}
function connect() {
	log("Connect called");
	document.getElementById("startupCountdown").innerHTML = "10";
	var startupTimeLeft = 10;
	startupTimer = setTimeout(startCountDown, 1000);
	firstFrame = true;
	if (SASocket) {
		alert('Already connected!');
        return false;
    }
	try {
		webapis.sa.requestSAAgent(onsuccess, onerror);
	} catch(err) {
		log("Exception in connect():" + err.name + ":" + err.message);
		seterror("Error", err.name + ":" + err.message);
	}
}

function disconnect() {
	try {
		if (SASocket != null) {
			log("Sent DISCONNECT to phone");
			SASocket.sendData(CHANNELID, "DISCONNECT");
		}
		else {
			log("No socket - Disconnected");
			tizen.power.release("SCREEN");
			tizen.application.getCurrentApplication().exit();
		}
	} catch(err) {
		log("Disconnected (with error)");
		tizen.power.release("SCREEN");
		tizen.application.getCurrentApplication().exit();
	}
}

function share(val) {
	if(val) {
		SASocket.sendData(CHANNELID, "PUBLISH," + filePath);
	}
	
	if($('#sharePortrait').is(":visible")) {
		$('#sharePortrait').fadeOut(500);
	}
	else {
		$('#shareLandscape').fadeOut(500);
	}
}

function registerTouchEvents() {
	if(!hammerRegistered) {
		Hammer(document).on("swipeleft", switchCam);
	    Hammer(document).on("swiperight", switchCam);
	    
	    Hammer(document.getElementById("imgLandscape")).on("swipeleft", switchCam);
	    Hammer(document.getElementById("imgLandscape")).on("swiperight", switchCam);
	    
	    hammerRegistered = true;
	    log("Touch registered!");
	}
}

function unregisterTouchEvents() {
	if(hammerRegistered) {
		Hammer(document).off("swipeleft", switchCam);
	    Hammer(document).off("swiperight", switchCam);
	    
	    Hammer(document.getElementById("imgLandscape")).off("swipeleft", switchCam);
	    Hammer(document.getElementById("imgLandscape")).off("swiperight", switchCam);
	    
	    hammerRegistered = false;
	    log("Touch unregistered!");
	}
	else {
		log("Touch already unregistered!");
	}
}



var previousOrientation = "";
var uploadQueue = 0;
var filePath;
var hammerRegistered = false;
var showingPreview = false;
function onreceive(channelId, data) {
	//TODO:Do something with the data
	//log("RECIEVED:" + data);
	if(data === "DISCONNECTED") {
		SASocket.close();
		unregisterTouchEvents();
		SASocket = null;
		log("Disconnected");
		tizen.power.release("SCREEN");
		tizen.application.getCurrentApplication().exit();
	}
	else if(data === "NOTLOGGEDIN") {
		alert('You must accept the End Users License Agreement (EULA) on the phone app before using Gear2cam!');
		disconnect();
	}
	else if(data === "SCREENOFF") {
		alert('Direct launch (with phone screen off) is not supported on your phone. Your phone screen must be switched on and unlocked before you launch this app.');
		disconnect();
	}
	else if(data.indexOf("ERROR") === 0) {
		var parts = data.split(',');
		if(parts[1] === "ERR_FB_PUBLISH_FAILED") {
			alert('Publishing to Facebook failed. Are you sure your phone is connected to the internet?');
		}
	}
	else if(data.indexOf("CAMERROR") === 0) {
		alert('Error opening camera. Please make sure no other apps are using the camera');
		disconnect();
	}
	else if(data.indexOf("SWITCHEDCAM") == 0) {
		isClicking = false; //This will enable clicking
		settransientmessage("Camera switched", 500, "Connected");
	}
	else if(data === "PUBLISHED") {
		settransientmessage("Published to facebook", 3000);
	}
	else if(data.indexOf("CLICKED") === 0) {
		var parts = data.split(',');
		var isPublishingEnabled = parseInt(parts[1]);
		filePath = parts[2];
		var orientation1 = parts[3];
		var preview = parts[4];
		if(isPublishingEnabled === 1) {
			if(orientation1 === "PORTRAIT") {
				$('#imgSharePortrait').attr("src", "data:image/jpeg;base64," + preview);
				$('#sharePortrait').fadeIn(500);
				$('#statusdiv').width("243px");
			}
			else {
				$('#imgShareLandscape').attr("src", "data:image/jpeg;base64," + preview);
				$('#shareLandscape').fadeIn(500);
				$('#statusdiv').width("320px");
			}
			previousOrientation = orientation1;
		}
		else {
			showingPreview = true;
			if(orientation1 == "PORTRAIT") {
				$('#imgPortrait').attr("src", "data:image/jpeg;base64," + preview);
				
				if(previousOrientation !== orientation1) {
					$('#layoutLandscape').hide();
					$('#statusdiv').width("243px");
					$('#layoutPortrait').fadeIn(1000, function() {
						if(firstFrame) {
							firstFrame = false;
							showToast("Swipe left/right to switch camera");
						}
					});
					
					previousOrientation = orientation1;
				}
			}
			else {
				$('#imgLandscape').attr("src", "data:image/jpeg;base64," + preview);
				
				if(previousOrientation !== orientation1) {
					$('#layoutPortrait').hide();
					$('#statusdiv').width("320px");
					$('#layoutLandscape').fadeIn(1000, function() {
						if(firstFrame) {
							firstFrame = false;
							showToast("Swipe left/right to switch camera");
						}
					});
					previousOrientation = orientation1;
				}
			}
			setTimeout(function() {
				showingPreview = false;
			}, 2000);
		}
		
		///opt/usr/media/DCIM/Camera
		/*
		log("Attempting to save local file!");
			var url = "file:///opt/usr/media/DCIM/Camera";
						tizen.filesystem.resolve(
								url,
							     function(dir){
									//Write file contents
									var fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
									
									var jpeg = dir.createFile("Gear2cam-" + fileName);
									jpeg.openStream("w",
									         function(fs){
									        	 fs.writeBase64(preview);
									        	 fs.close();
									         }, function(e){
									           log("Error " + e.message);
									         }, "UTF-8"
									     );
									var scanUrl = url + "/" + fileName;
									log(scanUrl);
									tizen.content.scanFile(scanUrl);
									//log(fileName);
							     }, function(e){
							       log("Error " + e.message);
							     }, "rw"
							 );
					*/
		
	}
	else if(data.indexOf("CLICKACK") === 0) {
		settransientmessage("Clicked!", 500);
	}
	else if(data.indexOf("FRAME") === 0) {
		if(showingPreview) {
			return;
		}
		var parts = data.split(',');
		var orientation = parts[1];
		var src = parts[2];
		var inQueue = parseInt(parts[3]);
		
		if(inQueue > 0) {
			$('#status').html("Connected (" + inQueue + " uploads left" + ")");
		}
		else {
			if(uploadQueue > 0) {
				$('#status').html("Connected");
				settransientmessage("Upload(s) completed", 1000);
			}
		}
		uploadQueue = inQueue;
		
		$('#layoutInfo').hide();
		
		if(orientation == "PORTRAIT") {
			$('#imgPortrait').attr("src", "data:image/jpeg;base64," + src);
			
			if(previousOrientation !== orientation) {
				$('#statusdiv').width("243px");
				$('#layoutLandscape').hide();
				$('#layoutPortrait').fadeIn(1000, function() {
					if(firstFrame) {
						firstFrame = false;
						showToast("Swipe left/right to switch camera");
					}
				});
				
				previousOrientation = orientation;
			}
		}
		else {
			$('#imgLandscape').attr("src", "data:image/jpeg;base64," + src);
			
			if(previousOrientation !== orientation) {
				$('#statusdiv').width("320px");
				$('#layoutPortrait').hide();
				$('#layoutLandscape').fadeIn(1000, function() {
					if(firstFrame) {
						firstFrame = false;
						showToast("Swipe left/right to switch camera");
					}
				});
				previousOrientation = orientation;
			}
		}
		registerTouchEvents();
	}
}

function fetch() {
	try {
		tizen.power.request("SCREEN", "SCREEN_NORMAL");
		SASocket.setDataReceiveListener(onreceive);
		SASocket.sendData(CHANNELID, "CONNECT");
	} catch(err) {
		seterror("Error", err.name + ":" + err.message);
	}
}

function updateTimerValue(val) {
	$('#timerValue').html(val);
	timer = val;
}

//Check the name of the visibility change event
var hidden,visibilityChange;
if (typeof document.hidden !== "undefined") {
	hidden = "hidden";
	visibilityChange = "visibilitychange";
} else if (typeof document.webkitHidden !== "undefined") {
	hidden = "webkitHidden";
	visibilityChange = "webkitvisibilitychange";
}

function handleVisibilityChange(){
	if (document[hidden]){
		log("Page is now hidden.");
		disconnect();
	} else {
		log("Page is now visible.");
		//connect();
	}
}

function switchCam(){
	if(isClicking) {
		return;
	}
	
	isClicking = true; //This will temporarily disable clicking
	$('#status').html("Switching camera");
	SASocket.sendData(CHANNELID, "SWITCHCAM");
	log("Switching camera");
}

window.onload = function () {
    // add eventListener for tizenhwkey
    document.addEventListener('tizenhwkey', function(e) {
        if(e.keyName == "back") {
        	log("Back");
        	disconnect();
        }
    });
    
    document.addEventListener(visibilityChange, handleVisibilityChange, false);
    connect();
};

function showError() {
	if(!error) {
		alert("Your device is connecting. Thank you for your patience.");
		return;
	}
	alert(error);
}

function countdown() {
	if(timer === 0) {
		isClicking = false;
		timer = $('input[name=timer]').val();
		var currentMessage = $('#status').html();
		if(currentMessage === "Clicked!") {
			currentHtml = "Connected";
		}
		else {
			currentHtml = "Connected";
			 $('#status').html(currentHtml);
		}
		currentHtml = "Connected";
		return;
	}
	
	timer--;
	$('#status').html("Clicking in " + timer + " sec");
	navigator.vibrate(200);
	setTimeout(countdown, 1000);
}

var isTimerShown = false;

function showTimer() {
	if(!isTimerShown) {
		$('#timerValues').fadeIn(1000);
		isTimerShown = true;
	}
	else {
		$('#timerValues').fadeOut(1000);
		isTimerShown = false;
	}
}

function clickPhoto(now) {
	if(error) {
		alert(error);
	}
	else if(isClicking) {
		return;
	}
	else {
		if(SASocket) {
			if(!now) {
				var timerString = $('input[name=timer]:checked').val();
				
				if(timerString) {
					timer = parseInt(timerString);
				}
				else {
					alert("You must select a timer value");
					return;
				}
				
				//$('input[name=timer]:checked').val(2);
			}
			else {
				timer = 0;
			}
			
			
			if(timer > 0) {
				isClicking = true;
				setTimeout(countdown, 1000);
			}
			
			log("CLICK," + timer);
			SASocket.sendData(CHANNELID, "CLICK," + timer);
			
			if(!now) {
				showTimer();
			}	
		}
		else {
			alert("Could not connect.");
		}
	}
}
