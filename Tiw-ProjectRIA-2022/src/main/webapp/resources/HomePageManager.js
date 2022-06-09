(function(){//ifee
    //page components
    var pageOrchestrator = new PageOrchestrator();//clientside controller
    var meetingTable, meetingCreator;

    //Servlet URL
    const URL_MEETING_LIST = "GetMeetingListData";
    const URL_USER_INVITABLE_LIST = "GetRegisteredUserData";
    const URL_CREATE_MEETING = "CreateMeeting";

    const MAX_ATTEMPT = 3; 

    window.addEventListener("load", ()=>{
        if(sessionStorage.getItem("username") == null){
            window.location.href = "loginPage.html";
        }else{//display initial content
            pageOrchestrator.start();
            pageOrchestrator.refresh()
        }
        
    } , false)


    function MeetingTable(_meetingTable, _meetingCreatedTableBody, _meetingInvitedTableBody, _messageContainer){
        this.messageContainer = _messageContainer;

        this.meetingTable = _meetingTable;
        this.meetingCreatedTableBody = _meetingCreatedTableBody;
        this.meetingInvitedTableBody = _meetingInvitedTableBody;

        this.reset = function(){
            this.meetingTable.style.visibility = "hidden";
        };

        this.show = function(){
            var self = this;

            makeCall("GET", URL_MEETING_LIST, null,
                function(req){
                    if(req.readyState == XMLHttpRequest.DONE){
                        var message = req.responseText;

                        if(req.status == 200){
                            var meetingListToShow = JSON.parse(message);
                            
                            if(meetingListToShow.length == 0){
                                self.messageContainer.textContent = "No meetings yet!";
                                return;
                            }
                            self.update(meetingListToShow);
                            
                        }else if(req.status == 403){//user not allowed
                            window.location.href = req.getResponseHeader("Location");
                            window.sessionStorage.removeItem("username");
                        }else{
                            self.messageContainer.textContent = message;
                        }
                    }
                }
            );

        };

        this.update = function(meetingArray){
            var row, titleCell, dateCell, timeCell, durationCell, maxParticipantCell;

            //empty the tables
            this.meetingCreatedTableBody.innerHTML = "";
            this.meetingInvitedTableBody.innerHTML = "";

            //build updated tables
            var self = this;
            meetingArray.forEach(function(meeting) {
                //create a row for each meeting
                row = document.createElement("tr");
                row.setAttribute("bgcolor", "lightGrey");
                
                //create a cell containing the title
                titleCell = document.createElement("td");
                titleCell.textContent = meeting.title;
                row.appendChild(titleCell);

                //create a cell containing the date
                dateCell = document.createElement("td");
                dateCell.textContent = meeting.date;
                row.appendChild(dateCell);

                //create a cell containing the time
                timeCell = document.createElement("td");
                timeCell.textContent = meeting.time;
                row.appendChild(timeCell);
                
                //create a cell containing the duration
                durationCell = document.createElement("td");
                durationCell.textContent = meeting.duration;
                row.appendChild(durationCell);
                
                //create a cell containing the maxParticipant
                maxParticipantCell = document.createElement("td");
                maxParticipantCell.textContent = meeting.maxParticipant;
                row.appendChild(maxParticipantCell);

                //append the row to the correct table
                meeting.creator ? (self.meetingCreatedTableBody.appendChild(row)) : (self.meetingInvitedTableBody.appendChild(row));
                
            });

            this.meetingTable.style.visibility = "visible";

            //build message to show if one of the table has no rows
            let noMeetingRow = document.createElement("td");
            noMeetingRow.setAttribute("colspan", "5");
            noMeetingRow.setAttribute("bgcolor", "lightGrey");

            if(this.meetingCreatedTableBody.childElementCount == 0){
                noMeetingRow.textContent = "You have not created a meeting yet";
                this.meetingCreatedTableBody.appendChild(noMeetingRow);
            }

            if(this.meetingInvitedTableBody.childElementCount == 0){
                noMeetingRow.textContent = "You have not been invited to a meeting yet";
                this.meetingInvitedTableBody.appendChild(noMeetingRow);
            }
        };

    }

    function MeetingCreator( _meetingFormContainer, _modalWindowContainer, _messageContainer){
        this.messageContainer = _messageContainer;

        this.meetingFormContainer = _meetingFormContainer;
        this.modalWindowContainer = _modalWindowContainer;
        this.meetingForm = this.meetingFormContainer.querySelector("form");

        var savedMeeting; //meeting to be saved in the db
        var numAttempts = 0;
        
        /*** CLIENTSIDE CONTROLS ***/
        //cannot select a date in the past
        var today = new Date();
        var formattedDate = today.toISOString().substring(0, 10); //to get yyyy/mm/dd
        this.meetingForm.querySelector('input[type="date"]').setAttribute("min", formattedDate);

        //select a positive duration/maxParticipant
        this.meetingForm.querySelector('input[name="duration"]').setAttribute("min", 1);
        this.meetingForm.querySelector('input[name="maxPart"]').setAttribute("min", 1);



        this.show = function(){
            var self = this;

            makeCall("GET", URL_USER_INVITABLE_LIST, null,
                function(req){
                    if(req.readyState == XMLHttpRequest.DONE){
                        var message = req.responseText;

                        if(req.status == 200){
                            var selectableUsers = JSON.parse(message);
                            
                            if(selectableUsers == 0){
                                self.messageContainer.textContent = "No users to invite!";
                                return;
                            }
                            self.update(selectableUsers);
                            
                        }else if(req.status == 403){//user not allowed
                            window.location.href = req.getResponseHeader("Location");
                            window.sessionStorage.removeItem("username");
                        }else{
                            self.messageContainer.textContent = message;
                        }
                    }
                }
            );
        };
        
        this.update = function(userArray){
            var divMW = document.getElementById("divMW");
            var input, label, br;

            divMW.innerHTML = ""; //empty the checkboxes
            userArray.forEach(function(user){
                

                //create an input for each user
                input = document.createElement("input");
                input.setAttribute("name", user.username);
                input.setAttribute("type", "checkbox");
                input.setAttribute("value", user.idUser);
                divMW.appendChild(input);
                
                //create a label with the name for each user
                label = document.createElement("label");
                label.innerText = user.username;
                divMW.appendChild(label);

                //create a br tag
                br = document.createElement("br");
                divMW.appendChild(br);
                }
            );

            this.modalWindowContainer.style.visibility = "visible";

        };

        this.reset = function(){
            this.meetingFormContainer.style.visibility = "visible";
            this.meetingFormContainer.querySelector("form").reset();

            this.modalWindowContainer.style.visibility = "hidden"
        };

        this.dismissCreation = function(){
            savedMeeting = null; //cancel meeting
            numAttempts = 0; //reset num of attempts
        }

        this.registerEvents = function(orchestrator){
            //manage create button
            this.meetingForm.querySelector('input[name="CreateButton"]')
            .addEventListener("click", (e) => {
                    this.meetingForm.querySelector('input[name="time"]').style.borderColor = ""; //reset border

                    //time cannot be in the past
                    let formattedTime = today.getHours() + ":" + today.getMinutes();
                    let selectedTime = this.meetingForm.querySelector('input[name="time"]').value;
                    let selectedDate = this.meetingForm.querySelector('input[name="date"]').value;

                    if(this.meetingForm.checkValidity()){
                        if(selectedDate != formattedDate || selectedTime >= formattedTime){
                            e.preventDefault();
                            this.meetingFormContainer.style.visibility = "hidden";

                            //save a copy of the meeting
                            let deep = true;
                            savedMeeting = e.target.closest("form").cloneNode(deep);
                            
                            this.show();//show the users that can be selected

                        }else{
                            e.preventDefault();//do not want to clear the form and refresh the page
                            this.meetingForm.querySelector('input[name="time"]').style.borderColor = "red";
                            this.messageContainer.innerText = "Time cannot be in the past";
                        }

                    }else{
                        this.meetingForm.reportValidity();
                    }

                }, false);

            this.modalWindowContainer.querySelector('input[name="InviteButton"]')
            .addEventListener("click", (e) => {
                    var errorMessageMW = document.getElementById("errorMessageMW");
                    errorMessageMW.innerText = "";

                    var MWform = e.target.closest("form"); 
                    var elementSelected = MWform.querySelectorAll('input:checked'); //css selector
                    var numberSelected = elementSelected.length; 
                    var maxParticipant = savedMeeting.querySelector('input[name="maxPart"]').value;

                    if(numberSelected <= 0){
                        errorMessageMW.innerText = "Select at least one user to invite!";
                    }else if ( numberSelected > maxParticipant){
                        ++numAttempts;
                        if(numAttempts < MAX_ATTEMPT){
                            e.preventDefault();
                            errorMessageMW.innerText = "Too many users selected, delete at least " + (numberSelected - maxParticipant);
                        }else{
                            this.dismissCreation();
                            orchestrator.refresh(); 
                            this.messageContainer.innerText = "Three attempts to define a meeting with too many participants, the meeting will not be created."
                        }
                        
                    }else{
                        var dataToServer = savedMeeting
                        var inputNumAttempts;
                        //adding users selected to the copy of the form
                        elementSelected.forEach(function(element){
                            dataToServer.appendChild(element);
                        });

                        //adding number of attempts as field of the form
                        inputNumAttempts =  document.createElement("input");
                        inputNumAttempts.setAttribute("name", "numAttempts")
                        inputNumAttempts.setAttribute("type", "number")
                        inputNumAttempts.setAttribute("value", numAttempts);
                        dataToServer.appendChild(inputNumAttempts);

                        var self = this;
                        makeCall("POST", URL_CREATE_MEETING, dataToServer, 
                        function(req){
                            if(req.readyState == XMLHttpRequest.DONE){
                                var message = req.responseText;
                                if(req.status == 200){
                                    orchestrator.refresh();//id of the new mission passed
                                }else if(req.status == 403){
                                    window.location.href = req.getResponseHeader("Location");
                                    window.sessionStorage.removeItem("username");
                                }else{
                                    self.messageContainer.innerText = message;
                                    self.reset();
                                }

                            }
                        });
                    }
                }, false);

            this.modalWindowContainer.querySelector('input[name="CancelButton"]')
            .addEventListener("click", (e) => {
                this.dismissCreation();
                orchestrator.refresh(); 
                this.messageContainer.innerText = "Meeting canceled.";
                        
            }
            , false);
            
        };

    }

    function PageOrchestrator(){
        messageContainer = document.getElementById("messageContainer");

        this.start = function(){

            meetingTable = new MeetingTable(document.getElementById("meetingTable"),
                                            document.getElementById("meetingCreatedTableBody"),
                                            document.getElementById("meetingInvitedTableBody"),
                                            messageContainer);

            //management of meeting creation
            meetingCreator = new MeetingCreator(document.getElementById("meetingFormContainer"),
                                                document.getElementById("modalWindowContainer"),
                                                messageContainer);

            meetingCreator.registerEvents(this);

            //management of logout button
            document.querySelector("a[href='Logout']").addEventListener("click", 
                () =>{
                    window.sessionStorage.removeItem("username");
                });
        };

        this.refresh = function(){
            messageContainer.textContent = "";

            meetingTable.reset();
            meetingTable.show();

            meetingCreator.reset();
        };
    }

})()