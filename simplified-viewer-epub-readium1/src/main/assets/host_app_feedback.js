//  Copyright (c) 2014 Readium Foundation and/or its licensees. All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without modification,
//  are permitted provided that the following conditions are met:
//  1. Redistributions of source code must retain the above copyright notice, this
//  list of conditions and the following disclaimer.
//  2. Redistributions in binary form must reproduce the above copyright notice,
//  this list of conditions and the following disclaimer in the documentation and/or
//  other materials provided with the distribution.
//  3. Neither the name of the organization nor the names of its contributors may be
//  used to endorse or promote products derived from this software without specific
//  prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
//  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
//  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
//  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
//  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
//  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
//  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
//  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
//  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
//  OF THE POSSIBILITY OF SUCH DAMAGE.

$(document).ready(function()
{
  console.log("DOM READY");

  require(["readium_shared_js/globalsSetup"], function ()
  {
    console.log("globalsSetup READY");
    ReadiumSDK.DEBUG_MODE = true;

    require(['readium_shared_js/views/reader_view'], function (ReaderView)
    {
      console.log("reader_view READY");

      ReadiumSDK.HostAppFeedback = function()
      {
        ReadiumSDK.on(
          ReadiumSDK.Events.READER_INITIALIZED,
          function()
          {
            window.navigator.epubReadingSystem.name = "Simplified";
            window.navigator.epubReadingSystem.version = "0.0.0";

            ReadiumSDK.reader.on(ReadiumSDK.Events.MEDIA_OVERLAY_STATUS_CHANGED, this.onMediaOverlayStatusChanged, this);
            ReadiumSDK.reader.on(ReadiumSDK.Events.MEDIA_OVERLAY_TTS_SPEAK, this.onMediaOverlayTTSSpeak, this);
            ReadiumSDK.reader.on(ReadiumSDK.Events.MEDIA_OVERLAY_TTS_STOP, this.onMediaOverlayTTSStop, this);
            ReadiumSDK.reader.on(ReadiumSDK.Events.PAGINATION_CHANGED, this.onPaginationChanged, this);
            ReadiumSDK.reader.on(ReadiumSDK.Events.SETTINGS_APPLIED, this.onSettingsApplied, this);
            ReadiumSDK.reader.on(ReadiumSDK.Events.CONTENT_DOCUMENT_LOADED, this.onContentDocumentLoaded, this);

            window.location.href = "readium:initialize";
          },
          this
        );

        this.onPaginationChanged = function(pageChangeData)
        {
          window.location.href = "readium:pagination-changed/" +
            encodeURIComponent(JSON.stringify(pageChangeData.paginationInfo));
        };

        this.onSettingsApplied = function()
        {
          window.location.href = "readium:settings-applied";
        };

        this.onMediaOverlayStatusChanged = function(status)
        {
          window.location.href = "readium:media-overlay-status-changed/" +
            encodeURIComponent(JSON.stringify(status));
        };

        this.onMediaOverlayTTSSpeak = function(tts)
        {
          window.location.href = "readium:media-overlay-tts-speak/" +
            encodeURIComponent(JSON.stringify(tts));
        };

        this.onMediaOverlayTTSStop = function()
        {
          window.location.href = "readium:media-overlay-tts-stop";
        };

        this.onContentDocumentLoaded = function() {
          window.location.href = "readium:content-document-loaded";
        };
      }();

      var opts = {
        needsFixedLayoutScalerWorkAround: true,
        el: "#viewport",
        annotationCSSUrl: '/readium_Annotations.css',
        fonts: [
          {
            fontFamily: "serif",
            url: "/fonts.css"
          },
          {
            fontFamily: "sans-serif",
            url: "/fonts.css"
          },
          {
            fontFamily: "OpenDyslexic3",
            url: "/fonts.css"
          }
        ]
      };

      ReadiumSDK.reader = new ReaderView(opts);
      console.log("DONE READER");

      ReadiumSDK.emit(ReadiumSDK.Events.READER_INITIALIZED, ReadiumSDK.reader);
    });
  });
});
