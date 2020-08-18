package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONPointer;

public abstract class SchemaStore {
  private final Collection<URI> unbuilt = new HashSet<>();
  private final Map<URI, Schema> built = new HashMap<>();

  abstract Object load(String filePart) throws GenerationException;

  public Object resolve(URI pointer) throws GenerationException {
    String filePart = pointer.getPath();
    String pointer1 = "#" + pointer.getFragment();
    Object object = load(filePart);
    if ("#/".equals(pointer1)) {
      return object;
    }
    JSONPointer jsonPointer = new JSONPointer(pointer1);
    return jsonPointer.queryFrom(object);
  }

  public URI require(URI pointer) throws GenerationException {
    if (unbuilt.contains(pointer) || built.containsKey(pointer)) {
      return pointer;
    }
    // Is a $ref?
    Object object = resolve(pointer);
    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      String ref = jsonObject.optString("$ref");
      if (!ref.isEmpty()) {
        try {
          URI ref2 = new URI(ref);
          String path = ref2.getPath();
          if (path == null || path.isEmpty()) {
            ref2 = new URI(null, null, null, -1, pointer.getPath(), null, ref2.getFragment());
          }
          String fragment = ref2.getFragment();
          if (fragment == null || fragment.isEmpty()) {
            ref2 = new URI(null, null, null, -1, ref2.getPath(), null, "/");
          }
          return require(ref2);
        } catch (URISyntaxException e) {
          throw new GenerationException(e);
        }
      }
    }

    unbuilt.add(pointer);

    return pointer;
  }

  public void process() throws GenerationException {
    while (!unbuilt.isEmpty()) {
      URI pointer = unbuilt.iterator().next();
      System.out.println("Processing " + pointer);
      built.put(pointer, Schemas.create(this, pointer));
      unbuilt.remove(pointer);
    }
  }
}
