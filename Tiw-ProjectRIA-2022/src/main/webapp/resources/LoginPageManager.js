(function(){//iife

    URL_CHECK_LOGIN = "CheckPassword";
    URL_CHECK_REGISTRATION = "CheckRegistration"
    
    //sign-in management
    document.getElementById("LoginButton").addEventListener("click", (e) =>{
        var form = e.target.closest("form");
        var userField = form.elements["username"];
        var pwdField = form.elements["password"];

        //reset style to delete the red border
        userField.style.borderColor = "";
        pwdField.style.borderColor = "";
        
        if(form.checkValidity()){

            makeCall("POST", URL_CHECK_LOGIN, form,
                function(req){
                    var message = req.responseText;
                    if(req.readyState == XMLHttpRequest.DONE){
                        if(req.status == 200){
                            //saving username in session and redirect to another page
                            sessionStorage.setItem('username', message);
                            window.location.href = "homePage.html"
                        }else if(req.status == 401){//incorrect credentials
                            userField.style.borderColor = "red";
                            pwdField.style.borderColor = "red";
                            document.getElementById("errorMessageLogin").textContent = message;
                        }else{
                            document.getElementById("errorMessageLogin").textContent = message;
                        }
                    }
                });
        }else{
            form.reportValidity();
        }
    });
    
    //sign-up management
    document.getElementById("RegistratonButton").addEventListener("click", (e) =>{
        var form = e.target.closest("form");
        var pwdField = form.elements["newPassword"];
        var rpt_pwdField = form.elements["newRepeatedPassword"]
        
        //reset the style to delete the red border
        pwdField.style.borderColor = "";
        rpt_pwdField.style.borderColor = "";

        //password management
        if(pwdField.value == rpt_pwdField.value){

            if(form.checkValidity()){
                makeCall("POST", URL_CHECK_REGISTRATION, form,
                    function(req){
                        var message = req.responseText;
                        if(req.readyState == XMLHttpRequest.DONE){
                            if(req.status == 200){
                                document.getElementById("errorMessageRegistration").textContent = "You have successfully registered , please log in";
                            }else{
                                document.getElementById("errorMessageRegistration").textContent = message;
                            }
                        }
                    });
            }else{
                form.reportValidity();
            }
        }else{
            e.preventDefault();//do not want to clear the form and refresh the page
            document.getElementById("errorMessageRegistration").textContent = "Passwords do not match.";
            pwdField.style.borderColor = "red";
            rpt_pwdField.style.borderColor = "red";
        }   
    });
    

})()