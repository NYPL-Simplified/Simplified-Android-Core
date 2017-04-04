package bclurms;

import com.sonydadc.urms.android.Urms;
import com.sonydadc.urms.android.UrmsError;

public class UrmsEvaluateLicenseRequest {

    public UrmsError execute(String ccid) {
        return Urms.createEvaluateLicenseTask(ccid).executeSync();
    }


//    public static void evaluateURMSLicense(final String bookCCID, final String bookUri, final Book book, final Context mContext, final Cursor bookCursor) { 
//        Log.e(TAG, "[evaluateURMSLicense] bookCCID = " + bookCCID);  
//        UrmsEvaluateLicenseRequest evaluateLicenseRequest = new UrmsEvaluateLicenseRequest(); 
//        UrmsError error = evaluateLicenseRequest.execute(bookCCID); 
//        if (error.isError()) { Log.e(TAG, "[evaluateURMSLicense] Registering book...");  
//            UrmsRegisterBookRequest registerBookRequest = new UrmsRegisterBookRequest();  
//            ISucceededCallback succeededCallback = new ISucceededCallback<EmptyResponse>() {
//                 @Override 
//
//                public void onSucceeded(IUrmsTask task, EmptyResponse result) { 
//                    Log.e(TAG, "Register book task succeeded."); 
//                    evaluateURMSLicense(bookCCID, bookUri, book, mContext, bookCursor); // Call self after registerBookTask succeeds, to evaluate again 
//                }
//
//                 
//            };
//
//              IFailedCallback failedCallback = new IFailedCallback() {
//                 @Override 
//
//                public void onFailed(IUrmsTask task, UrmsTaskStatus status, UrmsError error) { 
//                    Log.e(TAG, "Register book task failed."); Log.e(TAG, error.getErrorCode());  
//                    if (status != UrmsTaskStatus.Cancelled) { 
//                        if (error.getErrorType() == UrmsError.NetworkError || error.getErrorType() == UrmsError.NetworkTimeout) { 
//                            Log.e(TAG, "Network Error or Network Timeout."); } else if (error.getErrorType() == UrmsError.UrmsNotInitialized) { 
//                            Log.e(TAG, "URMS not initialized."); } else if (error.getErrorType() == UrmsError.NoBook) { 
//                            Log.e(TAG, "Invalid License."); } else if (error.getErrorType() == UrmsError.NotAuthorized) { 
//                            Log.e(TAG, "Not Authorized."); } else { 
//                            Log.e(TAG, "Other error occurred."); } } }
//
//                 
//            };  
//            registerBookRequest.execute(bookCCID, bookUri, succeededCallback, failedCallback);  //			final RegisterBookTask rbt = Urms.createRegisterBookTask(bookCCID); // //			Log.e(TAG, "[evaluateURMSLicense] bookUri: " + bookUri); //			rbt.setDestination(new File(bookUri)); // //			rbt.setProgressCallback(new IRegisterBookProgressCallback() { //				@Override //				public void onDownloadProgress(RegisterBookTask task, int current, int total) { //					BigDecimal ratio = new BigDecimal(String.valueOf(((double) current / (double) total) * 100)); //					Log.e(TAG, (ratio.setScale(2, BigDecimal.ROUND_HALF_UP).toString() + "%")); //				} //			}) // // .setPostExecuteCallback(new IPostExecuteCallback() { //				@Override //				public void onPostExecute(IUrmsTask task) { //					Log.e(TAG, "Register book task onPostExecute"); //				} //			}) 			Log.e(TAG, "Register book task executed."); 		} else { 			Log.e(TAG, "No error from evaluateURMSLicense. Opening book."); 			Book bookToOpen = CloudShelfDatabase.getBookAtCursorPoint(bookCursor); 			Container container = EPub3.openBook(bookUri); 			EPub3.setSdkErrorHandler(null); 			ContainerHolder.getInstance().put(container.getNativePtr(), container);  			// Pull latest book to prevent overwriting with the old book we have in the library. 			bookToOpen = CloudShelfDatabase.getBook(mContext, bookToOpen.getShelfBook());  			Intent readIntent = new Intent(mContext, ReadingFragmentHost.class); 			readIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 			readIntent.putExtra(Constants.BOOK_NAME, container.getName()); 			readIntent.putExtra(Constants.CONTAINER_ID, container.getNativePtr()); 			readIntent.putExtra(Constants.BOOK_DATA, bookToOpen);  			//					AppPreferences.writeLastReadBook(mContext, new ShelfBook(remoteBookId, mShelfId)); // TODO: keep track of last read book 			book.setLastReadDate(WebUtils.getUTCTime()); //TODO: Get Server based UTC instead 			CloudShelfDatabase.insertOrReplaceBook(mContext, book);  			final Intent _readIntent = readIntent;  			mContext.startActivity(_readIntent); 		} 	} 	 
//
}
