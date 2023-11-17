package com.dynatrace.research.shufflebench.consumer;

/**
 *  Keeps the state of a stateful consumer.
 */
public class State {
  private byte[] data;

  public State() {
  }

  public State(byte[] data) {
    this.data = data;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

}
