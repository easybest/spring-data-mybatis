package org.springframework.data.mybatis.repository.util;

import java.util.Random;

/**
 * Twitter's ID Generator. <p> from https://github.com/twitter/snowflake/blob/master/src/main/scala/com/twitter/service/snowflake/IdWorker.scala
 */
public class IdWorker {

  private final long workerId;
  private final long datacenterId;
  private final long idepoch;

  private static final long workerIdBits = 5L;
  private static final long datacenterIdBits = 5L;
  private static final long maxWorkerId = -1L ^ (-1L << workerIdBits);
  private static final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

  private static final long sequenceBits = 12L;
  private static final long workerIdShift = sequenceBits;
  private static final long datacenterIdShift = sequenceBits + workerIdBits;
  private static final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
  private static final long sequenceMask = -1L ^ (-1L << sequenceBits);

  private long lastTimestamp = -1L;
  private long sequence;
  private static final Random r = new Random();

  public IdWorker() {
    this(1344322705519L);
  }

  public IdWorker(long idepoch) {
    this(r.nextInt((int) maxWorkerId), r.nextInt((int) maxDatacenterId), 0, idepoch);
  }

  public IdWorker(long workerId, long datacenterId, long sequence) {
    this(workerId, datacenterId, sequence, 1344322705519L);
  }

  //
  public IdWorker(long workerId, long datacenterId, long sequence, long idepoch) {
    this.workerId = workerId;
    this.datacenterId = datacenterId;
    this.sequence = sequence;
    this.idepoch = idepoch;
    if (workerId < 0 || workerId > maxWorkerId) {
      throw new IllegalArgumentException("workerId is illegal: " + workerId);
    }
    if (datacenterId < 0 || datacenterId > maxDatacenterId) {
      throw new IllegalArgumentException("datacenterId is illegal: " + workerId);
    }
    if (idepoch >= System.currentTimeMillis()) {
      throw new IllegalArgumentException("idepoch is illegal: " + idepoch);
    }
  }

  public long getDatacenterId() {
    return datacenterId;
  }

  public long getWorkerId() {
    return workerId;
  }

  public long getTime() {
    return System.currentTimeMillis();
  }

  public long getId() {
    long id = nextId();
    return id;
  }

  public String getIdAsString() {
    long id = nextId();
    return String.valueOf(id);
  }

  private synchronized long nextId() {
    long timestamp = timeGen();
    if (timestamp < lastTimestamp) {
      throw new IllegalStateException("Clock moved backwards.");
    }
    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & sequenceMask;
      if (sequence == 0) {
        timestamp = tilNextMillis(lastTimestamp);
      }
    } else {
      sequence = 0;
    }
    lastTimestamp = timestamp;
    long id = ((timestamp - idepoch) << timestampLeftShift)//
        | (datacenterId << datacenterIdShift)//
        | (workerId << workerIdShift)//
        | sequence;
    return id;
  }

  /**
   * get the timestamp (millis second) of id
   *
   * @param id the nextId
   * @return the timestamp of id
   */
  public long getIdTimestamp(long id) {
    return idepoch + (id >> timestampLeftShift);
  }

  private long tilNextMillis(long lastTimestamp) {
    long timestamp = timeGen();
    while (timestamp <= lastTimestamp) {
      timestamp = timeGen();
    }
    return timestamp;
  }

  private long timeGen() {
    return System.currentTimeMillis();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("IdWorker{");
    sb.append("workerId=").append(workerId);
    sb.append(", datacenterId=").append(datacenterId);
    sb.append(", idepoch=").append(idepoch);
    sb.append(", lastTimestamp=").append(lastTimestamp);
    sb.append(", sequence=").append(sequence);
    sb.append('}');
    return sb.toString();
  }
}
