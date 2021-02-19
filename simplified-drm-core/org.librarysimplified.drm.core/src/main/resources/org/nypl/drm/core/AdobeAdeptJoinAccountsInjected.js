(function () {

  window.onAdobeFormSubmit = function (url) {
    var form = window.adobeForm;
    var elements = form.elements;

    var params = {};
    params["url"] = window.adobeActionURL;
    params["username"] = form.username.value;
    params["password"] = form.password.value;
    params["sessionId"] = form.sessionId.value;
    params["currentNonce"] = form.currentNonce.value;
    params["locale"] = form.elements['locale'].value;

    window.location.href = "adobe:join-form-submit/" + encodeURIComponent (JSON.stringify (params));
  };

  console.log("injected javascript starting for location " + window.location.href);
  console.log("removing cancel button");
  document.getElementById('form-fields').removeChild(document.getElementsByClassName('closeBtn')[0]);
  console.log("injecting js into form button");
  window.adobeForm = document.getElementsByTagName ("form")[0];
  console.log("window.adobeForm: " + window.adobeForm);
  window.adobeActionURL = window.adobeForm.action;
  console.log("window.adobeActionURL: " + window.adobeActionURL);
  window.adobeForm.action = "javascript:onAdobeFormSubmit ()";
})()
