package org.nypl.simplified.viewer.epub.readium1;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The type of objects that can be serialized to JSON.
 */

public interface ReaderJSONSerializableType
{
  /**
   * @return A JSON representation of the object
   *
   * @throws JSONException On serialization errors
   */

  JSONObject toJSON()
    throws JSONException;
}
