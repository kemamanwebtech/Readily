package com.infmme.readilyapp.readable.fb2;

import android.text.TextUtils;
import com.infmme.readilyapp.readable.interfaces.AbstractTocReference;
import com.infmme.readilyapp.xmlparser.XMLEvent;
import com.infmme.readilyapp.xmlparser.XMLEventType;
import com.infmme.readilyapp.xmlparser.XMLParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.infmme.readilyapp.readable.Utils.guessCharset;

/**
 * Created with love, by infm dated on 6/5/16.
 */

public class FB2Part implements AbstractTocReference {
  private String id = "";
  private String title = "";
  private double percentile;
  private final long streamByteStartLocation;
  private long streamByteEndLocation;

  private transient String filePath;
  private transient String cachedPreview = null;

  private List<FB2Part> children = new ArrayList<>();

  public FB2Part(long streamByteStartLocation, String filePath) {
    this.streamByteStartLocation = streamByteStartLocation;
    this.streamByteEndLocation = this.streamByteStartLocation;
    this.filePath = filePath;
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setPath(String path) {
    filePath = path;
  }

  @Override
  public String getPreview() throws IOException {
    if (cachedPreview == null) {
      File file = new File(filePath);
      FileInputStream encodingHelper = new FileInputStream(file);
      String encoding = guessCharset(encodingHelper);
      encodingHelper.close();

      FileInputStream inputStream = new FileInputStream(file);

      XMLParser parser = new XMLParser();
      parser.setInput(inputStream, encoding);
      parser.skip(streamByteStartLocation);
      XMLEvent event = parser.next();
      XMLEventType eventType = event.getType();

      StringBuilder text = new StringBuilder();
      while (eventType != XMLEventType.DOCUMENT_CLOSE &&
          parser.getPosition() <= streamByteEndLocation) {
        if (eventType == XMLEventType.CONTENT) {
          String contentType = event.getContentType();
          if (!TextUtils.isEmpty(contentType)) {
            if (contentType.equals("p"))
              text.append(event.getContent());
          }
          text.append(" ");
        }
        event = parser.next();
        eventType = event.getType();
      }
      cachedPreview = text.toString();
    }
    return cachedPreview;
  }

  public String getCachedPreview() {
    return cachedPreview;
  }

  @Override
  public double getPercentile() {
    return percentile;
  }

  public void setPercentile(double percentile) {
    this.percentile = percentile;
  }

  public long getStreamByteStartLocation() {
    return streamByteStartLocation;
  }

  public long getStreamByteEndLocation() {
    return streamByteEndLocation;
  }

  public void setStreamByteEndLocation(long streamByteEndLocation) {
    this.streamByteEndLocation = streamByteEndLocation;
  }

  @Override
  public List<? extends AbstractTocReference> getChildren() {
    return new ArrayList<>(children);
  }

  public void addChild(FB2Part child) {
    children.add(child);
  }
}