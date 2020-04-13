org.librarysimplified.cardcreator
===

The Card Creator module holds functionality for users to create library cards from within the app.  
The implementation uses modern Android APIs like ViewModels, ViewBinding, Coroutines, Navigation Components, LiveData, and Retrofit/Moshi for networking.  
To sum it up simply, the Card Creator simply validates users input along a wizard as 3 endpoints are called to created a new library card. Those endpoints in order are:  
1.  [v2/validate/address](https://github.com/NYPL-Simplified/card-creator/wiki/API---V2#post-v2validateaddress)  
2. [v2/validate/username](https://github.com/NYPL-Simplified/card-creator/wiki/API---V2#post-v2validateusername)  
3. [v2/create_patron](https://github.com/NYPL-Simplified/card-creator/wiki/API---V2#post-v2create_patron)
