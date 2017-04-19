URMS Integration
================

This document outlines the changes we've made to the NYPL SimplyE app to support URMS integration.

_ReaderActivity.java onCreate():_

We first read sample.epub from the assets directory and write it to the internal storage. This happens only once, the first time the app is run. On subsequent runs, sample.epub is read from the internal storage.

We specify the book CCID and obtain its path:

    final String bookCCID = "NHG6M6VG63D4DQKJMC986FYFDG5MDQJE";
    final String bookUri = in_epub_file.getAbsolutePath();
    
Next, we create a new thread and prepare the service call to fetch the authorization token from URMS.

    String userID = "google-110495186711904557779";
    String path = "/store/v2/users/" + userID + "/authtoken/generate";
    String sessionURL = "http://urms-967957035.eu-west-1.elb.amazonaws.com" + path;
    String timestamp = Long.toString(System.currentTimeMillis() / 1000);
    String hmacMessage = path + timestamp;
    String secretKey = "ucj0z3uthspfixtba5kmwewdgl7s1prm";
    // ...
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secret = new SecretKeySpec(key.getBytes("UTF-8"), mac.getAlgorithm());
    mac.init(secret);
    byte[] digest = mac.doFinal(base_string.getBytes());
    authHash = Base64.encodeToString(digest, Base64.DEFAULT);
    
We fetch the authToken from the response:

    final String authToken = responseJson.getString("authToken");
    
At this point we create the URMS profile using the authToken.

    Urms.createCreateProfileTask(authToken, profileName, null, config)

When the createProfile task succeeds, we perform a getProfiles task and switch to the created profile.

    profiles = Urms.createGetProfilesTask().getResultWithExecute().getProfiles();
        if (profiles.size() > 0) {
            Urms.createSwitchProfileTask(profiles.get(0)).getResultWithExecute();
            
Once we have switched to the profile, we call evaluateURMSLicense().

This will fail because we have not yet registered the book.

We create a getOnlineBooksTask, which is not strictly necessary but is a good debugging test to see if it works. If there are errors here, for instance, a NotPermitted error, it means that we do not have permissions for this particular app from Denuvo. In that case, we must contact Denuvo and send them the signature of the Android application, using the keytool -printcert command to obtain this.

Once getOnlineBooksTask succeds, we perform RegisterBookTask:

    RegisterBookTask rbt = Urms.createRegisterBookTask(bookCCID);
 
When this succeds, we call evaluateURMSLicense() once more, and it should succeed now because we have registered the book.

