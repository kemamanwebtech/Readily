package com.infmme.readilyapp.reader;

import android.text.TextUtils;
import android.util.Log;
import com.infmme.readilyapp.readable.interfaces.Chunked;
import com.infmme.readilyapp.readable.interfaces.Reading;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;

/**
 * Producer Runnable to load Chunked reading from the filesystem and feed it
 * into displaying logic.
 * <p>
 * Created with love, by infm dated on 6/10/16.
 */
public class ReaderTask implements Runnable {
  private static final int DEQUE_SIZE_LIMIT = 3;
  private final ArrayDeque<Reading> mReadingDeque = new ArrayDeque<>();

  private final MonitorObject mMonitor;

  private Chunked mChunked = null;
  private Reading mSingleReading = null;

  private ReaderTaskCallbacks mCallback;

  private boolean mOnceStarted = false;

  /**
   * Constructs ReaderTask for chunked reading source.
   *
   * @param monitorObject Monitor which manages this thread.
   * @param chunked       Chunked instance to load Readings.
   * @param callback      Callback to communicate with UI thread and another
   *                      parts of an app.
   */
  public ReaderTask(MonitorObject monitorObject, Chunked chunked,
                    ReaderTaskCallbacks callback) {
    this.mMonitor = monitorObject;
    this.mChunked = chunked;
    this.mCallback = callback;
  }

  public ReaderTask(MonitorObject object, Reading singleReading,
                    ReaderTaskCallbacks callback) {
    this.mMonitor = object;
    this.mSingleReading = singleReading;
    this.mCallback = callback;
  }

  private Reading nextNonEmptyReading() throws IOException {
    Reading nextReading = null;
    // Checks if we have only one reading to process.
    if (mChunked == null) {
      Log.d(ReaderTask.class.getName(), "mChunked is null");
      nextReading = mSingleReading;
      mSingleReading = null;
    } else if (mChunked.hasNextReading()) {
      Log.d(ReaderTask.class.getName(), "mChunked has next reading");
      // Finds non-empty consecutive reading.
      do {
        // Therefore we're here not for the first time.
        if (nextReading != null) {
          Log.d(ReaderTask.class.getName(), "mChunked skipping last reading");
          mChunked.skipLast();
        }
        nextReading = mChunked.readNext();
      } while (TextUtils.isEmpty(
          nextReading.getText()) && mChunked.hasNextReading());
    }
    if (nextReading != null) {
      TextParser result =
          TextParser.newInstance(nextReading,
                                 mCallback.getDelayCoefficients());
      result.process();
      nextReading = result.getReading();
    }
    return nextReading;
  }

  // TODO: Produce item on start immediately after first one.
  @Override
  public void run() {
    while (mCallback.shouldContinue()) {
      try {
        synchronized (mReadingDeque) {
          Log.d(ReaderTask.class.getName(), "Locking mReadingDeque");
          // Sort of initialization for a deque.
          Reading nextReading;
          if (!mOnceStarted) {
            nextReading = nextNonEmptyReading();
            if (nextReading != null && !TextUtils.isEmpty(
                nextReading.getText())) {
              Log.d(ReaderTask.class.getName(),
                    "Adding to reading deque for the first time");
              mReadingDeque.add(nextReading);
            }
          }
          // Checks if we have more data to produce.
          if (mChunked != null && mReadingDeque.size() > 0) {
            Reading currentReading = mReadingDeque.getLast();
            while (mReadingDeque.size() < DEQUE_SIZE_LIMIT &&
                currentReading != null &&
                !TextUtils.isEmpty(currentReading.getText())) {
              nextReading = nextNonEmptyReading();
              if (nextReading != null && !TextUtils.isEmpty(
                  nextReading.getText())) {
                // Duplicates adjacent reading data to transit smoothly between
                // them.
                duplicateAdjacentData(currentReading, nextReading);
                Log.d(ReaderTask.class.getName(), "Adding to reading deque");
                mReadingDeque.addLast(nextReading);
              }
              currentReading = nextReading;
            }
          }
        }
        // If we haven't started Reader flow yet, we have to do it now.
        if (!mOnceStarted) {
          Log.d(ReaderTask.class.getName(),
                "Removing from reading deque for the first time");
          mCallback.startReader(removeDequeHead());
          mOnceStarted = true;
        }
        synchronized (mMonitor) {
          mMonitor.pauseTask();
        }
      } catch (InterruptedException | IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Polls head of a deque. Called for start of an entire flow and from Reader
   * when it changes a Reading instance.
   *
   * @return Loaded Reading from a chunk.
   */
  public Reading removeDequeHead() {
    synchronized (mReadingDeque) {
      return mReadingDeque.pollFirst();
    }
  }

  public synchronized boolean isNextLoaded() {
    return mReadingDeque.size() > 1;
  }

  private void duplicateAdjacentData(Reading currentReading,
                                     Reading nextReading) {
    List<String> wordList = currentReading.getWordList();
    wordList.addAll(
        nextReading.getWordList()
                   .subList(0, Math.min(nextReading.getWordList().size(),
                                        Reader.LAST_WORD_PREFIX_SIZE)));
    currentReading.setWordList(wordList);

    List<Integer> emphasisList = currentReading.getEmphasisList();
    emphasisList.addAll(
        nextReading.getEmphasisList()
                   .subList(0, Math.min(nextReading.getEmphasisList().size(),
                                        Reader.LAST_WORD_PREFIX_SIZE)));
    currentReading.setEmphasisList(emphasisList);

    List<Integer> delayList = currentReading.getDelayList();
    delayList.addAll(
        nextReading.getDelayList()
                   .subList(0, Math.min(nextReading.getDelayList().size(),
                                        Reader.LAST_WORD_PREFIX_SIZE)));
    currentReading.setDelayList(delayList);
  }

  public interface ReaderTaskCallbacks {
    void startReader(Reading first);

    List<Integer> getDelayCoefficients();

    boolean shouldContinue();
  }
}