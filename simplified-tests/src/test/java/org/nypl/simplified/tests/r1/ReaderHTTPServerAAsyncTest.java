package org.nypl.simplified.tests.r1;

import android.content.res.AssetManager;

import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nypl.simplified.viewer.epub.readium1.ReaderHTTPMimeMapType;
import org.nypl.simplified.viewer.epub.readium1.ReaderHTTPServerAAsync;
import org.nypl.simplified.viewer.epub.readium1.ReaderHTTPServerStartListenerType;
import org.readium.sdk.android.ManifestItem;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.util.ResourceInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReaderHTTPServerAAsyncTest {
  private static final String GUESSED_MIME_TYPE = "itsjust/aguess";
  private static final String NULL_MANIFEST_ITEM_RELATIVE_PATH = "foo.bar";
  private static final String NULL_TYPE_MANIFEST_ITEM_RELATIVE_PATH = "notype.nope";
  private static final String XHTML_MANIFEST_ITEM_RELATIVE_PATH = "chapter1.xml";
  private static final String XHTML_MANIFEST_ITEM_MIME_TYPE = "application/xhtml+xml";

  private static ReaderHTTPServerAAsync httpServer;

  private static AssetManager createAssetManager() throws IOException {
    AssetManager mockAssetManager = mock(AssetManager.class);

    when(mockAssetManager.open(anyString(), anyInt())).thenThrow(new FileNotFoundException());

    return mockAssetManager;
  }

  private static ReaderHTTPMimeMapType createMimeMap() {
    ReaderHTTPMimeMapType mockMimeMap = mock(ReaderHTTPMimeMapType.class);

    when(mockMimeMap.guessMimeTypeForURI(anyString())).thenReturn(GUESSED_MIME_TYPE);

    return mockMimeMap;
  }

  private static Package createPackage() {
    Package mockPackage = mock(Package.class);

    when(mockPackage.getManifestItem(XHTML_MANIFEST_ITEM_RELATIVE_PATH)).thenReturn(
        new ManifestItem(XHTML_MANIFEST_ITEM_RELATIVE_PATH, XHTML_MANIFEST_ITEM_MIME_TYPE));

    when(mockPackage.getManifestItem(NULL_MANIFEST_ITEM_RELATIVE_PATH)).thenReturn(null);

    when(mockPackage.getManifestItem(NULL_TYPE_MANIFEST_ITEM_RELATIVE_PATH)).thenReturn(
        new ManifestItem(XHTML_MANIFEST_ITEM_RELATIVE_PATH, null));

    when(mockPackage.getInputStream(anyString(), anyBoolean())).thenReturn(
        mock(ResourceInputStream.class));

    return mockPackage;
  }

  private static ReaderHTTPServerStartListenerType createStartListener() {
    ReaderHTTPServerStartListenerType mockStartListener = mock(
        ReaderHTTPServerStartListenerType.class);

    return mockStartListener;
  }

  @BeforeClass
  public static void setUp() throws IOException {
    httpServer = (ReaderHTTPServerAAsync) ReaderHTTPServerAAsync.newServer(
        createAssetManager(), createMimeMap(), 8888);

    httpServer.startIfNecessaryForPackage(createPackage(), createStartListener());
  }

  private AsyncHttpServerRequest createRequestForRelativePath(String relativePath) {
    AsyncHttpServerRequest mockRequest = mock(AsyncHttpServerRequest.class);

    when(mockRequest.getHeaders()).thenReturn(new Headers());
    when(mockRequest.getPath()).thenReturn("/" + relativePath);

    return mockRequest;
  }

  @Test
  public void mimeTypeIsReadFromManifestItem() {
    AsyncHttpServerRequest mockRequest = createRequestForRelativePath(
        XHTML_MANIFEST_ITEM_RELATIVE_PATH);

    AsyncHttpServerResponse mockResponse = mock(AsyncHttpServerResponse.class);
    when(mockResponse.getHeaders()).thenReturn(new Headers());

    httpServer.onRequest(mockRequest, mockResponse);

    verify(mockResponse).setContentType(XHTML_MANIFEST_ITEM_MIME_TYPE);
  }

  @Test
  public void mimeTypeIsGuessedWhenManifestItemIsNull() {
    AsyncHttpServerRequest mockRequest = createRequestForRelativePath(
        NULL_MANIFEST_ITEM_RELATIVE_PATH);

    AsyncHttpServerResponse mockResponse = mock(AsyncHttpServerResponse.class);
    when(mockResponse.getHeaders()).thenReturn(new Headers());

    httpServer.onRequest(mockRequest, mockResponse);

    verify(mockResponse).setContentType(GUESSED_MIME_TYPE);
  }

  @Test
  public void mimeTypeIsGuessedWhenManifestItemTypeIsNull() {
    AsyncHttpServerRequest mockRequest = createRequestForRelativePath(
        NULL_TYPE_MANIFEST_ITEM_RELATIVE_PATH);

    AsyncHttpServerResponse mockResponse = mock(AsyncHttpServerResponse.class);
    when(mockResponse.getHeaders()).thenReturn(new Headers());

    httpServer.onRequest(mockRequest, mockResponse);

    verify(mockResponse).setContentType(GUESSED_MIME_TYPE);
  }
}
