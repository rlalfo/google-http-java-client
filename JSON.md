


# Pluggable Streaming Library #

A fully pluggable JSON streaming library abstraction allows you to take advantage of the native platform's built-in JSON library support (for example the JSON library that is built into Android Honeycomb).  The streaming library enables you to write optimized code for efficient memory usage that minimizes parsing and serialization time.

A big advantage of this JSON library is that the choice of low-level streaming library is fully pluggable.  There are three built-in choices, all of which extend [JsonFactory](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/json/JsonFactory.html). You can easily plug in your own implementation.

  * [JacksonFactory](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/json/jackson2/JacksonFactory.html): Based on the popular [Jackson](http://jackson.codehaus.org/) library, which is considered the fastest in terms of parsing/serialization speed. Our JSON library provides JacksonFactory implementations based on both [Jackson 1](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/json/jackson/JacksonFactory.html) and [Jackson 2](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/json/jackson2/JacksonFactory.html).

  * [GsonFactory](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/json/gson/GsonFactory.html): Based on the [Google GSON](http://code.google.com/p/google-gson/) library, which is a lighter-weight option (small size) that is also fairly fast, though not as fast as Jackson.
  * [AndroidJsonFactory](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/extensions/android/json/AndroidJsonFactory.html)([@Beta](https://code.google.com/p/google-api-java-client/#@Beta)): Based on the JSON library that is built into Android Honeycomb (SDK 3.0) or higher, and that is identical to the Google GSON library.

# User-Defined JSON Data Models #

User-defined JSON data models allow you to define Plain Old Java Objects (POJOs) and define how the library parses and serializes them to and from JSON. The code snippets below are part of a more complete example, [googleplus-simple-cmdline-sample](http://samples.google-http-java-client.googlecode.com/hg/googleplus-simple-cmdline-sample/instructions.html), which demonstrates these concepts.

## Example ##

Let's take a look at a JSON snippet of a typical Google+ activity feed (showing only the fields relevant for this example):

```
{
 "items": [
  {
   "id": "z13lwnljpxjgt5wn222hcvzimtebslkul",
   "url": "https://plus.google.com/116899029375914044550/posts/HYNhBAMeA7U",
   "object": {
    "content": "\u003cb\u003eWho will take the title of 2011 Angry Birds College Champ?\u003c/b\u003e\u003cbr /\u003e\u003cbr /\u003e\u003cbr /\u003eIt&#39;s the 2nd anniversary of Angry Birds this Sunday, December 11, and to celebrate this break-out game we&#39;re having an intercollegiate angry birds challenge for students to compete for the title of 2011 Angry Birds College Champion. Add \u003cspan class=\"proflinkWrapper\"\u003e\u003cspan class=\"proflinkPrefix\"\u003e+\u003c/span\u003e\u003ca href=\"https://plus.google.com/105912662528057048457\" class=\"proflink\" oid=\"105912662528057048457\"\u003eAngry Birds College Challenge\u003c/a\u003e\u003c/span\u003e to learn more. Good luck, and have fun!",
    "plusoners": {
     "totalItems": 27
    }
   }
  },
  {
   "id": "z13rtboyqt2sit45o04cdp3jxuf5cz2a3e4",
   "url": "https://plus.google.com/116899029375914044550/posts/X8W8m9Hk5rE",
   "object": {
    "content": "CNN Heroes shines a spotlight on everyday people changing the world. Hear the top ten heroes&#39; inspiring stories by tuning in to the CNN broadcast of &quot;CNN Heroes: An All-Star Tribute&quot; on Sunday, December 11, at 8pm ET/5 pm PT with host \u003cspan class=\"proflinkWrapper\"\u003e\u003cspan class=\"proflinkPrefix\"\u003e+\u003c/span\u003e\u003ca href=\"https://plus.google.com/106168900754103197479\" class=\"proflink\" oid=\"106168900754103197479\"\u003eAnderson Cooper 360\u003c/a\u003e\u003c/span\u003e, and donate to their causes online in a few simple steps with Google Wallet (formerly known as Google Checkout): \u003ca href=\"http://www.google.com/landing/cnnheroes/2011/\" \u003ehttp://www.google.com/landing/cnnheroes/2011/\u003c/a\u003e.",
    "plusoners": {
     "totalItems": 21
    }
   }
  },
  {
   "id": "z13wtpwpqvihhzeys04cdp3jxuf5cz2a3e4",
   "url": "https://plus.google.com/116899029375914044550/posts/dBnaybdLgzU",
   "object": {
    "content": "Today we hosted one of our Big Tent events in The Hague. \u003cspan class=\"proflinkWrapper\"\u003e\u003cspan class=\"proflinkPrefix\"\u003e+\u003c/span\u003e\u003ca href=\"https://plus.google.com/104233435224873922474\" class=\"proflink\" oid=\"104233435224873922474\"\u003eEric Schmidt\u003c/a\u003e\u003c/span\u003e, Dutch Foreign Minister Uri Rosenthal, U.S. Secretary of State Hillary Clinton and many others came together to discuss free expression and the Internet. The Hague is our third Big Tent, a place where we bring together various viewpoints to discuss essential topics to the future of the Internet. Read more on the Official Google Blog here: \u003ca href=\"http://goo.gl/d9cSe\" \u003ehttp://goo.gl/d9cSe\u003c/a\u003e, and watch the video below for highlights from the day.",
    "plusoners": {
     "totalItems": 76
    }
   }
  }
 ]
}
```

Here's one possible way to design the Java data classes to represent this:

```
/** Feed of Google+ activities. */
public static class ActivityFeed {

  /** List of Google+ activities. */
  @Key("items")
  private List<Activity> activities;

  public List<Activity> getActivities() {
    return activities;
  }
}

/** Google+ activity. */
public static class Activity extends GenericJson {

  /** Activity URL. */
  @Key
  private String url;

  public String getUrl() {
    return url;
  }

  /** Activity object. */
  @Key("object")
  private ActivityObject activityObject;

  public ActivityObject getActivityObject() {
    return activityObject;
  }
}

/** Google+ activity object. */
public static class ActivityObject {

  /** HTML-formatted content. */
  @Key
  private String content;

  public String getContent() {
    return content;
  }

  /** People who +1'd this activity. */
  @Key
  private PlusOners plusoners;

  public PlusOners getPlusOners() {
    return plusoners;
  }
}

/** People who +1'd an activity. */
public static class PlusOners {

  /** Total number of people who +1'd this activity. */
  @Key
  private long totalItems;

  public long getTotalItems() {
    return totalItems;
  }
}
```

A fully supported [HTTP JSON parser](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/json/JsonParser.html) makes it easy to parse HTTP responses to objects of these user defined classes:

```
private static void parseResponse(HttpResponse response) throws IOException {
  ActivityFeed feed = response.parseAs(ActivityFeed.class);
  if (feed.getActivities().isEmpty()) {
    System.out.println("No activities found.");
  } else {
    for (Activity activity : feed.getActivities()) {
      System.out.println();
      System.out.println("-----------------------------------------------");
      System.out.println("HTML Content: " + activity.getActivityObject().getContent());
      System.out.println("+1's: " + activity.getActivityObject().getPlusOners().getTotalItems());
      System.out.println("URL: " + activity.getUrl());
      System.out.println("ID: " + activity.get("id"));
    }
  }
}
```

## Key annotation ##

Use the @[Key](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/util/Key.html) annotation to indicate fields that need to be parsed from or serialized to JSON. By default, the `@Key` annotation uses the Java field name as the JSON key. To override this, specify the value of the `@Key` annotation.

Fields that don't have the `@Key` annotation are considered internal data and are not parsed from or serialized to JSON.

## Visibility ##

Visibility of the fields does not matter, nor does the existence of the getter or setter methods. So for example, the following alternative representation for `PlusOners` would work in the example given above:

```
/** People who +1'd an activity. */
public static class AlternativePlusOnersWithPublicField {

  /** Total number of people who +1'd this activity. */
  @Key
  public long totalItems;
}
```

## GenericJson ##

Normally only the fields you declare are parsed when a JSON response is parsed. The actual Google+ activity feed response contains a lot of content that we are not using in our example. The JSON parser skips that other content when parsing the response from Google+.

To retain the other content, declare your class to extend [GenericJson](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/json/GenericJson.html). Notice that `GenericJson` implements
[Map](http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Map.html), so we can use the [get](http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Map.html#get(java.lang.Object)) and [put](http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Map.html#put(K,%20V)) methods to set JSON content. See [googleplus-simple-cmdline-sample](http://samples.google-http-java-client.googlecode.com/hg/googleplus-simple-cmdline-sample/instructions.html) for an example of how it was used in the `Activity` class above.

## Map ##

The JSON library supports any implementation of [Map](http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Map.html), which works similarly to [GenericJson](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/json/GenericJson.html). The downside, of course, is that you lose the static type information for the fields.

## JSON null ##

One advantage of this JSON library is its ability to support JSON nulls and distinguish them from undeclared JSON keys. Although JSON nulls are relatively rare, when they do occur they often cause confusion.

Google+ doesn't use JSON null values, so the following example uses fictitious JSON data to illustrate what can happen:

```
{
 "items": [
  {
   "id": "1",
   "value": "some value"
  },
  {
   "id": "2",
   "value": null
  }
  {
   "id": "3"
  }
 ]
}
```

We might represent each item as follows:

```
public class Item {
  @Key
  public String id;
  @Key
  public String value;
}
```

For items `2` and `3`, what should be in the `value` field? The problem is that there is no obvious way in Java to distinguish between a JSON key that is undeclared and a JSON key whose value is JSON null. This JSON library solves the problem by using Java null for the common case of an undeclared JSON key, and a special "magic" instance of `String` ([Data.NULL\_STRING](http://javadoc.google-http-java-client.googlecode.com/hg/1.18.0-rc/com/google/api/client/util/Data.html#NULL_STRING)) to identify that it is a JSON null rather than a normal value.

The following example shows how you might take advantage of this functionality:

```
private static void show(List<Item> items) {
  for (Item item : items) {
    System.out.println("ID: " + item.id);
    if (item.value == null) {
      System.out.println("No Value");
    } else if (Data.isNull(item.value)) {
      System.out.print("Null Value");
    } else {
      System.out.println("Value: '" + item.value + "'");
    }
  }
}
```